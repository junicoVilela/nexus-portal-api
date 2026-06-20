package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseModuloVersao;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseModuloVersaoRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Gerencia o vínculo release ↔ versão por módulo (F0.4).
 *
 * Regras:
 * <ul>
 *   <li>Upsert por (release, módulo) — uma versão por par.</li>
 *   <li>Módulo precisa pertencer ao produto da release (cross-produto rejeitado).</li>
 *   <li>Release {@code PUBLICADA} ou {@code CANCELADA} ⇒ vínculos imutáveis.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReleaseModuloVersaoService {

  private final ReleaseModuloVersaoRepository repository;
  private final ReleaseRepository releaseRepository;
  private final ModuloProdutoRepository moduloRepository;

  public List<ReleaseModuloVersao> listar(UUID releaseId) {
    requerirRelease(releaseId);
    return repository
        .findByRelease_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(releaseId);
  }

  @Transactional
  public ReleaseModuloVersao salvar(UUID releaseId, UUID moduloId, String versao) {
    ReleaseEModulo ctx = requerirReleaseEModulo(releaseId, moduloId);
    bloquearSeReleaseImutavel(ctx.release());

    return repository.findByRelease_IdAndModuloProduto_Id(releaseId, moduloId)
        .map(existente -> {
          existente.alterarVersao(versao);
          return existente;
        })
        .orElseGet(() -> repository.save(
            new ReleaseModuloVersao(ctx.release(), ctx.modulo(), versao)));
  }

  @Transactional
  public void remover(UUID releaseId, UUID moduloId) {
    ReleaseModuloVersao vinculo = repository
        .findByRelease_IdAndModuloProduto_Id(releaseId, moduloId)
        .orElseThrow(() -> new NotFoundException("Vínculo não encontrado."));
    bloquearSeReleaseImutavel(vinculo.getRelease());
    repository.delete(vinculo);
  }

  private void bloquearSeReleaseImutavel(Release release) {
    ReleaseStatus s = release.getStatus();
    if (s == ReleaseStatus.PUBLICADA || s == ReleaseStatus.CANCELADA) {
      throw new BusinessException(
          "Release " + s + " é imutável — versões por módulo não podem ser alteradas.");
    }
  }

  private Release requerirRelease(UUID releaseId) {
    return releaseRepository.findById(releaseId)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
  }

  private ReleaseEModulo requerirReleaseEModulo(UUID releaseId, UUID moduloId) {
    Release release = requerirRelease(releaseId);
    ModuloProduto modulo = moduloRepository.findById(moduloId)
        .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
    if (!modulo.getProduto().getId().equals(release.getProduto().getId())) {
      throw new BusinessException(
          "Módulo informado não pertence ao produto da release.");
    }
    return new ReleaseEModulo(release, modulo);
  }

  private record ReleaseEModulo(Release release, ModuloProduto modulo) {}
}
