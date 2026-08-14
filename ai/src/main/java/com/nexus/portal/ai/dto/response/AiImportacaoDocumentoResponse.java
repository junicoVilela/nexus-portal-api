package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoStatus;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoTipo;
import com.nexus.portal.ai.entity.AiImportacaoStatus;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.entity.AiPaginaPlanoOrigem;
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
    List<String> projetoNomesSugeridos,
    AiDocumentoAnaliseOrigem analiseOrigem,
    String analiseMensagem,
    Integer tokensEntradaAnalise,
    Integer tokensSaidaAnalise,
    List<Sugestao> sugestoes,
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
      List<String> projetoNomesSugeridos,
      AiDocumentoAnaliseOrigem analiseOrigem,
      String analiseMensagem,
      Integer tokensEntradaAnalise,
      Integer tokensSaidaAnalise,
      List<Sugestao> sugestoes,
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
        projetoNomesSugeridos,
        analiseOrigem,
        analiseMensagem,
        tokensEntradaAnalise,
        tokensSaidaAnalise,
        sugestoes,
        modulos,
        avisos,
        importacao.getCreatedAt(),
        importacao.getUpdatedAt());
  }

  public record Modulo(UUID id, UUID moduloId, String nome, int ordem, List<Pagina> paginas) {}

  public record Sugestao(
      UUID id,
      AiDocumentoSugestaoTipo tipo,
      String titulo,
      String justificativa,
      double confianca,
      AiDocumentoSugestaoStatus status,
      boolean aplicacaoSegura,
      UUID paginaOrigemId,
      UUID paginaDestinoId,
      UUID moduloOrigemId,
      UUID moduloDestinoId,
      String valorSugerido,
      String conteudoSugerido) {}

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
      AiPaginaPlanoStatus status,
      UUID paginaId,
      UUID sessaoId,
      String erroMensagem,
      AiPaginaPlanoOrigem origem,
      boolean ajustadaManualmente) {}
}
