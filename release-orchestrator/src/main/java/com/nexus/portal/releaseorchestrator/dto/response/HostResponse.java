package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HostResponse(
    UUID id,
    String codigo,
    String nome,
    String hostname,
    String enderecoIp,
    SistemaOperacionalHost sistemaOperacional,
    boolean dockerDisponivel,
    TipoConexaoHost tipoConexao,
    Integer portaConexao,
    String usuarioConexao,
    String credencialRef,
    boolean ativo,
    String observacoes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static HostResponse from(Host h) {
    return new HostResponse(
        h.getId(),
        h.getCodigo(),
        h.getNome(),
        h.getHostname(),
        h.getEnderecoIp(),
        h.getSistemaOperacional(),
        h.isDockerDisponivel(),
        h.getTipoConexao(),
        h.getPortaConexao(),
        h.getUsuarioConexao(),
        h.getCredencialRef(),
        h.isAtivo(),
        h.getObservacoes(),
        h.getCreatedAt(),
        h.getUpdatedAt());
  }
}
