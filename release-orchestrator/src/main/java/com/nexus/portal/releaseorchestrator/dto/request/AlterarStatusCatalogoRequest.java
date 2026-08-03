package com.nexus.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotNull;

/** Reutilizado por DominioProduto e FuncionalidadeProduto. */
public record AlterarStatusCatalogoRequest(@NotNull Boolean ativo) {}
