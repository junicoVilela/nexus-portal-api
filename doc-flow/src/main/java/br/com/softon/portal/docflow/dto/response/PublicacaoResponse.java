package br.com.softon.portal.docflow.dto.response;

import br.com.softon.portal.docflow.entity.Publicacao;
import br.com.softon.portal.docflow.entity.StatusPublicacao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicacaoResponse(
    UUID id,
    UUID clienteId,
    String clienteNome,
    String versao,
    StatusPublicacao status,
    int quantidadePaginas,
    int quantidadeModulos,
    String arquivoZipNome,
    String hashPacote,
    String observacao,
    String relatorioValidacao,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy,
    String updatedBy) {
  public static PublicacaoResponse from(Publicacao publicacao) {
    return new PublicacaoResponse(
        publicacao.getId(),
        publicacao.getCliente().getId(),
        publicacao.getCliente().getNome(),
        publicacao.getVersao(),
        publicacao.getStatus(),
        publicacao.getQuantidadePaginas(),
        publicacao.getQuantidadeModulos(),
        publicacao.getArquivoZipNome(),
        publicacao.getHashPacote(),
        publicacao.getObservacao(),
        publicacao.getRelatorioValidacao(),
        publicacao.getCreatedAt(),
        publicacao.getUpdatedAt(),
        publicacao.getCreatedBy(),
        publicacao.getUpdatedBy());
  }
}
