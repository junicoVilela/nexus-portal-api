package br.com.softon.portal.docflow.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PaginaTemplateAplicacaoRequest(
    UUID projetoId,
    UUID moduloId,
    UUID clienteId,
    @Size(max = 200) String titulo,
    @Size(max = 120) String codigoTela) {
}
