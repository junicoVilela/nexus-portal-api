package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import java.util.List;
import java.util.UUID;

record AiDocumentoPlano(
    String projetoNome,
    String projetoDescricao,
    UUID projetoId,
    UUID clienteId,
    boolean estruturaConfirmada,
    List<Modulo> modulos,
    List<String> projetoNomesSugeridos,
    AiDocumentoAnaliseOrigem analiseOrigem,
    String analiseMensagem,
    Integer tokensEntradaAnalise,
    Integer tokensSaidaAnalise) {

  AiDocumentoPlano(
      String projetoNome,
      String projetoDescricao,
      UUID projetoId,
      UUID clienteId,
      boolean estruturaConfirmada,
      List<Modulo> modulos) {
    this(
        projetoNome,
        projetoDescricao,
        projetoId,
        clienteId,
        estruturaConfirmada,
        modulos,
        projetoNome == null ? List.of() : List.of(projetoNome),
        AiDocumentoAnaliseOrigem.ESTRUTURAL,
        null,
        null,
        null);
  }

  record Modulo(UUID id, UUID moduloId, String nome, int ordem, List<Pagina> paginas) {}

  record Pagina(
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
      String erroMensagem) {

    Pagina(
        UUID id,
        String titulo,
        int ordem,
        String briefing,
        UUID templateId,
        String templateCodigo,
        String templateNome,
        double confiancaTemplate,
        String motivoTemplate,
        AiPaginaPlanoStatus status) {
      this(
          id,
          titulo,
          ordem,
          briefing,
          templateId,
          templateCodigo,
          templateNome,
          confiancaTemplate,
          motivoTemplate,
          status,
          null,
          null,
          null);
    }

    Pagina comStatus(AiPaginaPlanoStatus novoStatus) {
      return new Pagina(
          id,
          titulo,
          ordem,
          briefing,
          templateId,
          templateCodigo,
          templateNome,
          confiancaTemplate,
          motivoTemplate,
          novoStatus,
          paginaId,
          sessaoId,
          erroMensagem);
    }

    Pagina comVinculo(UUID novoPaginaId, AiPaginaPlanoStatus novoStatus) {
      return new Pagina(
          id,
          titulo,
          ordem,
          briefing,
          templateId,
          templateCodigo,
          templateNome,
          confiancaTemplate,
          motivoTemplate,
          novoStatus,
          novoPaginaId,
          sessaoId,
          null);
    }

    Pagina comSessao(UUID novaSessaoId, AiPaginaPlanoStatus novoStatus, String novoErro) {
      return new Pagina(
          id,
          titulo,
          ordem,
          briefing,
          templateId,
          templateCodigo,
          templateNome,
          confiancaTemplate,
          motivoTemplate,
          novoStatus,
          paginaId,
          novaSessaoId,
          novoErro);
    }
  }
}
