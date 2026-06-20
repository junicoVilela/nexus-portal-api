package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Edita campos editáveis em RASCUNHO (cliente/produto/release são imutáveis). */
public record AtualizarEntregaRascunhoRequest(
    @NotNull AmbientePadrao ambiente,
    UUID responsavelId,
    @Size(max = 4000) String observacoes) {}
