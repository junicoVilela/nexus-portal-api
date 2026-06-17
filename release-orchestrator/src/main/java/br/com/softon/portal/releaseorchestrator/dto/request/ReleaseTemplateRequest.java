package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReleaseTemplateRequest(
        @NotBlank @Size(max = 200) String nome,
        @Size(max = 500) String descricao,
        TipoRelease tipoRelease,
        UUID produtoId,
        String estrutura,
        Boolean ativo
) {}
