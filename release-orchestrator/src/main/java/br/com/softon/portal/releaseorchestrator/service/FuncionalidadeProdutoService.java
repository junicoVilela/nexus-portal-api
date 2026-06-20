package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.FuncionalidadeProdutoRequest;
import br.com.softon.portal.releaseorchestrator.entity.DominioProduto;
import br.com.softon.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorFuncionalidadeProdutoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * CRUD das funcionalidades dentro de um domínio do catálogo do produto (F1.3).
 *
 * Regras:
 * <ul>
 *   <li>`codigo` único por domínio, imutável após criação.</li>
 *   <li>O domínio precisa pertencer ao produto da rota (validação cruzada).</li>
 * </ul>
 */
@Service("orchestratorFuncionalidadeProdutoService")
@RequiredArgsConstructor
public class FuncionalidadeProdutoService {

  private final OrchestratorFuncionalidadeProdutoRepository repository;
  private final DominioProdutoService dominioService;

  public List<FuncionalidadeProduto> listar(UUID produtoId, UUID dominioId) {
    dominioService.buscar(produtoId, dominioId);
    return repository.findByDominio_IdOrderByOrdemAscNomeAsc(dominioId);
  }

  public FuncionalidadeProduto buscar(UUID produtoId, UUID dominioId, UUID id) {
    dominioService.buscar(produtoId, dominioId);
    return repository.findByDominio_IdAndId(dominioId, id)
        .orElseThrow(() -> new NotFoundException("Funcionalidade não encontrada."));
  }

  @Transactional
  public FuncionalidadeProduto criar(UUID produtoId, UUID dominioId,
      FuncionalidadeProdutoRequest request) {
    DominioProduto dominio = dominioService.buscar(produtoId, dominioId);
    if (repository.existsByDominio_IdAndCodigoIgnoreCase(dominioId, request.codigo())) {
      throw new BusinessException(
          "Já existe uma funcionalidade com esse código neste domínio.");
    }
    boolean critica = request.critica() != null && request.critica();
    int ordem = request.ordem() != null ? request.ordem() : 0;
    return repository.save(new FuncionalidadeProduto(
        dominio, request.nome(), request.codigo(),
        request.codigoLegado(), request.codigoOperacao(),
        request.descricao(), critica, ordem));
  }

  @Transactional
  public FuncionalidadeProduto atualizar(UUID produtoId, UUID dominioId, UUID id,
      FuncionalidadeProdutoRequest request) {
    FuncionalidadeProduto func = buscar(produtoId, dominioId, id);
    boolean critica = request.critica() != null ? request.critica() : func.isCritica();
    int ordem = request.ordem() != null ? request.ordem() : func.getOrdem();
    func.atualizar(request.nome(), request.codigoLegado(), request.codigoOperacao(),
        request.descricao(), critica, ordem);
    return func;
  }

  @Transactional
  public FuncionalidadeProduto alterarStatus(UUID produtoId, UUID dominioId, UUID id,
      boolean ativo) {
    FuncionalidadeProduto func = buscar(produtoId, dominioId, id);
    func.alterarStatus(ativo);
    return func;
  }

  @Transactional
  public void excluir(UUID produtoId, UUID dominioId, UUID id) {
    FuncionalidadeProduto func = buscar(produtoId, dominioId, id);
    repository.delete(func);
  }
}
