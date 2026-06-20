package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.ClienteRequest;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
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
 * CRUD do cliente operacional do release-orchestrator (F1.2). Não confundir
 * com o Cliente do DocFlow ({@code tb_cliente}).
 */
@Service("orchestratorClienteService")
@RequiredArgsConstructor
public class ClienteService {

  private final OrchestratorClienteRepository repository;

  @Transactional
  public Cliente criar(ClienteRequest request) {
    String sigla = request.sigla().trim().toUpperCase();
    if (repository.existsBySiglaIgnoreCase(sigla)) {
      throw new BusinessException("Já existe um cliente com essa sigla.");
    }
    if (cnpjPreenchido(request.cnpj()) && repository.existsByCnpj(request.cnpj())) {
      throw new BusinessException("Já existe um cliente com esse CNPJ.");
    }

    Cliente cliente = new Cliente(request.nome(), sigla, request.ambientePadrao());
    aplicarOpcionais(cliente, request);
    if (request.ativo() != null) {
      cliente.alterarStatus(request.ativo());
    }
    return repository.save(cliente);
  }

  @Transactional
  public Cliente atualizar(UUID id, ClienteRequest request) {
    Cliente cliente = buscar(id);
    String sigla = request.sigla().trim().toUpperCase();
    if (repository.existsBySiglaIgnoreCaseAndIdNot(sigla, id)) {
      throw new BusinessException("Já existe um cliente com essa sigla.");
    }
    if (cnpjPreenchido(request.cnpj()) && repository.existsByCnpjAndIdNot(request.cnpj(), id)) {
      throw new BusinessException("Já existe um cliente com esse CNPJ.");
    }
    cliente.atualizar(
        request.nome(),
        request.razaoSocial(),
        request.cnpj(),
        sigla,
        request.responsavelComercialId(),
        request.ambientePadrao(),
        request.tipoBanco(),
        request.codificacao(),
        request.fusoHorario(),
        request.observacoes());
    if (request.ativo() != null) {
      cliente.alterarStatus(request.ativo());
    }
    return cliente;
  }

  @Transactional
  public Cliente alterarStatus(UUID id, boolean ativo) {
    Cliente cliente = buscar(id);
    cliente.alterarStatus(ativo);
    return cliente;
  }

  @Transactional
  public void excluir(UUID id) {
    Cliente cliente = buscar(id);
    repository.delete(cliente);
  }

  public Cliente buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
  }

  public Page<Cliente> listar(String filtroTexto, Boolean ativo, Pageable pageable) {
    Specification<Cliente> spec = (root, query, cb) -> {
      List<Predicate> preds = new ArrayList<>();
      if (filtroTexto != null && !filtroTexto.isBlank()) {
        String like = "%" + filtroTexto.toLowerCase() + "%";
        preds.add(cb.or(
            cb.like(cb.lower(root.get("nome")), like),
            cb.like(cb.lower(root.get("sigla")), like)));
      }
      if (ativo != null) {
        preds.add(cb.equal(root.get("ativo"), ativo));
      }
      return cb.and(preds.toArray(new Predicate[0]));
    };
    return repository.findAll(spec, pageable);
  }

  private void aplicarOpcionais(Cliente cliente, ClienteRequest request) {
    cliente.atualizar(
        request.nome(),
        request.razaoSocial(),
        request.cnpj(),
        request.sigla().trim().toUpperCase(),
        request.responsavelComercialId(),
        request.ambientePadrao(),
        request.tipoBanco(),
        request.codificacao(),
        request.fusoHorario(),
        request.observacoes());
  }

  private boolean cnpjPreenchido(String cnpj) {
    return cnpj != null && !cnpj.isBlank();
  }
}
