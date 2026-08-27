package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.HostRequest;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorHostRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * CRUD de hosts de execução (RF-002). Não armazena senha — só {@code credencialRef}.
 */
@Service("orchestratorHostService")
@RequiredArgsConstructor
public class HostService {

  private static final Pattern IPV4 =
      Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
  private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]+$");

  private final OrchestratorHostRepository repository;
  private final OrchestratorInstalacaoClienteRepository instalacaoRepository;

  @Transactional
  public Host criar(HostRequest request) {
    String codigo = normalizarCodigo(request.codigo());
    String hostname = request.hostname().trim();
    garantirCodigoUnico(codigo, null);
    garantirHostnameUnico(hostname, null);
    validarEnderecoIp(request.enderecoIp());

    TipoConexaoHost tipoConexao = resolverTipoConexao(request);
    boolean dockerDisponivel = resolverDockerDisponivel(request, tipoConexao);
    Integer porta = resolverPorta(request.portaConexao(), tipoConexao);

    Host host = new Host(codigo, request.nome(), hostname, request.sistemaOperacional(), tipoConexao);
    host.atualizar(
        codigo,
        request.nome(),
        hostname,
        request.enderecoIp(),
        request.sistemaOperacional(),
        dockerDisponivel,
        tipoConexao,
        porta,
        request.usuarioConexao(),
        request.credencialRef(),
        request.observacoes());
    if (request.ativo() != null) {
      host.alterarStatus(request.ativo());
    }
    return repository.save(host);
  }

  @Transactional
  public Host atualizar(UUID id, HostRequest request) {
    Host host = buscar(id);
    String codigo = normalizarCodigo(request.codigo());
    String hostname = request.hostname().trim();
    garantirCodigoUnico(codigo, id);
    garantirHostnameUnico(hostname, id);
    validarEnderecoIp(request.enderecoIp());

    TipoConexaoHost tipoConexao = resolverTipoConexao(request);
    boolean dockerDisponivel = resolverDockerDisponivel(request, tipoConexao);
    Integer porta = resolverPorta(request.portaConexao(), tipoConexao);

    host.atualizar(
        codigo,
        request.nome(),
        hostname,
        request.enderecoIp(),
        request.sistemaOperacional(),
        dockerDisponivel,
        tipoConexao,
        porta,
        request.usuarioConexao(),
        request.credencialRef(),
        request.observacoes());
    if (request.ativo() != null) {
      host.alterarStatus(request.ativo());
    }
    return host;
  }

  @Transactional
  public Host alterarStatus(UUID id, boolean ativo) {
    Host host = buscar(id);
    host.alterarStatus(ativo);
    return host;
  }

  @Transactional
  public void excluir(UUID id) {
    Host host = buscar(id);
    if (instalacaoRepository.existsByHost_Id(id)) {
      throw new BusinessException("Não é possível excluir o host. Remova as instalações vinculadas antes.");
    }
    repository.delete(host);
  }

  public Host buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Host não encontrado."));
  }

  public Page<Host> listar(String filtroTexto, Boolean ativo, SistemaOperacionalHost sistemaOperacional,
      Boolean dockerDisponivel, Pageable pageable) {
    Specification<Host> spec = (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();
      if (filtroTexto != null && !filtroTexto.isBlank()) {
        String like = "%" + filtroTexto.toLowerCase() + "%";
        preds.add(cb.or(
            cb.like(cb.lower(root.get("codigo")), like),
            cb.like(cb.lower(root.get("nome")), like),
            cb.like(cb.lower(root.get("hostname")), like),
            cb.like(cb.lower(cb.coalesce(root.get("enderecoIp"), "")), like)));
      }
      if (ativo != null) {
        preds.add(cb.equal(root.get("ativo"), ativo));
      }
      if (sistemaOperacional != null) {
        preds.add(cb.equal(root.get("sistemaOperacional"), sistemaOperacional));
      }
      if (dockerDisponivel != null) {
        preds.add(cb.equal(root.get("dockerDisponivel"), dockerDisponivel));
      }
      return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
    };
    return repository.findAll(spec, pageable);
  }

  private void garantirCodigoUnico(String codigo, UUID idAtual) {
    boolean duplicado = idAtual == null
        ? repository.existsByCodigoIgnoreCase(codigo)
        : repository.existsByCodigoIgnoreCaseAndIdNot(codigo, idAtual);
    if (duplicado) {
      throw new BusinessException("Já existe um host com esse código.");
    }
  }

  private void garantirHostnameUnico(String hostname, UUID idAtual) {
    boolean duplicado = idAtual == null
        ? repository.existsByHostnameIgnoreCase(hostname)
        : repository.existsByHostnameIgnoreCaseAndIdNot(hostname, idAtual);
    if (duplicado) {
      throw new BusinessException("Já existe um host com esse hostname.");
    }
  }

  private static String normalizarCodigo(String codigo) {
    return codigo.trim().toUpperCase();
  }

  static TipoConexaoHost resolverTipoConexao(HostRequest request) {
    if (request.tipoConexao() != null) {
      return request.tipoConexao();
    }
    return request.sistemaOperacional() == SistemaOperacionalHost.WINDOWS
        ? TipoConexaoHost.WINRM
        : TipoConexaoHost.SSH;
  }

  static boolean resolverDockerDisponivel(HostRequest request, TipoConexaoHost tipoConexao) {
    if (tipoConexao == TipoConexaoHost.DOCKER) {
      return true;
    }
    return Boolean.TRUE.equals(request.dockerDisponivel());
  }

  static Integer resolverPorta(Integer informada, TipoConexaoHost tipoConexao) {
    if (informada != null) {
      return informada;
    }
    return switch (tipoConexao) {
      case SSH -> 22;
      case WINRM -> 5985;
      case DOCKER -> 2376;
    };
  }

  static void validarEnderecoIp(String enderecoIp) {
    if (enderecoIp == null || enderecoIp.isBlank()) {
      return;
    }
    String ip = enderecoIp.trim();
    if (IPV4.matcher(ip).matches()) {
      return;
    }
    if (ip.contains(":") && IPV6.matcher(ip).matches()) {
      return;
    }
    throw new BusinessException("Endereço IP inválido. Informe IPv4 ou IPv6.");
  }
}
