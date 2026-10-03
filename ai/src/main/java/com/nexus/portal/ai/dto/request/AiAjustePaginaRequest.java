package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido de ajuste de uma página existente. {@code version}: a versão que o editor tem aberta;
 * {@code secaoId}: seção do esboço ({@code s1}, {@code s2}…) que limita o ajuste.
 */
public record AiAjustePaginaRequest(
    @NotBlank @Size(min = 10, max = 2_000) String instrucao,
    @Pattern(regexp = "s\\d{1,4}") String secaoId,
    @NotNull Long version) {
}
