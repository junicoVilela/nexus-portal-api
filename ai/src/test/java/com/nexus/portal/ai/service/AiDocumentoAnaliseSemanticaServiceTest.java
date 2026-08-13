package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.provider.LlmCompletion;
import com.nexus.portal.ai.provider.LlmProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiDocumentoAnaliseSemanticaServiceTest {

  @Test
  void analisar_reorganizaManifestoSemReescreverConteudo() {
    UUID paginaConsultarId = UUID.randomUUID();
    UUID paginaCadastrarId = UUID.randomUUID();
    AiDocumentoPlano base = plano(paginaConsultarId, paginaCadastrarId);
    String json = """
        {
          "projetoNomes":["Portal de Identidades","Gestão de Acessos"],
          "projetoDescricao":"Manual operacional de identidades e acessos.",
          "modulos":[
            {"nome":"Primeiros passos","paginas":[
              {"paginaId":"%s","titulo":"Consultar usuários"}
            ]},
            {"nome":"Administração","paginas":[
              {"paginaId":"%s","titulo":"Cadastrar usuário"}
            ]}
          ]
        }
        """.formatted(paginaConsultarId, paginaCadastrarId);
    var service = new AiDocumentoAnaliseSemanticaService(provider(json), new ObjectMapper());

    AiDocumentoPlano resultado = service.analisar(base, "manual.docx");

    assertThat(resultado.analiseOrigem()).isEqualTo(AiDocumentoAnaliseOrigem.LLM);
    assertThat(resultado.projetoNomesSugeridos())
        .containsExactly("Portal de Identidades", "Gestão de Acessos");
    assertThat(resultado.modulos()).extracting(AiDocumentoPlano.Modulo::nome)
        .containsExactly("Primeiros passos", "Administração");
    assertThat(resultado.modulos().getFirst().paginas().getFirst().briefing())
        .contains("# Projeto: Portal de Identidades")
        .contains("## Módulo: Primeiros passos")
        .contains("Texto original exclusivo da consulta.");
    assertThat(resultado.modulos().get(1).paginas().getFirst().briefing())
        .contains("Texto original exclusivo do cadastro.");
  }

  @Test
  void analisar_recusaQuandoLlmOmitePagina() {
    UUID paginaConsultarId = UUID.randomUUID();
    UUID paginaCadastrarId = UUID.randomUUID();
    String json = """
        {
          "projetoNomes":["Portal"],
          "projetoDescricao":"Manual.",
          "modulos":[{"nome":"Consulta","paginas":[
            {"paginaId":"%s","titulo":"Consultar usuários"}
          ]}]
        }
        """.formatted(paginaConsultarId);
    var service = new AiDocumentoAnaliseSemanticaService(provider(json), new ObjectMapper());

    assertThatThrownBy(() -> service.analisar(plano(paginaConsultarId, paginaCadastrarId), "manual.docx"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("todas as páginas");
  }

  private AiDocumentoPlano plano(UUID paginaConsultarId, UUID paginaCadastrarId) {
    return new AiDocumentoPlano(
        "Manual original",
        "Descrição original.",
        null,
        null,
        false,
        List.of(new AiDocumentoPlano.Modulo(
            UUID.randomUUID(),
            null,
            "Conteúdo importado",
            1,
            List.of(
                pagina(
                    paginaConsultarId,
                    1,
                    "Consultar",
                    "Texto original exclusivo da consulta."),
                pagina(
                    paginaCadastrarId,
                    2,
                    "Cadastrar",
                    "Texto original exclusivo do cadastro.")))));
  }

  private AiDocumentoPlano.Pagina pagina(UUID id, int ordem, String titulo, String conteudo) {
    return new AiDocumentoPlano.Pagina(
        id,
        titulo,
        ordem,
        "# Projeto: Manual original\n\n## Módulo: Conteúdo importado\n\n### Página: "
            + titulo
            + "\n\n"
            + conteudo,
        null,
        "FUNCIONALIDADE",
        "Funcionalidade",
        0.8,
        "Fluxo funcional.",
        AiPaginaPlanoStatus.PENDENTE);
  }

  private LlmProvider provider(String content) {
    return new LlmProvider() {
      @Override
      public String id() {
        return "teste";
      }

      @Override
      public LlmCompletion completar(String systemPrompt, String userPrompt) {
        return LlmCompletion.of(content, 120, 80);
      }
    };
  }
}
