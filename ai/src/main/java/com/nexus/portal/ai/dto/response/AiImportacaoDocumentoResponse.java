package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiImportacaoStatus;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.entity.AiTipoDocumento;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiImportacaoDocumentoResponse(
    UUID id,
    String nomeArquivo,
    AiTipoDocumento tipoArquivo,
    String mimeType,
    long tamanhoBytes,
    int caracteresExtraidos,
    int totalPaginasOrigem,
    AiImportacaoStatus status,
    long version,
    String projetoNome,
    String projetoDescricao,
    UUID projetoId,
    UUID clienteId,
    boolean estruturaConfirmada,
    List<Modulo> modulos,
    List<String> avisos,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  public static AiImportacaoDocumentoResponse from(
      AiDocumentoImportacao importacao,
      String projetoNome,
      String projetoDescricao,
      UUID projetoId,
      UUID clienteId,
      boolean estruturaConfirmada,
      List<Modulo> modulos,
      List<String> avisos) {
    return new AiImportacaoDocumentoResponse(
        importacao.getId(),
        importacao.getNomeArquivo(),
        importacao.getTipoArquivo(),
        importacao.getMimeType(),
        importacao.getTamanhoBytes(),
        importacao.getTextoExtraido().length(),
        importacao.getTotalPaginasOrigem(),
        importacao.getStatus(),
        importacao.getVersion(),
        projetoNome,
        projetoDescricao,
        projetoId,
        clienteId,
        estruturaConfirmada,
        modulos,
        avisos,
        importacao.getCreatedAt(),
        importacao.getUpdatedAt());
  }

  public record Modulo(UUID id, UUID moduloId, String nome, int ordem, List<Pagina> paginas) {}

  public record Pagina(
      UUID id,
      String titulo,
      int ordem,
      String briefing,
      UUID templateId,
      String templateCodigo,
      String templateNome,
      double confiancaTemplate,
      String motivoTemplate,
      AiPaginaPlanoStatus status) {}
}
