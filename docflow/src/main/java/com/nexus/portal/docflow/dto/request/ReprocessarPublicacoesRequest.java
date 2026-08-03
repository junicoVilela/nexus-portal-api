package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ReprocessarPublicacoesRequest(
    @NotEmpty(message = "Informe ao menos uma publicação.")
    @Size(max = 100, message = "Reprocesse no máximo 100 publicações por vez.")
    List<UUID> ids) {
}
