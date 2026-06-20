package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.SalvarClienteProdutoModuloRequest;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Módulos contratados pelo cliente para um produto (F1.5).
 *
 * Regras:
 * <ul>
 *   <li>Upsert por (clienteProduto, móduloProduto).</li>
 *   <li>Módulo precisa pertencer ao produto do clienteProduto
 *       (cross-produto rejeitado).</li>
 * </ul>
 */
@Service("orchestratorClienteProdutoModuloService")
@RequiredArgsConstructor
public class ClienteProdutoModuloService {

  private final OrchestratorClienteProdutoModuloRepository repository;
  private final ClienteProdutoService clienteProdutoService;
  private final ModuloProdutoRepository moduloRepository;

  public List<ClienteProdutoModulo> listar(UUID clienteId, UUID clienteProdutoId) {
    clienteProdutoService.buscar(clienteId, clienteProdutoId);
    return repository
        .findByClienteProduto_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(
            clienteProdutoId);
  }

  @Transactional
  public ClienteProdutoModulo salvar(UUID clienteId, UUID clienteProdutoId,
      UUID moduloProdutoId, SalvarClienteProdutoModuloRequest request) {
    ClienteProduto cp = clienteProdutoService.buscar(clienteId, clienteProdutoId);
    ModuloProduto modulo = moduloRepository.findById(moduloProdutoId)
        .orElseThrow(() -> new NotFoundException("Módulo do produto não encontrado."));
    if (!modulo.getProduto().getId().equals(cp.getProduto().getId())) {
      throw new BusinessException(
          "Módulo informado não pertence ao produto contratado por este cliente.");
    }

    return repository
        .findByClienteProduto_IdAndModuloProduto_Id(clienteProdutoId, moduloProdutoId)
        .map(existente -> {
          boolean ativo = request.ativo() == null ? existente.isAtivo() : request.ativo();
          existente.atualizar(request.versaoAtual(), ativo);
          return existente;
        })
        .orElseGet(() -> {
          ClienteProdutoModulo novo = new ClienteProdutoModulo(cp, modulo, request.versaoAtual());
          if (request.ativo() != null) {
            novo.setAtivo(request.ativo());
          }
          return repository.save(novo);
        });
  }

  @Transactional
  public void remover(UUID clienteId, UUID clienteProdutoId, UUID moduloProdutoId) {
    clienteProdutoService.buscar(clienteId, clienteProdutoId);
    ClienteProdutoModulo cpm = repository
        .findByClienteProduto_IdAndModuloProduto_Id(clienteProdutoId, moduloProdutoId)
        .orElseThrow(() -> new NotFoundException("Módulo contratado não encontrado."));
    repository.delete(cpm);
  }
}
