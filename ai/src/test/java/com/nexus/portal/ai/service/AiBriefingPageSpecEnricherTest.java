package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoSlotResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiBriefingPageSpecEnricherTest {

  private final AiBriefingPageSpecEnricher enricher = new AiBriefingPageSpecEnricher();

  @Test
  void distribuiSecoesMarkdownNosComponentesSemPerderConteudoInformado() {
    List<String> ids = List.of(
        "introducao",
        "objetivo",
        "pre-requisitos",
        "filtros-resultado",
        "acoes-tela",
        "passo-a-passo",
        "regras",
        "mensagens-sistema",
        "resultado-esperado",
        "boas-praticas",
        "ver-tambem");
    List<PaginaBlocoResponse> catalogo = ids.stream().map(AiBriefingPageSpecEnricherTest::componente).toList();
    AiPageSpec original = new AiPageSpec(
        2,
        "formulario-operacional",
        "Cadastro de usuários autorizados",
        "cadastro-de-usuarios",
        "CAD-USU",
        "Cadastro completo.",
        ids.stream().map(id -> new AiPageSpec.Bloco(id, List.of())).toList());

    AiPageSpec enriquecida = enricher.enriquecer(original, catalogo, BRIEFING);
    String textos = enriquecida.blocos().stream()
        .flatMap(bloco -> bloco.textos().stream())
        .map(AiPageSpec.Texto::valor)
        .reduce("", (a, b) -> a + " " + b);

    assertThat(enriquecida.titulo()).isEqualTo("Cadastro de usuários autorizados");
    assertThat(enriquecida.resumo()).contains("gerenciamento completo");
    assertThat(enriquecida.blocos()).extracting(AiPageSpec.Bloco::componenteId)
        .doesNotContain("ver-tambem");
    assertThat(enriquecida.blocos().getFirst().textos()).extracting(AiPageSpec.Texto::valor)
        .contains("Cadastro de usuários autorizados");
    assertThat(textos)
        .contains("gerenciamento completo")
        .contains("Menu principal → Cadastros")
        .contains("Código ou identificador")
        .contains("Validação do formato")
        .contains("localize-o na listagem")
        .contains("dependências vinculadas")
        .contains("Cadastro realizado com sucesso")
        .contains("Antes de cadastrar um novo registro");
  }

  private static PaginaBlocoResponse componente(String id) {
    List<PaginaBlocoSlotResponse> slots = new ArrayList<>();
    for (int indice = 1; indice <= 13; indice++) {
      slots.add(new PaginaBlocoSlotResponse(
          "t" + indice,
          indice == 1 ? "h2" : "p",
          "",
          "Texto padrão " + indice));
    }
    return new PaginaBlocoResponse(
        id, id, "Componente " + id, "Estrutura", "intro", "<section/>", null, 1, slots);
  }

  private static final String BRIEFING = """
      # Cadastro de usuários

      ## História do usuário
      Como usuário autorizado, desejo cadastrar, consultar, visualizar, editar e excluir registros.

      ## Objetivo
      A funcionalidade permite realizar o gerenciamento completo dos registros.

      ## Acesso à funcionalidade
      Menu principal → Cadastros → Usuários.

      ## Listagem de registros
      A listagem poderá apresentar Código ou identificador, nome, situação e ações disponíveis.
      * Código ou identificador do registro
      * Nome ou descrição

      ## Inclusão de um novo registro
      Durante o preenchimento poderão ocorrer validações.
      * Validação do formato de e-mail, telefone, CPF, CNPJ ou datas

      ## Visualização dos detalhes
      Para consultar todas as informações, localize-o na listagem e clique em Visualizar.

      ## Edição de um registro
      Localize o registro, clique em Editar, altere os dados e salve.

      ## Exclusão de um registro
      A exclusão poderá ser bloqueada quando existirem dependências vinculadas.

      ## Ativação e inativação
      Registros inativos não poderão ser utilizados em novos processos.

      ## Mensagens do sistema
      * Cadastro realizado com sucesso.
      * Preencha os campos obrigatórios.

      ## Recomendações de uso
      Antes de cadastrar um novo registro, utilize a pesquisa para verificar se ele já existe.
      Confira os dados antes de salvar ou excluir.
      """;
}
