package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.AtualizarClienteProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ContratarProdutoRequest;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.rbac.service.EscopoResolver;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Contratos cliente × produto (F1.5). Um cliente pode contratar N produtos,
 * cada um com seu ambiente (PROD/HOM/DEV/TEST).
 *
 * Regras:
 * <ul>
 *   <li>UQ por (cliente, produto) — não há duplicidade.</li>
 *   <li>Exclusão hard cascateia módulos contratados (DB ON DELETE CASCADE).</li>
 * </ul>
 */
@Service("orchestratorClienteProdutoService")
@RequiredArgsConstructor
public class ClienteProdutoService {

  private final OrchestratorClienteProdutoRepository repository;
  private final ProdutoRhRepository produtoRepository;
  private final ClienteService clienteService;
  private final EscopoResolver escopoResolver;

  public List<ClienteProduto> listar(UUID clienteId) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_IdOrderByProduto_NomeAsc(clienteId);
  }

  public ClienteProduto buscar(UUID clienteId, UUID id) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_IdAndId(clienteId, id)
        .orElseThrow(() -> new NotFoundException("Contrato não encontrado para o cliente."));
  }

  @Transactional
  public ClienteProduto contratar(UUID clienteId, ContratarProdutoRequest request) {
    Cliente cliente = clienteService.buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    if (repository.existsByCliente_IdAndProduto_Id(clienteId, request.produtoId())) {
      throw new BusinessException("Cliente já contrata esse produto.");
    }
    ProdutoRh produto = produtoRepository.findById(request.produtoId())
        .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
    return repository.save(new ClienteProduto(cliente, produto, request.ambiente()));
  }

  @Transactional
  public ClienteProduto atualizar(UUID clienteId, UUID id,
      AtualizarClienteProdutoRequest request) {
    ClienteProduto cp = buscar(clienteId, id);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    boolean ativo = request.ativo() == null ? cp.isAtivo() : request.ativo();
    cp.atualizar(request.ambiente(), ativo);
    return cp;
  }

  @Transactional
  public void rescindir(UUID clienteId, UUID id) {
    ClienteProduto cp = buscar(clienteId, id);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    repository.delete(cp);
  }
}
