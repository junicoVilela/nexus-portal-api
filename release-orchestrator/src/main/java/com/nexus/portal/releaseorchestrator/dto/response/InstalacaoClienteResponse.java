package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.HealthInstalacao;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.StatusInstalacao;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;

public record InstalacaoClienteResponse(
    UUID id,
    String codigo,
    String nome,
    UUID clienteId,
    String clienteNome,
    String clienteSigla,
    UUID hostId,
    String hostCodigo,
    String hostNome,
    UUID produtoId,
    String produtoNome,
    String produtoSigla,
    TipoImplantacao tipoImplantacao,
    StatusInstalacao status,
    AmbientePadrao ambiente,
    String imagemRef,
    String arquivoImagemRef,
    String diretorioInstalacao,
    String observacoes,
    String versaoAtual,
    HealthInstalacao health,
    OffsetDateTime ultimaVerificacao,
    String ultimoErro,
    ConfiguracaoInstalacaoResponse configuracao,
    List<ReservaPortaResponse> portas,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static InstalacaoClienteResponse from(InstalacaoCliente i) {
    return new InstalacaoClienteResponse(
        i.getId(),
        i.getCodigo(),
        i.getNome(),
        i.getCliente().getId(),
        i.getCliente().getNome(),
        i.getCliente().getSigla(),
        i.getHost().getId(),
        i.getHost().getCodigo(),
        i.getHost().getNome(),
        i.getProduto().getId(),
        i.getProduto().getNome(),
        i.getProduto().getSigla(),
        i.getTipoImplantacao(),
        i.getStatus(),
        i.getAmbiente(),
        i.getImagemRef(),
        i.getArquivoImagemRef(),
        i.getDiretorioInstalacao(),
        i.getObservacoes(),
        i.getVersaoAtual(),
        i.getHealth(),
        i.getUltimaVerificacao(),
        i.getUltimoErro(),
        Hibernate.isInitialized(i.getConfiguracao())
            ? ConfiguracaoInstalacaoResponse.from(i.getConfiguracao())
            : null,
        i.getPortas() != null && Hibernate.isInitialized(i.getPortas())
            ? i.getPortas().stream().map(ReservaPortaResponse::from).toList()
            : List.of(),
        i.getCreatedAt(),
        i.getUpdatedAt());
  }
}
