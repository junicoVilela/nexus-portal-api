package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record InstalacaoClienteRequest(
    @NotBlank @Size(max = 40)
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9-]*", message = "Código deve conter apenas letras, números e hífen")
    String codigo,
    @NotBlank @Size(max = 200) String nome,
    @NotNull UUID clienteId,
    @NotNull UUID hostId,
    @NotNull UUID produtoId,
    @NotNull TipoImplantacao tipoImplantacao,
    StatusInstalacao status,
    @NotNull AmbientePadrao ambiente,
    @Size(max = 300) String imagemRef,
    @Size(max = 400) String arquivoImagemRef,
    @Size(max = 400) String diretorioInstalacao,
    @Size(max = 4000) String observacoes,
    @Size(max = 80) String versaoAtual,
    @Valid ConfiguracaoInstalacaoRequest configuracao,
    @Valid List<ReservaPortaRequest> portas) {}
