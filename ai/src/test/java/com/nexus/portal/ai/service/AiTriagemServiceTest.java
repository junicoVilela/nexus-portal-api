package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.service.AiTriagemService.ResultadoTriagem;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiTriagemServiceTest {

  private AiTriagemService service;

  @BeforeEach
  void setUp() {
    service = new AiTriagemService(new AiProperties(true, null, null, null, null, null, 30, 5, 1000, 20));
  }

  @Test
  void briefingRicoFicaProntoSemPerguntasObrigatorias() {
    String briefing = """
        Consulta de pedidos
        codigoTela: PED-CONSULTA
        Público: operador do atendimento.
        Fluxo: filtrar por período, visualizar lista e exportar CSV com os pedidos do cliente.
        Inclui pré-requisitos de permissão PEDIDO:LER e resultado esperado na tela de sucesso.
        """;

    ResultadoTriagem resultado = service.avaliar(AiObjetivo.CRIAR_PAGINA, briefing, Map.of());

    assertThat(resultado.completa()).isTrue();
    assertThat(resultado.contextoExtraido()).containsEntry("codigoTela", "PED-CONSULTA");
    assertThat(resultado.contextoExtraido()).containsKey("titulo");
    assertThat(resultado.perguntas()).noneMatch(p -> p.obrigatoria());
  }

  @Test
  void briefingCurtoPedePerguntas() {
    ResultadoTriagem resultado = service.avaliar(
        AiObjetivo.CRIAR_PAGINA,
        "Nova tela de pedidos para o time operacional usar no dia a dia.",
        Map.of());

    assertThat(resultado.completa()).isFalse();
    assertThat(resultado.perguntas()).extracting(p -> p.id())
        .contains("codigoTela", "fluxo");
  }

  @Test
  void respostasCompletamTriagem() {
    ResultadoTriagem resultado = service.avaliar(
        AiObjetivo.CRIAR_PAGINA,
        "Precisamos documentar a nova tela de pedidos do portal.",
        Map.of(
            "titulo", "Consulta de pedidos",
            "codigoTela", "PED-CONSULTA",
            "fluxo", "Filtrar, listar e exportar pedidos do cliente."));

    assertThat(resultado.completa()).isTrue();
  }

  @Test
  void removeMarcadorMarkdownDoTitulo() {
    ResultadoTriagem resultado = service.avaliar(
        AiObjetivo.CRIAR_PAGINA,
        """
            # Cadastro de usuários

            ## Objetivo
            Documentar o cadastro, a consulta e a edição dos usuários autorizados no sistema.
            Código tela: CAD-USU
            """,
        Map.of());

    assertThat(resultado.contextoExtraido()).containsEntry("titulo", "Cadastro de usuários");
  }
}
