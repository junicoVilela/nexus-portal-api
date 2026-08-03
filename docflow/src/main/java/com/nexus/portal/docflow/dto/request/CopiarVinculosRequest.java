package com.nexus.portal.docflow.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CopiarVinculosRequest(@NotNull UUID origemClienteId) {
}
