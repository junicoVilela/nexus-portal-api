package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.DeployInstalacao;
import com.nexus.portal.releaseorchestrator.entity.ModoDeploy;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.StatusDeploy;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeployInstalacaoResponse(
    UUID id,
    UUID releaseId,
    String releaseVersao,
    UUID instalacaoId,
    String instalacaoCodigo,
    String instalacaoNome,
    String hostCodigo,
    UUID entregaId,
    TipoImplantacao tipoImplantacao,
    OperacaoDeploy operacao,
    ModoDeploy modo,
    StatusDeploy status,
    String versaoOrigem,
    String versaoDestino,
    String imagemRef,
    String arquivoImagemRef,
    String diretorioInstalacao,
    String fingerprint,
    String mensagem,
    String erro,
    String operador,
    boolean reutilizado,
    OffsetDateTime iniciadoEm,
    OffsetDateTime concluidoEm,
    OffsetDateTime createdAt) {

  public static DeployInstalacaoResponse from(DeployInstalacao d) {
    return new DeployInstalacaoResponse(
        d.getId(),
        d.getRelease().getId(),
        d.getRelease().getVersao(),
        d.getInstalacao().getId(),
        d.getInstalacao().getCodigo(),
        d.getInstalacao().getNome(),
        d.getInstalacao().getHost().getCodigo(),
        d.getEntrega() == null ? null : d.getEntrega().getId(),
        d.getTipoImplantacao(),
        d.getOperacao(),
        d.getModo(),
        d.getStatus(),
        d.getVersaoOrigem(),
        d.getVersaoDestino(),
        d.getImagemRef(),
        d.getArquivoImagemRef(),
        d.getDiretorioInstalacao(),
        d.getFingerprint(),
        d.getMensagem(),
        d.getErro(),
        d.getOperador(),
        d.isReutilizado(),
        d.getIniciadoEm(),
        d.getConcluidoEm(),
        d.getCreatedAt());
  }
}
