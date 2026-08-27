package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.docflow.dto.request.PaginaSnippetRequest;
import com.nexus.portal.docflow.entity.PaginaSnippet;
import com.nexus.portal.docflow.repository.PaginaSnippetRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaginaSnippetServiceTest {

  @Mock PaginaSnippetRepository repository;
  @Mock AuditoriaService auditoriaService;

  PaginaSnippetService service;

  @BeforeEach
  void setUp() {
    service = new PaginaSnippetService(repository, auditoriaService);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void criar_normalizaOCodigoParaMaiusculas() {
    PaginaSnippet snippet = service.criar(
        new PaginaSnippetRequest("aviso-lgpd", "Aviso LGPD", null, "<p>Dados pessoais</p>", true), null);

    assertThat(snippet.getCodigo()).isEqualTo("AVISO-LGPD");
  }

  @Test
  void criar_recusaCodigoDuplicado() {
    when(repository.existsByCodigo("AVISO")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(
        new PaginaSnippetRequest("aviso", "Aviso", null, "<p>x</p>", true), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Já existe um trecho");
  }

  @Test
  void criar_sanitizaScriptDoConteudo() {
    PaginaSnippet snippet = service.criar(new PaginaSnippetRequest("AVISO", "Aviso", null,
        "<p>ok</p><script>alert('x')</script>", true), null);

    assertThat(snippet.getConteudoHtml()).contains("<p>ok</p>").doesNotContain("<script>");
  }

  @Test
  void atualizar_naoDeixaTrocarOCodigo() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id))
        .thenReturn(Optional.of(new PaginaSnippet("AVISO", "Aviso", null, "<p>x</p>", true)));

    assertThatThrownBy(() -> service.atualizar(id,
        new PaginaSnippetRequest("OUTRO", "Aviso", null, "<p>x</p>", true), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não pode ser alterado");
  }

  @Test
  void resolver_substituiAReferenciaPeloConteudo() {
    when(repository.findByAtivoTrueOrderByCodigoAsc()).thenReturn(
        List.of(new PaginaSnippet("AVISO", "Aviso", null, "<p>Confira os dados.</p>", true)));

    String html = service.resolver("<h2>Passo</h2>{{snippet:aviso}}<p>fim</p>");

    assertThat(html)
        .isEqualTo("<h2>Passo</h2><p>Confira os dados.</p><p>fim</p>");
  }

  @Test
  void resolver_referenciaDesconhecidaViraAvisoVisivel() {
    when(repository.findByAtivoTrueOrderByCodigoAsc()).thenReturn(List.of());

    String html = service.resolver("<p>a</p>{{snippet:SUMIU}}");

    assertThat(html).contains("snippet-ausente").contains("SUMIU");
  }

  @Test
  void resolver_snippetInativoNaoEntraNoPacote() {
    when(repository.findByAtivoTrueOrderByCodigoAsc()).thenReturn(List.of());

    assertThat(service.resolver("{{snippet:DESATIVADO}}")).contains("indisponível");
  }

  @Test
  void resolver_htmlSemReferencia_naoConsultaOBanco() {
    String html = "<p>sem trecho reutilizável</p>";

    assertThat(service.resolver(html)).isEqualTo(html);
    org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
        .findByAtivoTrueOrderByCodigoAsc();
  }

  @Test
  void resolver_conteudoDoSnippetComCifrao_naoQuebraASubstituicao() {
    when(repository.findByAtivoTrueOrderByCodigoAsc()).thenReturn(
        List.of(new PaginaSnippet("PRECO", "Preço", null, "<p>Valor: R$ 1,00</p>", true)));

    assertThat(service.resolver("{{snippet:PRECO}}")).contains("R$ 1,00");
  }

  @Test
  void referencias_listaOsCodigosCitados() {
    assertThat(service.referencias("{{snippet:a}} texto {{snippet:B}} {{snippet:a}}"))
        .containsExactly("A", "B");
  }
}
