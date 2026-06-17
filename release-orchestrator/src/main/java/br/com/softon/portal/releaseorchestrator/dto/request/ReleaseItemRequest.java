package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.CategoriaItem;
import br.com.softon.portal.releaseorchestrator.entity.VisibilidadeItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReleaseItemRequest(
        @NotNull  CategoriaItem categoria,
        @NotBlank @Size(max = 300) String titulo,
        String descricao,
        VisibilidadeItem visibilidade,
        Integer ordem,
        @Size(max = 100) String ticket,
        @Size(max = 100) String commit,
        @Size(max = 100) String pullRequest,
        UUID responsavelId
) {}
