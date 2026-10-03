package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.portal.ai.entity.AiDocumentoSugestaoTipo;
import com.nexus.portal.ai.entity.AiPaginaPlanoOrigem;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiDocumentoPlanoOperacoesTest {

  private final UUID cadastroId = UUID.randomUUID();
  private final UUID consultaId = UUID.randomUUID();
  private final UUID incluirId = UUID.randomUUID();
  private final UUID editarId = UUID.randomUUID();
  private final UUID listarId = UUID.randomUUID();

  private final AiDocumentoPlano plano = new AiDocumentoPlano(
      "Portal", null, null, null, false,
      List.of(
          new AiDocumentoPlano.Modulo(cadastroId, null, "Cadastro", 1, List.of(
              pagina(incluirId, "Incluir", 1, "Cadastro"),
              pagina(editarId, "Editar", 2, "Cadastro"))),
          new AiDocumentoPlano.Modulo(consultaId, null, "Consulta", 2, List.of(
              pagina(listarId, "Listar", 1, "Consulta")))));

  @Test
  void moverPaginaAtualizaCabecalhoEReordena() {
    var modulos = AiDocumentoPlanoOperacoes.aplicarSugestao(
        plano, sugestao(AiDocumentoSugestaoTipo.MOVER_PAGINA, incluirId, null, null, consultaId, null));

    assertThat(modulos.get(0).paginas()).extracting(AiDocumentoPlano.Pagina::titulo).containsExactly("Editar");
    assertThat(modulos.get(0).paginas().get(0).ordem()).isEqualTo(1);
    var movida = modulos.get(1).paginas().get(1);
    assertThat(movida.titulo()).isEqualTo("Incluir");
    assertThat(movida.ordem()).isEqualTo(2);
    assertThat(movida.briefing()).contains("## Módulo: Consulta");
  }

  @Test
  void mesclarRemoveOrigemEConsolidaConteudoNoDestino() {
    var modulos = AiDocumentoPlanoOperacoes.aplicarSugestao(
        plano, sugestao(AiDocumentoSugestaoTipo.MESCLAR_PAGINAS, editarId, incluirId, null, null, "Manter cadastro"));

    assertThat(modulos.get(0).paginas()).extracting(AiDocumentoPlano.Pagina::titulo)
        .containsExactly("Manter cadastro");
    assertThat(modulos.get(0).paginas().get(0).briefing())
        .contains("### Página: Manter cadastro")
        .contains("Conteúdo de Incluir")
        .contains("## Conteúdo consolidado de Editar")
        .contains("Conteúdo de Editar");
  }

  @Test
  void renomearModuloRecusaNomeRepetido() {
    assertThatThrownBy(() -> AiDocumentoPlanoOperacoes.aplicarSugestao(
        plano, sugestao(AiDocumentoSugestaoTipo.RENOMEAR_MODULO, null, null, cadastroId, null, "consulta")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Já existe outro módulo");
  }

  @Test
  void adicionarPaginaEntraNoFimDoModuloComOrigemIa() {
    var modulos = AiDocumentoPlanoOperacoes.aplicarSugestao(
        plano, sugestao(AiDocumentoSugestaoTipo.ADICIONAR_PAGINA, null, null, null, consultaId, "Exportar"));

    var nova = modulos.get(1).paginas().get(1);
    assertThat(nova.titulo()).isEqualTo("Exportar");
    assertThat(nova.ordem()).isEqualTo(2);
    assertThat(nova.origem()).isEqualTo(AiPaginaPlanoOrigem.IA);
    assertThat(nova.briefing()).startsWith("# Projeto: Portal\n\n## Módulo: Consulta\n\n### Página: Exportar");
  }

  @Test
  void operacoesNaoAlteramOPlanoOriginal() {
    AiDocumentoPlanoOperacoes.aplicarSugestao(
        plano, sugestao(AiDocumentoSugestaoTipo.MOVER_PAGINA, incluirId, null, null, consultaId, null));
    assertThat(plano.modulos().get(0).paginas()).hasSize(2);
  }

  private static AiDocumentoPlano.Pagina pagina(UUID id, String titulo, int ordem, String modulo) {
    return new AiDocumentoPlano.Pagina(
        id, titulo, ordem,
        AiDocumentoPlanoOperacoes.montarBriefing("Portal", modulo, titulo, "Conteúdo de " + titulo),
        null, null, null, 0, null, AiPaginaPlanoStatus.PENDENTE, null, null, null,
        AiPaginaPlanoOrigem.DOCUMENTO, false, null, null, List.of(), List.of(), false);
  }

  private static AiDocumentoPlano.Sugestao sugestao(
      AiDocumentoSugestaoTipo tipo,
      UUID paginaOrigem,
      UUID paginaDestino,
      UUID moduloOrigem,
      UUID moduloDestino,
      String valor) {
    return new AiDocumentoPlano.Sugestao(
        UUID.randomUUID(), tipo, "Sugestão", "Justificativa", 0.9, null,
        paginaOrigem, paginaDestino, moduloOrigem, moduloDestino, valor, null);
  }
}
