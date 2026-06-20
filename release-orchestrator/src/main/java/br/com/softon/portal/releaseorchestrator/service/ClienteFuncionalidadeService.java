package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.SalvarClienteFuncionalidadeRequest;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ClienteFuncionalidadeProduto;
import br.com.softon.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import br.com.softon.portal.releaseorchestrator.entity.OrigemFuncionalidade;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteFuncionalidadeRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorFuncionalidadeProdutoRepository;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Camada B: gerencia o subconjunto de funcionalidades habilitadas por cliente
 * (matriz cliente × funcionalidade do produto — F1.4).
 *
 * Regras:
 * <ul>
 *   <li>Upsert por par (cliente, funcionalidade) — UQ no banco.</li>
 *   <li>Origem default = MANUAL quando o request não informa.</li>
 *   <li>Funcionalidades ausentes da matriz contam como NÃO habilitadas
 *       (interpretação default — a UI deve mostrá-las desabilitadas).</li>
 * </ul>
 */
@Service("orchestratorClienteFuncionalidadeService")
@RequiredArgsConstructor
public class ClienteFuncionalidadeService {

  private final OrchestratorClienteFuncionalidadeRepository repository;
  private final OrchestratorFuncionalidadeProdutoRepository funcionalidadeRepository;
  private final ClienteService clienteService;

  public List<ClienteFuncionalidadeProduto> listarPorCliente(UUID clienteId) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_Id(clienteId);
  }

  public List<ClienteFuncionalidadeProduto> listarPorClienteEProduto(
      UUID clienteId, UUID produtoId) {
    clienteService.buscar(clienteId);
    return repository.findByClienteEProduto(clienteId, produtoId);
  }

  @Transactional
  public ClienteFuncionalidadeProduto salvar(UUID clienteId, UUID funcionalidadeId,
      SalvarClienteFuncionalidadeRequest request) {
    Cliente cliente = clienteService.buscar(clienteId);
    FuncionalidadeProduto funcionalidade = funcionalidadeRepository.findById(funcionalidadeId)
        .orElseThrow(() -> new NotFoundException("Funcionalidade não encontrada."));
    OrigemFuncionalidade origem = request.origem() != null
        ? request.origem() : OrigemFuncionalidade.MANUAL;

    return repository.findByCliente_IdAndFuncionalidade_Id(clienteId, funcionalidadeId)
        .map(existente -> {
          existente.atualizar(request.habilitada(), origem);
          return existente;
        })
        .orElseGet(() -> repository.save(new ClienteFuncionalidadeProduto(
            cliente, funcionalidade, request.habilitada(), origem)));
  }

  @Transactional
  public void remover(UUID clienteId, UUID funcionalidadeId) {
    ClienteFuncionalidadeProduto cf = repository
        .findByCliente_IdAndFuncionalidade_Id(clienteId, funcionalidadeId)
        .orElseThrow(() -> new NotFoundException("Vínculo cliente↔funcionalidade não encontrado."));
    repository.delete(cf);
  }
}
