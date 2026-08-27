package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.integration.docker.DockerEngineClient;
import com.nexus.portal.releaseorchestrator.integration.docker.DockerEngineException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implanta {@link TipoImplantacao#DOCKER_PULL} via Engine API no host. */
@Component
@RequiredArgsConstructor
public class DockerPullImplantacaoAdapter implements ImplantacaoAdapter {

  private final DockerEngineClient docker;

  @Override
  public Resultado aplicar(Contexto contexto) {
    if (contexto.instalacao().getTipoImplantacao() != TipoImplantacao.DOCKER_PULL) {
      return Resultado.falha("Adaptador Docker pull chamado para tipo "
          + contexto.instalacao().getTipoImplantacao() + ".");
    }
    Host host = contexto.host();
    if (!host.isDockerDisponivel()) {
      return Resultado.falha("O host " + host.getCodigo() + " não tem Docker disponível.");
    }
    if (contexto.operacao().cicloVida()) {
      return cicloVida(contexto, host);
    }
    String imagemRef = contexto.manifesto().imagemRef();
    if (imagemRef == null || imagemRef.isBlank()) {
      return Resultado.falha("Manifesto sem imagem para Docker pull.");
    }
    try {
      String baseUrl = baseUrl(host);
      String nome = nomeContainer(contexto.instalacao().getCodigo());
      ImageRef parsed = ImageRef.parse(imagemRef);
      List<Integer> portas = contexto.instalacao().getPortas() == null
          ? List.of()
          : contexto.instalacao().getPortas().stream()
              .filter(p -> p.getStatus() == StatusReservaPorta.RESERVADA
                  || p.getStatus() == StatusReservaPorta.EM_USO)
              .map(ReservaPorta::getPorta)
              .toList();
      docker.ping(baseUrl);
      docker.pull(baseUrl, parsed.imagem(), parsed.tag());
      docker.removerSeExistir(baseUrl, nome);
      Map<String, Object> hostConfig = Map.of(
          "RestartPolicy", Map.of("Name", "unless-stopped"),
          "PortBindings", DockerEngineClient.portBindings(portas));
      docker.criarContainer(
          baseUrl,
          nome,
          parsed.completa(),
          hostConfig,
          DockerEngineClient.exposedPorts(portas),
          Map.of(
              "com.nexus.instalacao", contexto.instalacao().getCodigo(),
              "com.nexus.release", contexto.release().getVersao()));
      docker.iniciar(baseUrl, nome);
      String acao = contexto.operacao() == OperacaoDeploy.CRIAR ? "criou" : "atualizou";
      return Resultado.ok("REAL: " + acao + " o container " + nome + " em " + host.getHostname()
          + " com " + parsed.completa() + ".");
    } catch (DockerEngineException ex) {
      return Resultado.falha(ex.getMessage());
    }
  }

  private Resultado cicloVida(Contexto contexto, Host host) {
    try {
      String baseUrl = baseUrl(host);
      String nome = nomeContainer(contexto.instalacao().getCodigo());
      docker.ping(baseUrl);
      if (contexto.operacao() == OperacaoDeploy.INICIAR) {
        docker.iniciar(baseUrl, nome);
        return Resultado.ok("REAL: iniciou o container " + nome + " em " + host.getHostname() + ".");
      }
      docker.parar(baseUrl, nome);
      return Resultado.ok("REAL: parou o container " + nome + " em " + host.getHostname() + ".");
    } catch (DockerEngineException ex) {
      return Resultado.falha(ex.getMessage());
    }
  }

  static String baseUrl(Host host) {
    int porta = host.getPortaConexao() != null
        ? host.getPortaConexao()
        : (host.getTipoConexao() == TipoConexaoHost.DOCKER ? 2376 : 2375);
    if (porta == 2376) {
      throw new DockerEngineException(
          "A porta 2376 é TLS e ainda não é suportada no modo REAL. "
              + "Exponha a Engine API em HTTP na porta 2375 (ou ajuste a porta de conexão do host).");
    }
    return "http://" + host.getHostname() + ":" + porta;
  }

  static String nomeContainer(String codigo) {
    String raw = codigo == null ? "nexus" : codigo.trim().toLowerCase(Locale.ROOT);
    String nome = raw.replaceAll("[^a-z0-9_.-]", "-");
    if (nome.isBlank() || !Character.isLetterOrDigit(nome.charAt(0))) {
      nome = "n" + nome;
    }
    return nome;
  }

  record ImageRef(String imagem, String tag) {
    static ImageRef parse(String ref) {
      String value = ref.trim();
      int slash = value.lastIndexOf('/');
      int colon = value.lastIndexOf(':');
      if (colon > slash) {
        return new ImageRef(value.substring(0, colon), value.substring(colon + 1));
      }
      return new ImageRef(value, "latest");
    }

    String completa() {
      return imagem + ":" + tag;
    }
  }
}
