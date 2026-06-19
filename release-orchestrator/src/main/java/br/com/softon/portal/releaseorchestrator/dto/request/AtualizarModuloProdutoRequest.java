package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de atualização de módulo. Não permite mudar {@code codigo} nem
 * {@code tipo} — imutáveis após criação (spec §7.1).
 */
public record AtualizarModuloProdutoRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotNull Boolean geraDelta,
    @NotNull Boolean obrigatorio,
    @Size(max = 10_000) String configEspecifica) {
}
