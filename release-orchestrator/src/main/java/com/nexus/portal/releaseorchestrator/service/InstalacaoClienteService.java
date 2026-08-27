package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.ConfiguracaoInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.InstalacaoClienteRequest;
import com.nexus.portal.releaseorchestrator.dto.request.RegistrarHealthInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ReservaPortaRequest;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.ConfiguracaoInstalacao;
import com.nexus.portal.releaseorchestrator.entity.HealthInstalacao;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.ProtocoloPorta;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorDeployInstalacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaInstalacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorHostRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * CRUD de instalações do produto por cliente (RF-003). O tipo de implantação
 * RPA discrimina Docker pull, Docker {@code .tar}, Linux manual e Windows manual.
 */
@Service("orchestratorInstalacaoClienteService")
@RequiredArgsConstructor
public class InstalacaoClienteService {

  private final OrchestratorInstalacaoClienteRepository repository;
  private final OrchestratorClienteRepository clienteRepository;
  private final OrchestratorHostRepository hostRepository;
  private final ProdutoRhRepository produtoRepository;
  private final ReservaPortaService reservaPortaService;
  private final OrchestratorEntregaInstalacaoRepository entregaInstalacaoRepository;
  private final OrchestratorDeployInstalacaoRepository deployRepository;
  private final EntityManager entityManager;

  @Transactional
  public InstalacaoCliente criar(InstalacaoClienteRequest request) {
    String codigo = normalizarCodigo(request.codigo());
    garantirCodigoUnico(codigo, null);
    Cliente cliente = buscarCliente(request.clienteId());
    Host host = buscarHost(request.hostId());
    ProdutoRh produto = buscarProduto(request.produtoId());
    garantirAlvoUnico(cliente.getId(), produto.getId(), host.getId(), request.ambiente(), null);
    validarCompatibilidade(host, request.tipoImplantacao());
    StatusInstalacao status = request.status() == null ? StatusInstalacao.INEXISTENTE : request.status();
    validarCamposDoTipo(request.tipoImplantacao(), status,
        request.imagemRef(), request.arquivoImagemRef(), request.diretorioInstalacao());

    InstalacaoCliente inst = new InstalacaoCliente(
        codigo, request.nome(), cliente, host, produto, request.tipoImplantacao(), request.ambiente());
    inst.atualizar(
        codigo,
        request.nome(),
        cliente,
        host,
        produto,
        request.tipoImplantacao(),
        status,
        request.ambiente(),
        request.imagemRef(),
        request.arquivoImagemRef(),
        request.diretorioInstalacao(),
        request.observacoes());
    inst.definirVersaoAtual(request.versaoAtual());
    aplicarConfiguracao(inst, request.configuracao());
    aplicarPortas(inst, host, null, request.portas(), status);
    return repository.save(inst);
  }

  @Transactional
  public InstalacaoCliente atualizar(UUID id, InstalacaoClienteRequest request) {
    InstalacaoCliente inst = buscar(id);
    String codigo = normalizarCodigo(request.codigo());
    garantirCodigoUnico(codigo, id);
    Cliente cliente = buscarCliente(request.clienteId());
    Host host = buscarHost(request.hostId());
    ProdutoRh produto = buscarProduto(request.produtoId());
    garantirAlvoUnico(cliente.getId(), produto.getId(), host.getId(), request.ambiente(), id);
    validarCompatibilidade(host, request.tipoImplantacao());
    StatusInstalacao status = request.status() == null ? inst.getStatus() : request.status();
    validarCamposDoTipo(request.tipoImplantacao(), status,
        request.imagemRef(), request.arquivoImagemRef(), request.diretorioInstalacao());

    inst.atualizar(
        codigo,
        request.nome(),
        cliente,
        host,
        produto,
        request.tipoImplantacao(),
        status,
        request.ambiente(),
        request.imagemRef(),
        request.arquivoImagemRef(),
        request.diretorioInstalacao(),
        request.observacoes());
    inst.definirVersaoAtual(request.versaoAtual());
    aplicarConfiguracao(inst, request.configuracao());
    aplicarPortas(inst, host, id, request.portas(), status);
    return inst;
  }

  @Transactional
  public InstalacaoCliente alterarStatus(UUID id, StatusInstalacao status) {
    InstalacaoCliente inst = buscar(id);
    validarCamposDoTipo(inst.getTipoImplantacao(), status,
        inst.getImagemRef(), inst.getArquivoImagemRef(), inst.getDiretorioInstalacao());
    inst.alterarStatus(status);
    return inst;
  }

  @Transactional
  public InstalacaoCliente registrarHealth(UUID id, RegistrarHealthInstalacaoRequest request) {
    InstalacaoCliente inst = buscar(id);
    HealthInstalacao health = request.health() == null ? HealthInstalacao.DESCONHECIDO : request.health();
    inst.registrarHealth(health, request.versaoAtual(), request.ultimoErro());
    return inst;
  }

  @Transactional
  public void excluir(UUID id) {
    InstalacaoCliente inst = buscar(id);
    if (entregaInstalacaoRepository.existsByInstalacao_Id(id)) {
      throw new BusinessException(
          "Não é possível excluir a instalação: ela está vinculada a uma ou mais entregas.");
    }
    if (deployRepository.existsByInstalacao_Id(id)) {
      throw new BusinessException(
          "Não é possível excluir a instalação: há histórico de implantação.");
    }
    repository.delete(inst);
  }

  public InstalacaoCliente buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Instalação não encontrada."));
  }

  @Transactional
  public Page<InstalacaoCliente> listar(String filtroTexto, UUID clienteId, UUID hostId, UUID produtoId,
      TipoImplantacao tipoImplantacao, StatusInstalacao status, AmbientePadrao ambiente, Pageable pageable) {
    Specification<InstalacaoCliente> spec = (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();
      if (filtroTexto != null && !filtroTexto.isBlank()) {
        String like = "%" + filtroTexto.toLowerCase() + "%";
        preds.add(cb.or(
            cb.like(cb.lower(root.get("codigo")), like),
            cb.like(cb.lower(root.get("nome")), like),
            cb.like(cb.lower(root.get("cliente").get("nome")), like),
            cb.like(cb.lower(root.get("cliente").get("sigla")), like),
            cb.like(cb.lower(root.get("host").get("codigo")), like),
            cb.like(cb.lower(root.get("produto").get("sigla")), like)));
      }
      if (clienteId != null) {
        preds.add(cb.equal(root.get("cliente").get("id"), clienteId));
      }
      if (hostId != null) {
        preds.add(cb.equal(root.get("host").get("id"), hostId));
      }
      if (produtoId != null) {
        preds.add(cb.equal(root.get("produto").get("id"), produtoId));
      }
      if (tipoImplantacao != null) {
        preds.add(cb.equal(root.get("tipoImplantacao"), tipoImplantacao));
      }
      if (status != null) {
        preds.add(cb.equal(root.get("status"), status));
      }
      if (ambiente != null) {
        preds.add(cb.equal(root.get("ambiente"), ambiente));
      }
      return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
    };
    return repository.findAll(spec, pageable);
  }

  static void validarCompatibilidade(Host host, TipoImplantacao tipo) {
    switch (tipo) {
      case DOCKER_PULL, DOCKER_TAR -> {
        if (!host.isDockerDisponivel()) {
          throw new BusinessException("Este tipo de implantação exige um host com Docker.");
        }
      }
      case LINUX_MANUAL -> {
        if (host.getSistemaOperacional() != SistemaOperacionalHost.LINUX) {
          throw new BusinessException("Instalação Linux manual exige um host Linux.");
        }
      }
      case WINDOWS_MANUAL -> {
        if (host.getSistemaOperacional() != SistemaOperacionalHost.WINDOWS) {
          throw new BusinessException("Instalação Windows manual exige um host Windows.");
        }
      }
    }
  }

  static void validarCamposDoTipo(TipoImplantacao tipo, StatusInstalacao status,
      String imagemRef, String arquivoImagemRef, String diretorioInstalacao) {
    if (status == StatusInstalacao.INEXISTENTE) {
      return;
    }
    switch (tipo) {
      case DOCKER_PULL -> {
        if (blank(imagemRef)) {
          throw new BusinessException("Informe a imagem a baixar (registry/repositório:tag).");
        }
      }
      case DOCKER_TAR -> {
        if (blank(arquivoImagemRef)) {
          throw new BusinessException("Informe o arquivo da imagem (.tar).");
        }
      }
      case LINUX_MANUAL, WINDOWS_MANUAL -> {
        if (blank(diretorioInstalacao)) {
          throw new BusinessException("Informe o diretório de instalação no host.");
        }
      }
    }
  }

  private void garantirCodigoUnico(String codigo, UUID idAtual) {
    boolean duplicado = idAtual == null
        ? repository.existsByCodigoIgnoreCase(codigo)
        : repository.existsByCodigoIgnoreCaseAndIdNot(codigo, idAtual);
    if (duplicado) {
      throw new BusinessException("Já existe uma instalação com esse código.");
    }
  }

  private void garantirAlvoUnico(UUID clienteId, UUID produtoId, UUID hostId, AmbientePadrao ambiente,
      UUID idAtual) {
    boolean duplicado = idAtual == null
        ? repository.existsByCliente_IdAndProduto_IdAndHost_IdAndAmbiente(
            clienteId, produtoId, hostId, ambiente)
        : repository.existsByCliente_IdAndProduto_IdAndHost_IdAndAmbienteAndIdNot(
            clienteId, produtoId, hostId, ambiente, idAtual);
    if (duplicado) {
      throw new BusinessException("Já existe instalação deste produto neste host e ambiente para o cliente.");
    }
  }

  private Cliente buscarCliente(UUID id) {
    return clienteRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
  }

  private Host buscarHost(UUID id) {
    return hostRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Host não encontrado."));
  }

  private ProdutoRh buscarProduto(UUID id) {
    return produtoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
  }

  private static String normalizarCodigo(String codigo) {
    return codigo.trim().toUpperCase();
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private void aplicarConfiguracao(InstalacaoCliente inst, ConfiguracaoInstalacaoRequest request) {
    if (request == null) {
      return;
    }
    ConfiguracaoInstalacao cfg = inst.getConfiguracao();
    if (cfg == null) {
      cfg = new ConfiguracaoInstalacao(inst);
      inst.definirConfiguracao(cfg);
    }
    cfg.atualizar(
        request.tipoBanco(),
        request.bancoHost(),
        request.bancoPorta(),
        request.bancoNome(),
        request.bancoUsuario(),
        request.bancoCredencialRef(),
        request.urlBackend(),
        request.urlFrontend(),
        request.parametros());
  }

  private void aplicarPortas(InstalacaoCliente inst, Host host, UUID instalacaoId,
      List<ReservaPortaRequest> portas, StatusInstalacao statusInstalacao) {
    if (portas == null) {
      return;
    }
    reservaPortaService.validarReservas(host.getId(), instalacaoId, portas);
    if (instalacaoId != null) {
      inst.getPortas().clear();
      entityManager.flush();
    }
    List<ReservaPorta> novas = new ArrayList<>();
    for (var req : portas) {
      ProtocoloPorta protocolo = req.protocolo() == null ? ProtocoloPorta.TCP : req.protocolo();
      StatusReservaPorta statusPorta = req.status() != null
          ? req.status()
          : (statusInstalacao == StatusInstalacao.ATIVA
              ? StatusReservaPorta.EM_USO
              : StatusReservaPorta.RESERVADA);
      novas.add(new ReservaPorta(host, inst, req.tipo(), req.papel(), req.porta(), protocolo, statusPorta));
    }
    inst.substituirPortas(novas);
  }
}
