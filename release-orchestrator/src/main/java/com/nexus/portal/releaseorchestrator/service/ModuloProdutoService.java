package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.AtualizarModuloProdutoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.CriarModuloProdutoRequest;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * CRUD do catálogo de módulos por produto (F0.6).
 *
 * Regras (spec §7):
 * <ul>
 *   <li>`codigo` e `tipo` são imutáveis após criação.</li>
 *   <li>`codigo` é único por produto (validação case-insensitive).</li>
 *   <li>Exclusão hard só se o módulo não tem artefato/release histórica
 *       (cobrado por FK no banco). Caso contrário, usar inativação.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ModuloProdutoService {

  private final ModuloProdutoRepository repository;
  private final ProdutoRhRepository produtoRepository;

  public List<ModuloProduto> listar(UUID produtoId) {
    requerirProduto(produtoId);
    return repository.findByProduto_IdOrderByOrdemAscNomeAsc(produtoId);
  }

  public ModuloProduto buscar(UUID produtoId, UUID id) {
    return repository.findByProduto_IdAndId(produtoId, id)
        .orElseThrow(() -> new NotFoundException("Módulo não encontrado para o produto informado."));
  }

  @Transactional
  public ModuloProduto criar(UUID produtoId, CriarModuloProdutoRequest request) {
    ProdutoRh produto = requerirProduto(produtoId);
    if (repository.existsByProduto_IdAndCodigoIgnoreCase(produtoId, request.codigo())) {
      throw new BusinessException("Já existe um módulo com esse código neste produto.");
    }
    int ordem = request.ordem() != null ? request.ordem() : 0;
    ModuloProduto modulo = new ModuloProduto(
        produto, request.codigo(), request.nome(), request.tipo(),
        request.geraDelta(), request.obrigatorio(), ordem, request.configEspecifica());
    return repository.save(modulo);
  }

  @Transactional
  public ModuloProduto atualizar(UUID produtoId, UUID id, AtualizarModuloProdutoRequest request) {
    ModuloProduto modulo = buscar(produtoId, id);
    modulo.atualizar(request.nome(), request.geraDelta(), request.obrigatorio(),
        request.configEspecifica());
    return modulo;
  }

  @Transactional
  public ModuloProduto alterarStatus(UUID produtoId, UUID id, boolean ativo) {
    ModuloProduto modulo = buscar(produtoId, id);
    modulo.alterarStatus(ativo);
    return modulo;
  }

  /**
   * Exclusão hard. Se o módulo já estiver referenciado em uma release histórica
   * ou possuir artefatos, a FK do banco lança DataIntegrityViolation — usar
   * {@link #alterarStatus} pra desativar nesses casos.
   */
  @Transactional
  public void excluir(UUID produtoId, UUID id) {
    ModuloProduto modulo = buscar(produtoId, id);
    repository.delete(modulo);
  }

  private ProdutoRh requerirProduto(UUID produtoId) {
    return produtoRepository.findById(produtoId)
        .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
  }
}
