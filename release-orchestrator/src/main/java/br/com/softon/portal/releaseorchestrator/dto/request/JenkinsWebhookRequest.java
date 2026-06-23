package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload do webhook do Jenkins (S11 P2). Jenkins envia uma chamada por
 * transição de status (started, completed) ou via plugin Notification.
 *
 * <p>Esperado em JSON:
 * <pre>{
 *   "produtoSigla": "DTECLD",
 *   "versao": "1.5.0",
 *   "status": "EM_ANDAMENTO",
 *   "numero": 42,
 *   "url": "https://jenkins.softon.local/job/dtec-ld-build/42/"
 * }</pre>
 *
 * @param status EM_ANDAMENTO | SUCCESS | FAILED | UNSTABLE | ABORTED.
 *               Caller normaliza (Jenkins manda SUCCESS/FAILURE/etc).
 */
public record JenkinsWebhookRequest(
    @NotBlank @Size(max = 20) String produtoSigla,
    @NotBlank @Size(max = 50) String versao,
    @NotBlank @Size(max = 30) String status,
    Integer numero,
    @Size(max = 500) String url) {
}
