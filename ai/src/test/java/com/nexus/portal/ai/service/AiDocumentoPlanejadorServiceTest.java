package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.portal.ai.dto.response.AiTemplateCandidatoResponse;
import com.nexus.portal.ai.dto.response.AiTemplateRecomendacaoResponse;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.entity.AiTipoDocumento;
import com.nexus.portal.ai.service.AiDocumentoExtratorService.DocumentoExtraido;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiDocumentoPlanejadorServiceTest {

  private AiDocumentoPlanejadorService service;

  @BeforeEach
  void setUp() {
    var templateService = mock(AiTemplateRecomendacaoService.class);
    var candidato = new AiTemplateCandidatoResponse(
        UUID.randomUUID(),
        "FUNCIONALIDADE",
        "Funcionalidade",
        "Fluxo funcional",
        0.87,
        "Operações e regras identificadas.");
    when(templateService.recomendar(any()))
        .thenReturn(new AiTemplateRecomendacaoResponse(candidato, List.of(candidato), false));
    service = new AiDocumentoPlanejadorService(templateService);
  }

  @Test
  void planejar_markdownDistribuiCadaSecaoNoBriefingCorreto() {
    String texto = """
        # Cadastro de produto

        ## História do usuário
        Como usuário, desejo manter produtos atualizados para uso nos pedidos.

        ## Listagem de registros
        O sistema apresenta código, nome, situação, filtros e paginação.

        ## Inclusão de um novo registro
        Clique em Novo, preencha os campos obrigatórios e clique em Salvar.
        """;
    var documento = new DocumentoExtraido("cadastro-produto.txt", AiTipoDocumento.TXT, texto, 1, List.of());

    AiDocumentoPlano plano = service.planejar(documento, null, null);

    assertThat(plano.projetoNome()).isEqualTo("Cadastro de produto");
    assertThat(plano.modulos()).hasSize(1);
    assertThat(plano.modulos().getFirst().paginas()).extracting(AiDocumentoPlano.Pagina::titulo)
        .containsExactly("História do usuário", "Listagem de registros", "Inclusão de um novo registro");
    assertThat(plano.modulos().getFirst().paginas().get(1).briefing())
        .contains("### Página: Listagem de registros", "código, nome, situação")
        .doesNotContain("preencha os campos obrigatórios");
  }

  @Test
  void planejar_recomendaTemplateParaCadaPaginaEMantemOrdem() {
    String texto = """
        # Manual financeiro
        ## Consulta
        Consulte lançamentos usando período, situação e responsável pelo registro.
        ## Exportação
        Exporte os resultados filtrados nos formatos CSV, Excel ou PDF para conferência.
        """;
    var documento = new DocumentoExtraido("financeiro.txt", AiTipoDocumento.TXT, texto, 1, List.of());

    AiDocumentoPlano plano = service.planejar(documento, null, null);

    var paginas = plano.modulos().getFirst().paginas();
    assertThat(paginas).extracting(AiDocumentoPlano.Pagina::ordem).containsExactly(1, 2);
    assertThat(paginas).allSatisfy(pagina -> {
      assertThat(pagina.templateCodigo()).isEqualTo("FUNCIONALIDADE");
      assertThat(pagina.templateId()).isNotNull();
      assertThat(pagina.status()).isEqualTo(AiPaginaPlanoStatus.PENDENTE);
      assertThat(pagina.briefing()).contains("# Projeto: Manual financeiro");
    });
  }

  @Test
  void planejar_baixaConfiancaSugereModeloMasNaoOForca() {
    var templateService = mock(AiTemplateRecomendacaoService.class);
    var candidato = new AiTemplateCandidatoResponse(
        UUID.randomUUID(),
        "FUNCIONALIDADE",
        "Funcionalidade",
        "Fluxo genérico",
        0.55,
        "Texto ainda ambíguo.");
    when(templateService.recomendar(any()))
        .thenReturn(new AiTemplateRecomendacaoResponse(candidato, List.of(candidato), true));
    service = new AiDocumentoPlanejadorService(templateService);
    var documento = new DocumentoExtraido(
        "manual.txt",
        AiTipoDocumento.TXT,
        "# Manual\n## Orientações\nConteúdo genérico suficiente para uma página de orientação do sistema.",
        1,
        List.of());

    AiDocumentoPlano plano = service.planejar(documento, null, null);

    AiDocumentoPlano.Pagina pagina = plano.modulos().getFirst().paginas().getFirst();
    assertThat(pagina.templateCodigo()).isEqualTo("FUNCIONALIDADE");
    assertThat(pagina.templateId()).isNull();
    assertThat(pagina.confiancaTemplate()).isEqualTo(0.55);
  }

  @Test
  void planejar_pdfSemMarkdownInfereTitulosNumerados() {
    String texto = """
        1 Cadastro
        Esta seção explica como cadastrar um registro com validações e mensagens de sucesso.
        2 Consulta
        Esta seção explica filtros, paginação, ordenação e visualização dos resultados.
        """;
    var documento = new DocumentoExtraido("manual.pdf", AiTipoDocumento.PDF, texto, 2, List.of());

    AiDocumentoPlano plano = service.planejar(documento, null, null);

    assertThat(plano.modulos()).hasSize(1);
    assertThat(plano.modulos().getFirst().paginas()).extracting(AiDocumentoPlano.Pagina::titulo)
        .containsExactly("Cadastro", "Consulta");
  }

  @Test
  void planejar_hierarquiaCompletaInterpretaH1ComoProjetoH2ComoModuloEH3ComoPagina() {
    String texto = """
        # Portal corporativo
        ## Cadastros
        ### Clientes
        Permite incluir e editar clientes com validação dos campos obrigatórios.
        ### Produtos
        Permite incluir e consultar produtos ativos para utilização em pedidos.
        ## Relatórios
        ### Vendas
        Apresenta indicadores, filtros por período e exportação dos resultados.
        """;
    var documento = new DocumentoExtraido("portal.txt", AiTipoDocumento.TXT, texto, 1, List.of());

    AiDocumentoPlano plano = service.planejar(documento, null, null);

    assertThat(plano.projetoNome()).isEqualTo("Portal corporativo");
    assertThat(plano.modulos()).extracting(AiDocumentoPlano.Modulo::nome)
        .containsExactly("Cadastros", "Relatórios");
    assertThat(plano.modulos().getFirst().paginas()).extracting(AiDocumentoPlano.Pagina::titulo)
        .containsExactly("Clientes", "Produtos");
    assertThat(plano.modulos().get(1).paginas()).extracting(AiDocumentoPlano.Pagina::titulo)
        .containsExactly("Vendas");
  }
}
