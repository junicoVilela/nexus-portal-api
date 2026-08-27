package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import java.util.UUID;

public record ManifestoImplantacaoResponse(
    UUID releaseId,
    String releaseVersao,
    TipoImplantacao tipoImplantacao,
    String imagemRef,
    String arquivoImagemRef,
    String diretorioInstalacao,
    String observacoes,
    String resumo,
    boolean derivado,
    String fingerprint) {}
