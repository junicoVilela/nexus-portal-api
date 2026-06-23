package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.JenkinsWebhookRequest;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Processa notificações do webhook Jenkins (S11 P2). Resolve a Release pelo
 * par {@code produtoSigla + versao}, normaliza o status e persiste o
 * snapshot em {@link Release#getUltimoBuildStatus()} via
 * {@link Release#atualizarBuildStatus(String, Integer, String)}.
 *
 * <p>Idempotente: cada chamada sobrescreve o snapshot anterior. Jenkins
 * notifica started → completed, então o status no portal segue a transição.
 *
 * <p>Tag {@code v1.5.0} é mapeada para versão {@code 1.5.0} (strip do
 * prefixo {@code v} — convenção do guia 40-guia-versao-tag).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsWebhookService {

  private final ReleaseRepository releaseRepository;

  @Transactional
  public void processar(JenkinsWebhookRequest req) {
    String versao = req.versao().startsWith("v") ? req.versao().substring(1) : req.versao();
    Release release = releaseRepository
        .findByProdutoSiglaAndVersao(req.produtoSigla(), versao)
        .orElseThrow(() -> new NotFoundException(
            "Release " + req.produtoSigla() + "/" + versao + " não encontrada."));

    String statusNormalizado = normalizarStatus(req.status());
    release.atualizarBuildStatus(statusNormalizado, req.numero(), req.url());
    log.info("Webhook Jenkins: release {}/{} → status {} (build #{})",
        req.produtoSigla(), versao, statusNormalizado, req.numero());
  }

  /**
   * Aceita os formatos comuns do Jenkins (SUCCESS/FAILURE) e do portal
   * (EM_ANDAMENTO), normalizando para o enum da constraint.
   */
  private String normalizarStatus(String raw) {
    String upper = raw.toUpperCase();
    return switch (upper) {
      case "STARTED", "BUILDING", "IN_PROGRESS", "EM_ANDAMENTO" -> "EM_ANDAMENTO";
      case "SUCCESS", "SUCCESSFUL", "STABLE" -> "SUCCESS";
      case "FAILURE", "FAILED", "FAIL" -> "FAILED";
      case "UNSTABLE" -> "UNSTABLE";
      case "ABORTED", "CANCELED", "CANCELLED" -> "ABORTED";
      default -> throw new IllegalArgumentException(
          "Status de build inválido: " + raw
              + " (esperado: EM_ANDAMENTO/SUCCESS/FAILED/UNSTABLE/ABORTED)");
    };
  }
}
