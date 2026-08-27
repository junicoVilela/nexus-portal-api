package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.config.LinuxManualProperties;
import com.nexus.portal.releaseorchestrator.entity.BuildInstalacaoArtefato;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.StatusBuildInstalacao;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsAdapter;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsArtifact;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsBuildInfo;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsQueueItem;
import com.nexus.portal.releaseorchestrator.integration.ssh.HostSshClient;
import com.nexus.portal.releaseorchestrator.integration.ssh.LocalHostFsClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Após o Jenkins enfileirar o job, espera o build e copia WAR/JAR para
 * {@code {diretorioInstalacao}/artifacts/}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildArtefatoInstalacaoWorker {

  static final Duration POLL = Duration.ofSeconds(4);
  static final Duration TIMEOUT = Duration.ofMinutes(25);

  private final BuildAlvoInstalacaoService alvos;
  private final JenkinsAdapter jenkins;
  private final JenkinsPollSleeper sleeper;
  private final LocalHostFsClient localFs;
  private final HostSshClient ssh;
  private final LinuxManualProperties linuxManual;

  @Async("applicationTaskExecutor")
  public void acompanhar(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    List<BuildInstalacaoArtefato> rows = alvos.carregarParaAcompanhar(ids);
    Map<String, List<BuildInstalacaoArtefato>> porFila = new LinkedHashMap<>();
    for (BuildInstalacaoArtefato row : rows) {
      if (row.getStatus() != StatusBuildInstalacao.ENFILEIRADO) {
        continue;
      }
      String chave = (row.getJenkinsJob() + "|" + String.valueOf(row.getQueueUrl())).toLowerCase(Locale.ROOT);
      porFila.computeIfAbsent(chave, k -> new ArrayList<>()).add(row);
    }
    for (List<BuildInstalacaoArtefato> grupo : porFila.values()) {
      try {
        acompanharGrupo(grupo);
      } catch (RuntimeException | InterruptedException ex) {
        if (ex instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        String msg = ex.getMessage() == null ? "Falha ao acompanhar o build Jenkins." : ex.getMessage();
        log.warn("Cópia de artefato falhou: {}", msg);
        for (BuildInstalacaoArtefato row : grupo) {
          marcar(row, StatusBuildInstalacao.FAILED, msg);
        }
      }
    }
  }

  void acompanharGrupo(List<BuildInstalacaoArtefato> grupo) throws InterruptedException {
    BuildInstalacaoArtefato primeiro = grupo.get(0);
    ProdutoRh produto = primeiro.getProduto();
    Integer numero = esperarNumero(produto, primeiro.getQueueUrl());
    if (numero == null) {
      for (BuildInstalacaoArtefato row : grupo) {
        marcar(row, StatusBuildInstalacao.FAILED, "O Jenkins cancelou a fila ou o build não começou a tempo.");
      }
      return;
    }
    for (BuildInstalacaoArtefato row : grupo) {
      alvos.atualizarAcompanhamento(row.getId(), numero, null, null);
      row.marcarNumero(numero);
    }
    JenkinsBuildInfo info = esperarTermino(produto, primeiro.getJenkinsJob(), numero);
    if (info.building()) {
      for (BuildInstalacaoArtefato row : grupo) {
        marcar(row, StatusBuildInstalacao.FAILED, "Timeout aguardando o Jenkins (#" + numero + ").");
      }
      return;
    }
    if (!"SUCCESS".equalsIgnoreCase(info.result()) && !"UNSTABLE".equalsIgnoreCase(info.result())) {
      String res = info.result() == null ? "desconhecido" : info.result();
      for (BuildInstalacaoArtefato row : grupo) {
        marcar(row, StatusBuildInstalacao.FAILED, "Jenkins #" + numero + " terminou com " + res + ".");
      }
      return;
    }
    List<JenkinsArtifact> arts = jenkins.listarArtefatos(
        produto.getJenkinsUrl(), primeiro.getJenkinsJob(), produto.getJenkinsUser(),
        produto.getJenkinsToken(), numero);
    for (BuildInstalacaoArtefato row : grupo) {
      copiar(row, arts, numero);
    }
  }

  private Integer esperarNumero(ProdutoRh produto, String queueUrl) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline) {
      JenkinsQueueItem item = jenkins.consultarFila(
          produto.getJenkinsUrl(), produto.getJenkinsUser(), produto.getJenkinsToken(), queueUrl)
          .orElse(null);
      if (item != null && item.cancelled()) {
        return null;
      }
      if (item != null && item.executableNumber() != null) {
        return item.executableNumber();
      }
      sleeper.sleep(POLL);
    }
    return null;
  }

  private JenkinsBuildInfo esperarTermino(ProdutoRh produto, String job, int numero)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
    JenkinsBuildInfo ultimo = null;
    while (System.currentTimeMillis() < deadline) {
      ultimo = jenkins.consultarBuild(
          produto.getJenkinsUrl(), job, produto.getJenkinsUser(), produto.getJenkinsToken(), numero)
          .orElse(null);
      if (ultimo != null && !ultimo.building()) {
        return ultimo;
      }
      sleeper.sleep(POLL);
    }
    return ultimo == null
        ? new JenkinsBuildInfo(numero, null, true, 0, 0, null)
        : ultimo;
  }

  private void copiar(BuildInstalacaoArtefato row, List<JenkinsArtifact> arts, int numero) {
    InstalacaoCliente inst = row.getInstalacao();
    String dir = inst.getDiretorioInstalacao();
    if (dir == null || dir.isBlank()) {
      marcar(row, StatusBuildInstalacao.FAILED,
          "Informe o diretório da instalação para copiar os arquivos para artifacts/.");
      return;
    }
    List<JenkinsArtifact> match = arts.stream()
        .filter(a -> ArtefatoBuildMatcher.casa(a.fileName(), row.getPadraoAsset()))
        .toList();
    if (match.isEmpty()) {
      marcar(row, StatusBuildInstalacao.FAILED,
          "Jenkins #" + numero + " não arquivou artefato combinando com "
              + (row.getPadraoAsset() == null ? "*.war/*.jar" : row.getPadraoAsset())
              + ". Confira archiveArtifacts no job.");
      return;
    }
    Host host = inst.getHost();
    HostSshClient acesso = escolher(host);
    List<String> copiados = new ArrayList<>();
    try {
      for (JenkinsArtifact art : match) {
        String destNome = ArtefatoBuildMatcher.destino(art.fileName(),
            match.size() == 1 ? row.getNomeArquivo() : null);
        if (destNome.isBlank() || destNome.contains("..")) {
          continue;
        }
        byte[] bytes = jenkins.baixarArtefato(
            row.getProduto().getJenkinsUrl(), row.getJenkinsJob(),
            row.getProduto().getJenkinsUser(), row.getProduto().getJenkinsToken(),
            numero, art.pathDownload());
        String dest = dir.replaceAll("/+$", "") + "/artifacts/" + destNome;
        acesso.enviar(host, dest, bytes);
        copiados.add(destNome);
      }
    } catch (RuntimeException ex) {
      marcar(row, StatusBuildInstalacao.FAILED, ex.getMessage());
      return;
    }
    if (copiados.isEmpty()) {
      marcar(row, StatusBuildInstalacao.FAILED, "Nenhum arquivo válido para copiar.");
      return;
    }
    marcar(row, StatusBuildInstalacao.SUCCESS,
        "Copiado para artifacts/: " + String.join(", ", copiados));
  }

  private HostSshClient escolher(Host host) {
    if (LinuxManualImplantacaoAdapter.isLocal(host) && linuxManual.localFsHabilitado()) {
      return localFs;
    }
    return ssh;
  }

  private void marcar(BuildInstalacaoArtefato row, StatusBuildInstalacao status, String mensagem) {
    alvos.atualizarAcompanhamento(row.getId(), row.getBuildNumber(), status, mensagem);
    row.concluir(status, mensagem);
  }
}
