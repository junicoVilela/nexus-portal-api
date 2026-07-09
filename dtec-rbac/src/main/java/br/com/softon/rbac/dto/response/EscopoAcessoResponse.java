package br.com.softon.rbac.dto.response;

import br.com.softon.rbac.entity.EscopoAcesso;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EscopoAcessoResponse(
    UUID id,
    UUID usuarioId,
    UUID grupoAcessoId,
    UUID clienteId,
    UUID ambienteId,
    UUID produtoId,
    String tipoAmbiente,
    boolean somenteLeitura,
    boolean ativo,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm) {

  public static EscopoAcessoResponse from(EscopoAcesso e) {
    return new EscopoAcessoResponse(
        e.getId(), e.getUsuarioId(), e.getGrupoId(),
        e.getClienteId(), e.getAmbienteId(), e.getProdutoId(),
        e.getTipoAmbiente() == null ? null : e.getTipoAmbiente().name(),
        e.isSomenteLeitura(), e.isAtivo(),
        e.getCreatedAt(), e.getUpdatedAt());
  }
}
