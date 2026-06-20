package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.PrioridadeEntrega;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record ProximaEntregaRequest(
    @NotNull UUID clienteId,
    @NotNull UUID produtoId,
    UUID releaseId,
    @NotNull LocalDate dataPrevista,
    @NotNull AmbientePadrao ambiente,
    @NotNull PrioridadeEntrega prioridade,
    UUID responsavelId,
    @Size(max = 4000) String observacoes) {}
