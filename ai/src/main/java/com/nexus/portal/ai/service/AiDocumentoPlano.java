package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoStatus;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoTipo;
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
    Integer tokensSaidaAnalise,
    List<Sugestao> sugestoes) {

  AiDocumentoPlano {
    modulos = modulos == null ? List.of() : List.copyOf(modulos);
    projetoNomesSugeridos = projetoNomesSugeridos == null
        ? List.of()
        : List.copyOf(projetoNomesSugeridos);
    sugestoes = sugestoes == null ? List.of() : List.copyOf(sugestoes);
  }

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
        null,
        List.of());
  }

  record Sugestao(
      UUID id,
      AiDocumentoSugestaoTipo tipo,
      String titulo,
      String justificativa,
      double confianca,
      AiDocumentoSugestaoStatus status,
      UUID paginaOrigemId,
      UUID paginaDestinoId,
      UUID moduloOrigemId,
      UUID moduloDestinoId,
      String valorSugerido,
      String conteudoSugerido) {

    Sugestao {
      status = status == null ? AiDocumentoSugestaoStatus.PENDENTE : status;
      confianca = Math.max(0, Math.min(1, confianca));
    }

    boolean aplicacaoSegura() {
      return tipo == AiDocumentoSugestaoTipo.RENOMEAR_PAGINA
          || tipo == AiDocumentoSugestaoTipo.MOVER_PAGINA
          || tipo == AiDocumentoSugestaoTipo.RENOMEAR_MODULO;
    }

    Sugestao comStatus(AiDocumentoSugestaoStatus novoStatus) {
      return new Sugestao(
          id,
          tipo,
          titulo,
          justificativa,
          confianca,
          novoStatus,
          paginaOrigemId,
          paginaDestinoId,
          moduloOrigemId,
          moduloDestinoId,
          valorSugerido,
          conteudoSugerido);
    }
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
