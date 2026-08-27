package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HostRequest(
    @NotBlank @Size(max = 40)
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9-]*", message = "Código deve conter apenas letras, números e hífen")
    String codigo,
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Size(max = 255) String hostname,
    @Size(max = 45) String enderecoIp,
    @NotNull SistemaOperacionalHost sistemaOperacional,
    Boolean dockerDisponivel,
    TipoConexaoHost tipoConexao,
    @Min(1) @Max(65535) Integer portaConexao,
    @Size(max = 120) String usuarioConexao,
    @Size(max = 200) String credencialRef,
    @Size(max = 4000) String observacoes,
    Boolean ativo) {}
