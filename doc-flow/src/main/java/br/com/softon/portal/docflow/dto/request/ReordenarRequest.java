package br.com.softon.portal.docflow.dto.request;

import java.util.List;
import java.util.UUID;

public record ReordenarRequest(List<UUID> paginaIds) {
}
