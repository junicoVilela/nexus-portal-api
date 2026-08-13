package com.nexus.portal.ai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AiGerarLoteDocumentoRequest(
    @NotEmpty @Size(max = 10) List<UUID> paginas) {}
