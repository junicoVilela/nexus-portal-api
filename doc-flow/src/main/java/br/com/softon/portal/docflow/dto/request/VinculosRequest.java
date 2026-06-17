package br.com.softon.portal.docflow.dto.request;

import java.util.List;
import java.util.UUID;

public record VinculosRequest(List<UUID> projetoIds, List<UUID> moduloIds, List<UUID> paginaIds) {
}
