package br.com.softon.rbac.dto.request;

import java.util.UUID;

public record EscopoAcessoRequest(
    UUID usuarioId,
    UUID grupoAcessoId,
    UUID clienteId,
    UUID ambienteId,
    UUID produtoId,
    String tipoAmbiente,
    boolean somenteLeitura,
    boolean ativo) {
}
