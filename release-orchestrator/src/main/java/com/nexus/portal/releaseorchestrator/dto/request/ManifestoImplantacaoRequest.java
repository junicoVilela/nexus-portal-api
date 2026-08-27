package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManifestoImplantacaoRequest(
    @NotNull TipoImplantacao tipoImplantacao,
    @Size(max = 300) String imagemRef,
    @Size(max = 400) String arquivoImagemRef,
    @Size(max = 400) String diretorioInstalacao,
    @Size(max = 4000) String observacoes) {}
