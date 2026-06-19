package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de criação de módulo. {@code codigo} segue regex
 * {@code [a-z0-9-]+} e é único por produto; {@code tipo} é imutável após
 * criação. Demais campos editáveis via {@link AtualizarModuloProdutoRequest}.
 */
public record CriarModuloProdutoRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Size(max = 80) @Pattern(regexp = "[a-z0-9-]+") String codigo,
    @NotNull TipoModulo tipo,
    Boolean geraDelta,
    Boolean obrigatorio,
    Integer ordem,
    @Size(max = 10_000) String configEspecifica) {
}
