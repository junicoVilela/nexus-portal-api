package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.AlterarStatusProdutoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ProdutoRhRequest;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProdutoRhService {

  private final ProdutoRhRepository repository;
  private final ReleaseRepository releaseRepository;

  @Transactional
  public ProdutoRh criar(ProdutoRhRequest request) {
    String sigla = request.sigla().toUpperCase().trim();
    if (repository.existsBySiglaIgnoreCase(sigla)) {
      throw new BusinessException("Já existe um produto com a sigla informada.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    ProdutoRh produto = new ProdutoRh(
        request.nome().trim(), sigla, request.descricao(),
        request.cor(), request.responsavelId(), ativo);
    aplicarIntegracaoGithub(produto, request);
    aplicarIntegracaoJenkins(produto, request);
    return repository.save(produto);
  }

  @Transactional
  public ProdutoRh atualizar(UUID id, ProdutoRhRequest request) {
    ProdutoRh produto = buscar(id);
    String sigla = request.sigla().toUpperCase().trim();
    if (repository.existsBySiglaIgnoreCaseAndIdNot(sigla, id)) {
      throw new BusinessException("Já existe um produto com a sigla informada.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    produto.atualizar(request.nome().trim(), sigla, request.descricao(),
        request.cor(), request.responsavelId(), ativo);
    aplicarIntegracaoGithub(produto, request);
    aplicarIntegracaoJenkins(produto, request);
    return produto;
  }

  private void aplicarIntegracaoJenkins(ProdutoRh produto, ProdutoRhRequest req) {
    String url = req.jenkinsUrl();
    if (url != null) url = url.trim();
    if (url != null && url.isBlank()) url = null;
    if (url != null && url.endsWith("/")) url = url.substring(0, url.length() - 1);
    String job = req.jenkinsJob();
    if (job != null && job.isBlank()) job = null;
    String user = req.jenkinsUser();
    if (user != null && user.isBlank()) user = null;
    String triggerMode = req.jenkinsTriggerMode();
    if (triggerMode == null || triggerMode.isBlank()) triggerMode = "BUILD_ON_TAG";
    produto.atualizarIntegracaoJenkins(url, job, user, req.jenkinsToken(), triggerMode);
  }

  private void aplicarIntegracaoGithub(ProdutoRh produto, ProdutoRhRequest req) {
    String repo = req.repositorioGithub();
    if (repo != null && !repo.isBlank()) {
      repo = normalizarRepositorio(repo.trim());
    }
    String branch = req.branchPadrao();
    if (branch == null || branch.isBlank()) branch = "main";
    String padraoTag = req.padraoTag();
    if (padraoTag == null || padraoTag.isBlank()) padraoTag = "^v\\d+\\.\\d+\\.\\d+$";
    produto.atualizarIntegracaoGithub(repo, branch, padraoTag, req.githubToken());
  }

  /**
   * Aceita URL completa ou owner/repo e normaliza para owner/repo.
   * Ex.: https://github.com/nexus/nexus-ld → nexus/nexus-ld
   */
  private String normalizarRepositorio(String input) {
    String s = input.replaceFirst("(?i)^https?://github.com/", "");
    s = s.replaceFirst("\\.git$", "");
    s = s.replaceFirst("/$", "");
    return s;
  }

  @Transactional
  public ProdutoRh alterarStatus(UUID id, AlterarStatusProdutoRequest request) {
    ProdutoRh produto = buscar(id);
    produto.alterarStatus(request.ativo());
    return produto;
  }

  public Page<ProdutoRh> listar(String nome, Boolean ativo, Pageable pageable) {
    Specification<ProdutoRh> spec = (root, q, cb) -> cb.conjunction();
    if (nome != null && !nome.isBlank()) {
      String filtro = "%" + nome.toLowerCase().trim() + "%";
      spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("nome")), filtro));
    }
    if (ativo != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("ativo"), ativo));
    }
    return repository.findAll(spec, pageable);
  }

  public ProdutoRh buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
  }

  @Transactional
  public void excluir(UUID id) {
    ProdutoRh produto = buscar(id);
    if (releaseRepository.countByProdutoId(id) > 0) {
      throw new BusinessException("Não é possível excluir produto com releases vinculadas.");
    }
    repository.delete(produto);
  }
}
