package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import java.util.List;
import java.util.UUID;

record AiDocumentoPlano(
    String projetoNome,
    String projetoDescricao,
    UUID projetoId,
    UUID clienteId,
    boolean estruturaConfirmada,
    List<Modulo> modulos) {

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
      AiPaginaPlanoStatus status) {

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
          novoStatus);
    }
  }
}
