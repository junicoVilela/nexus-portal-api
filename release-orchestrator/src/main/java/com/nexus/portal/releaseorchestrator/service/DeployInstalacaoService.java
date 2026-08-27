package com.nexus.portal.releaseorchestrator.service;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.releaseorchestrator.dto.request.ExecutarDeployRequest;
import com.nexus.portal.releaseorchestrator.dto.response.AlvosEntregaDeployResponse;
import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.DeployInstalacao;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaInstalacao;
import com.nexus.portal.releaseorchestrator.entity.HealthInstalacao;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.StatusDeploy;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.integration.docker.DockerEngineException;
import com.nexus.portal.releaseorchestrator.integration.ssh.SshHostException;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorDeployInstalacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * {@code deploy(releaseId, instalacaoId)} — entrega/pacote é opcional.
 * Dry-run não altera o host; REAL aplica Docker pull ou Linux manual
 * ({@code install.sh}: JDK, Tomcat, .env) e atualiza o inventário.
 */
@Service
@RequiredArgsConstructor
public class DeployInstalacaoService {

  private final OrchestratorDeployInstalacaoRepository repository;
  private final OrchestratorInstalacaoClienteRepository instalacaoRepository;
  private final ReleaseRepository releaseRepository;
  private final OrchestratorEntregaRepository entregaRepository;
  private final ManifestoImplantacaoService manifestoService;
  private final ImplantacaoAdapterResolver adapters;
  private final EscopoResolver escopoResolver;

  public ManifestoImplantacaoResponse preview(UUID releaseId, UUID instalacaoId) {
    InstalacaoCliente inst = buscarInstalacao(instalacaoId);
    if (!escopoResolver.podeAcessarCliente(inst.getCliente().getId())) {
      throw new NotFoundException("Instalação não encontrada.");
    }
    Release release = buscarRelease(releaseId);
    garantirProduto(release, inst);
    return manifestoService.resolver(release, inst);
  }

  public AlvosEntregaDeployResponse alvosDaEntrega(UUID entregaId) {
    Entrega entrega = entregaRepository.findById(entregaId)
        .orElseThrow(() -> new NotFoundException("Entrega não encontrada."));
    escopoResolver.assertPodeEscreverEmCliente(entrega.getCliente().getId());
    if (entrega.getAlvos() == null || entrega.getAlvos().isEmpty()) {
      throw new BusinessException("A entrega não tem instalações alvo. Pacote não é necessário: vincule instalações.");
    }
    List<AlvosEntregaDeployResponse.Alvo> alvos = entrega.getAlvos().stream()
        .map(EntregaInstalacao::getInstalacao)
        .map(i -> new AlvosEntregaDeployResponse.Alvo(i.getId(), i.getTipoImplantacao()))
        .toList();
    return new AlvosEntregaDeployResponse(entrega.getRelease().getId(), entrega.getId(), alvos);
  }

  @Transactional
  public DeployInstalacao executar(ExecutarDeployRequest request) {
    InstalacaoCliente inst = buscarInstalacao(request.instalacaoId());
    escopoResolver.assertPodeEscreverEmCliente(inst.getCliente().getId());
    if (inst.getStatus() == StatusInstalacao.INATIVA) {
      throw new BusinessException("Instalação inativa não pode receber deploy.");
    }
    if (!inst.getHost().isAtivo()) {
      throw new BusinessException("O host da instalação está inativo.");
    }
    Release release = buscarRelease(request.releaseId());
    garantirProduto(release, inst);
    if (!ReleaseDisponivelDeployService.IMPLANTAVEIS.contains(release.getStatus())) {
      throw new BusinessException(
          "Só é possível implantar release em desenvolvimento, em revisão, aprovada ou publicada.");
    }
    ModoDeploy modo = request.modoOuPadrao();
    if (modo == ModoDeploy.REAL
        && inst.getTipoImplantacao() != TipoImplantacao.DOCKER_PULL
        && inst.getTipoImplantacao() != TipoImplantacao.LINUX_MANUAL) {
      throw new BusinessException(
          "Modo REAL só está disponível para Docker pull e Linux manual. Os demais tipos continuam em dry-run.");
    }
    Entrega entrega = resolverEntrega(request.entregaId(), release, inst);
    ManifestoImplantacaoResponse manifesto = manifestoService.resolver(release, inst);
    manifestoService.validarParaTipo(inst.getTipoImplantacao(), manifesto);

    OperacaoDeploy operacao = inst.getStatus() == StatusInstalacao.INEXISTENTE
        ? OperacaoDeploy.CRIAR
        : OperacaoDeploy.ATUALIZAR;

    if (modo == ModoDeploy.REAL && inst.getTipoImplantacao() == TipoImplantacao.DOCKER_PULL
        && !inst.getHost().isDockerDisponivel()) {
      DeployInstalacao fail = new DeployInstalacao(
          release, inst, entrega, operacao, modo,
          inst.getVersaoAtual(), release.getVersao(),
          manifesto.imagemRef(), manifesto.arquivoImagemRef(), manifesto.diretorioInstalacao(),
          manifesto.fingerprint(), operadorAtual());
      fail.marcarFalhou("Modo REAL exige host com Docker disponível.");
      inst.registrarHealth(HealthInstalacao.INDISPONIVEL, null, fail.getErro());
      return repository.save(fail);
    }

    boolean forcar = Boolean.TRUE.equals(request.forcar());
    if (!forcar) {
      var existente = repository
          .findFirstByRelease_IdAndInstalacao_IdAndFingerprintAndModoAndStatusOrderByCreatedAtDesc(
              release.getId(), inst.getId(), manifesto.fingerprint(), modo, StatusDeploy.CONCLUIDO);
      if (existente.isPresent()) {
        OperacaoDeploy operacaoSkip = inst.getStatus() == StatusInstalacao.INEXISTENTE
            ? OperacaoDeploy.CRIAR
            : OperacaoDeploy.ATUALIZAR;
        DeployInstalacao skip = new DeployInstalacao(
            release, inst, entrega, operacaoSkip, modo,
            inst.getVersaoAtual(), release.getVersao(),
            manifesto.imagemRef(), manifesto.arquivoImagemRef(), manifesto.diretorioInstalacao(),
            manifesto.fingerprint(), operadorAtual());
        String como = modo == ModoDeploy.REAL ? "aplicado" : "simulado";
        skip.marcarIgnorado("Idempotente: manifesto já " + como + " em " + existente.get().getId() + ".");
        return repository.save(skip);
      }
    }

    DeployInstalacao deploy = new DeployInstalacao(
        release, inst, entrega, operacao, modo,
        inst.getVersaoAtual(), release.getVersao(),
        manifesto.imagemRef(), manifesto.arquivoImagemRef(), manifesto.diretorioInstalacao(),
        manifesto.fingerprint(), operadorAtual());
    deploy = repository.save(deploy);
    deploy.marcarEmAndamento();

    ImplantacaoAdapter.Resultado resultado;
    try {
      resultado = adapters.resolver(modo, inst.getTipoImplantacao()).aplicar(
          new ImplantacaoAdapter.Contexto(release, inst, inst.getHost(), operacao, manifesto));
    } catch (DockerEngineException | SshHostException | BusinessException ex) {
      deploy.marcarFalhou(ex.getMessage());
      if (modo == ModoDeploy.REAL) {
        inst.registrarHealth(HealthInstalacao.INDISPONIVEL, null, ex.getMessage());
      }
      return deploy;
    } catch (RuntimeException ex) {
      String msg = ex.getMessage() == null ? "Falha inesperada no adaptador." : ex.getMessage();
      deploy.marcarFalhou(msg);
      if (modo == ModoDeploy.REAL) {
        inst.registrarHealth(HealthInstalacao.INDISPONIVEL, null, msg);
      }
      return deploy;
    }
    if (resultado.ok()) {
      deploy.marcarConcluido(resultado.mensagem());
      if (modo == ModoDeploy.REAL) {
        aplicarInventario(inst, release.getVersao());
      }
    } else {
      deploy.marcarFalhou(resultado.erro());
      if (modo == ModoDeploy.REAL) {
        inst.registrarHealth(HealthInstalacao.INDISPONIVEL, null, resultado.erro());
      }
    }
    return deploy;
  }

  @Transactional
  public DeployInstalacao executarCicloVida(UUID instalacaoId, OperacaoDeploy operacao, ModoDeploy modoReq) {
    if (!operacao.cicloVida()) {
      throw new BusinessException("Use INICIAR ou PARAR para o ciclo de vida da instalação.");
    }
    InstalacaoCliente inst = buscarInstalacao(instalacaoId);
    escopoResolver.assertPodeEscreverEmCliente(inst.getCliente().getId());
    if (inst.getStatus() == StatusInstalacao.INEXISTENTE) {
      throw new BusinessException("Crie a instalação no host antes de iniciar ou parar.");
    }
    if (inst.getStatus() == StatusInstalacao.INATIVA && operacao == OperacaoDeploy.INICIAR) {
      throw new BusinessException("Instalação inativa não pode ser iniciada.");
    }
    if (!inst.getHost().isAtivo()) {
      throw new BusinessException("O host da instalação está inativo.");
    }
    ModoDeploy modo = modoReq == null ? ModoDeploy.REAL : modoReq;
    if (modo == ModoDeploy.REAL
        && inst.getTipoImplantacao() != TipoImplantacao.DOCKER_PULL
        && inst.getTipoImplantacao() != TipoImplantacao.LINUX_MANUAL) {
      throw new BusinessException(
          "Start/stop em modo REAL só está disponível para Docker pull e Linux manual.");
    }
    Release release = resolverReleaseCicloVida(inst);
    ManifestoImplantacaoResponse manifesto = manifestoService.resolver(release, inst);
    String versao = inst.getVersaoAtual() == null || inst.getVersaoAtual().isBlank()
        ? release.getVersao()
        : inst.getVersaoAtual();

    DeployInstalacao deploy = new DeployInstalacao(
        release, inst, null, operacao, modo,
        inst.getVersaoAtual(), versao,
        manifesto.imagemRef(), manifesto.arquivoImagemRef(), manifesto.diretorioInstalacao(),
        "ciclo:" + operacao.name().toLowerCase(), operadorAtual());
    deploy = repository.save(deploy);
    deploy.marcarEmAndamento();

    ImplantacaoAdapter.Resultado resultado;
    try {
      resultado = adapters.resolver(modo, inst.getTipoImplantacao()).aplicar(
          new ImplantacaoAdapter.Contexto(release, inst, inst.getHost(), operacao, manifesto));
    } catch (DockerEngineException | SshHostException | BusinessException ex) {
      deploy.marcarFalhou(ex.getMessage());
      if (modo == ModoDeploy.REAL) {
        inst.registrarHealth(HealthInstalacao.INDISPONIVEL, null, ex.getMessage());
      }
      return deploy;
    } catch (RuntimeException ex) {
      String msg = ex.getMessage() == null ? "Falha inesperada no adaptador." : ex.getMessage();
      deploy.marcarFalhou(msg);
      if (modo == ModoDeploy.REAL) {
        inst.registrarHealth(HealthInstalacao.INDISPONIVEL, null, msg);
      }
      return deploy;
    }
    if (resultado.ok()) {
      deploy.marcarConcluido(resultado.mensagem());
      if (modo == ModoDeploy.REAL) {
        inst.registrarHealth(
            operacao == OperacaoDeploy.INICIAR ? HealthInstalacao.SAUDAVEL : HealthInstalacao.INDISPONIVEL,
            inst.getVersaoAtual(),
            null);
      }
    } else {
      deploy.marcarFalhou(resultado.erro());
      if (modo == ModoDeploy.REAL) {
        inst.registrarHealth(HealthInstalacao.INDISPONIVEL, null, resultado.erro());
      }
    }
    return deploy;
  }

  public DeployInstalacao buscar(UUID id) {
    DeployInstalacao deploy = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Deploy não encontrado."));
    if (!escopoResolver.podeAcessarCliente(deploy.getInstalacao().getCliente().getId())) {
      throw new NotFoundException("Deploy não encontrado.");
    }
    return deploy;
  }

  public Page<DeployInstalacao> listar(UUID instalacaoId, UUID releaseId, UUID entregaId, Pageable pageable) {
    if (instalacaoId == null && releaseId == null && entregaId == null) {
      throw new BusinessException("Informe instalacaoId, releaseId ou entregaId.");
    }
    Specification<DeployInstalacao> spec = (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();
      if (instalacaoId != null) {
        preds.add(cb.equal(root.get("instalacao").get("id"), instalacaoId));
      }
      if (releaseId != null) {
        preds.add(cb.equal(root.get("release").get("id"), releaseId));
      }
      if (entregaId != null) {
        preds.add(cb.equal(root.get("entrega").get("id"), entregaId));
      }
      escopoResolver.clientesPermitidosDoUsuarioAtual().ifPresent(ids ->
          preds.add(root.get("instalacao").get("cliente").get("id").in(ids)));
      return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
    };
    return repository.findAll(spec, pageable);
  }

  private void aplicarInventario(InstalacaoCliente inst, String versao) {
    inst.alterarStatus(StatusInstalacao.ATIVA);
    inst.definirVersaoAtual(versao);
    inst.registrarHealth(HealthInstalacao.SAUDAVEL, versao, null);
    if (inst.getPortas() != null) {
      for (ReservaPorta porta : inst.getPortas()) {
        porta.marcarEmUso();
      }
    }
  }

  private Release resolverReleaseCicloVida(InstalacaoCliente inst) {
    var ultimo = repository.findFirstByInstalacao_IdAndOperacaoInAndStatusOrderByCreatedAtDesc(
        inst.getId(),
        List.of(OperacaoDeploy.CRIAR, OperacaoDeploy.ATUALIZAR),
        StatusDeploy.CONCLUIDO);
    if (ultimo.isPresent() && ultimo.get().getRelease() != null) {
      return ultimo.get().getRelease();
    }
    String versao = inst.getVersaoAtual();
    if (versao != null && !versao.isBlank()) {
      return releaseRepository.findFirstByProduto_IdAndVersao(inst.getProduto().getId(), versao)
          .orElseThrow(() -> new BusinessException(
              "Não achei a release " + versao + " do produto. Implante pelo portal antes de iniciar/parar."));
    }
    throw new BusinessException("Implante uma release nesta instalação antes de iniciar ou parar.");
  }

  private Entrega resolverEntrega(UUID entregaId, Release release, InstalacaoCliente inst) {
    if (entregaId == null) {
      return null;
    }
    Entrega entrega = entregaRepository.findById(entregaId)
        .orElseThrow(() -> new NotFoundException("Entrega não encontrada."));
    if (!entrega.getRelease().getId().equals(release.getId())) {
      throw new BusinessException("A entrega não é desta release.");
    }
    if (!entrega.getCliente().getId().equals(inst.getCliente().getId())
        || !entrega.getProduto().getId().equals(inst.getProduto().getId())) {
      throw new BusinessException("A instalação não pertence ao cliente/produto da entrega.");
    }
    return entrega;
  }

  private void garantirProduto(Release release, InstalacaoCliente inst) {
    if (!release.getProduto().getId().equals(inst.getProduto().getId())) {
      throw new BusinessException("A release não pertence ao produto da instalação.");
    }
  }

  private InstalacaoCliente buscarInstalacao(UUID id) {
    return instalacaoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Instalação não encontrada."));
  }

  private Release buscarRelease(UUID id) {
    return releaseRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
  }

  private static String operadorAtual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
      return null;
    }
    return auth.getName();
  }
}
