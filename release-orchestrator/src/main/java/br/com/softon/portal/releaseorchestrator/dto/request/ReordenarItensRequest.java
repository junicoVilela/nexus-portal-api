package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReordenarItensRequest(@NotNull List<ItemOrdem> ordens) {

    public record ItemOrdem(@NotNull UUID id, @NotNull Integer ordem) {}
}
