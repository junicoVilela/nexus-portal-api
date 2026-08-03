package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.dto.request.PaginaTemplateRequest;
import com.nexus.portal.docflow.dto.request.PaginaTemplateAplicacaoRequest;
import com.nexus.portal.docflow.dto.request.PaginaTemplateDuplicarRequest;
import com.nexus.portal.docflow.dto.response.PaginaTemplateAplicacaoResponse;
import com.nexus.portal.docflow.entity.PaginaTemplate;
import com.nexus.portal.docflow.entity.PaginaTemplateVersao;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.repository.ClienteProjetoRepository;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ModuloRepository;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.docflow.repository.PaginaTemplateRepository;
import com.nexus.portal.docflow.repository.PaginaTemplateVersaoRepository;
import com.nexus.portal.docflow.repository.ProjetoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.identityaccess.service.AuditoriaService;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaginaTemplateServiceTest {

  @Mock
  PaginaTemplateRepository paginaTemplateRepository;

  @Mock
  PaginaTemplateVersaoRepository paginaTemplateVersaoRepository;

  @Mock
  ProjetoRepository projetoRepository;

  @Mock
  ClienteRepository clienteRepository;

  @Mock
  ModuloRepository moduloRepository;

  @Mock
  ClienteProjetoRepository clienteProjetoRepository;

  @Mock
  PaginaRepository paginaRepository;

  @Mock
  AuditoriaService auditoriaService;

  @InjectMocks
  PaginaTemplateService paginaTemplateService;

  @Test
  void listarAtivos_deveRetornarTemplatesNaOrdemDoRepositorio() {
    PaginaTemplate funcionalidade = new PaginaTemplate(
        "FUNCIONALIDADE", "Funcionalidade", "Descrição", "<h2>Objetivo</h2>", 10, true);
    PaginaTemplate faq = new PaginaTemplate(
        "FAQ", "Perguntas frequentes", "Descrição", "<h2>FAQ</h2>", 20, true);
    when(paginaTemplateRepository.findByAtivoTrueOrderByOrdemAscNomeAsc())
        .thenReturn(List.of(funcionalidade, faq));

    List<PaginaTemplate> resultado = paginaTemplateService.listarAtivos();

    assertThat(resultado).containsExactly(funcionalidade, faq);
    verify(paginaTemplateRepository).findByAtivoTrueOrderByOrdemAscNomeAsc();
  }

  @Test
  void criar_deveSalvarTemplatePersonalizadoDoProjetoESanitizarHtml() {
    UUID projetoId = UUID.randomUUID();
    Projeto projeto = new Projeto("Portal", "portal", null, true);
    PaginaTemplateRequest request = new PaginaTemplateRequest(
        " Cadastro padrão ",
        " Estrutura da equipe ",
        "<section class=\"doc-section\"><h2>Cadastro</h2><script>alert(1)</script></section>",
        projetoId,
        null);
    when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
    when(paginaTemplateRepository.save(any(PaginaTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PaginaTemplate resultado = paginaTemplateService.criar(request, () -> "editor");

    assertThat(resultado.isPersonalizado()).isTrue();
    assertThat(resultado.getProjeto()).isSameAs(projeto);
    assertThat(resultado.getCliente()).isNull();
    assertThat(resultado.getNome()).isEqualTo("Cadastro padrão");
    assertThat(resultado.getDescricao()).isEqualTo("Estrutura da equipe");
    assertThat(resultado.getConteudoHtml()).contains("doc-section").doesNotContain("script", "alert");
    assertThat(resultado.getCodigo()).startsWith("CUSTOM_");
    assertThat(resultado.getVersaoAtual()).isEqualTo(1);
    verify(paginaTemplateVersaoRepository).save(any(PaginaTemplateVersao.class));
    verify(auditoriaService).registrar(eq("PAGINA_TEMPLATE"), isNull(), eq("CRIAR"),
        eq("Modelo personalizado criado: Cadastro padrão"), any(Principal.class));
  }

  @Test
  void excluir_deveRecusarTemplateDoSistema() {
    UUID id = UUID.randomUUID();
    PaginaTemplate sistema = new PaginaTemplate(
        "FAQ", "Perguntas frequentes", null, "<h2>FAQ</h2>", 10, true);
    when(paginaTemplateRepository.findById(id)).thenReturn(Optional.of(sistema));

    assertThatThrownBy(() -> paginaTemplateService.excluir(id, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("sistema");

    verify(paginaTemplateRepository, never()).delete(any());
  }

  @Test
  void excluir_deveRemoverTemplatePersonalizado() {
    UUID id = UUID.randomUUID();
    Projeto projeto = new Projeto("Portal", "portal", null, true);
    PaginaTemplate personalizado = new PaginaTemplate(
        "CUSTOM_1", "Cadastro padrão", null, "<h2>Cadastro</h2>", 1000, projeto, null);
    when(paginaTemplateRepository.findById(id)).thenReturn(Optional.of(personalizado));

    paginaTemplateService.excluir(id, null);

    verify(paginaTemplateRepository).delete(personalizado);
    verify(auditoriaService).registrar("PAGINA_TEMPLATE", id, "EXCLUIR",
        "Modelo personalizado excluído: Cadastro padrão", null);
  }

  @Test
  void aplicar_deveResolverVariaveisConhecidasEManterPendentes() {
    UUID id = UUID.randomUUID();
    PaginaTemplate sistema = new PaginaTemplate(
        "GUIA", "Guia", null,
        "<h2>{{ pagina.titulo }}</h2><p>{{ pagina.codigo }} · {{ modulo.nome }}</p>", 10, true);
    when(paginaTemplateRepository.findById(id)).thenReturn(Optional.of(sistema));

    PaginaTemplateAplicacaoResponse resultado = paginaTemplateService.aplicar(id,
        new PaginaTemplateAplicacaoRequest(null, null, null, "Cadastro de clientes", "CLI-001"));

    assertThat(resultado.conteudoHtml()).contains("Cadastro de clientes", "CLI-001", "{{ modulo.nome }}");
    assertThat(resultado.variaveisPendentes()).containsExactly("modulo.nome");
    assertThat(resultado.variaveisResolvidas()).containsEntry("pagina.titulo", "Cadastro de clientes");
  }

  @Test
  void aplicar_deveResolverAliasesGenericosDeKits() {
    UUID id = UUID.randomUUID();
    PaginaTemplate sistema = new PaginaTemplate(
        "KIT", "Kit", null,
        "<h2>{{ TITULO }}</h2><p>{{ CODIGO_TELA }} · {{ MODULO }}</p>", 10, true);
    when(paginaTemplateRepository.findById(id)).thenReturn(Optional.of(sistema));

    PaginaTemplateAplicacaoResponse resultado = paginaTemplateService.aplicar(id,
        new PaginaTemplateAplicacaoRequest(null, null, null, "Lista de registros", "LISTA-001"));

    assertThat(resultado.conteudoHtml()).contains("Lista de registros", "LISTA-001", "{{ MODULO }}");
    assertThat(resultado.variaveisResolvidas())
        .containsEntry("TITULO", "Lista de registros")
        .containsEntry("CODIGO_TELA", "LISTA-001");
  }

  @Test
  void atualizar_deveCriarNovaVersaoSemAlterarPaginasExistentes() {
    UUID id = UUID.randomUUID();
    UUID projetoId = UUID.randomUUID();
    Projeto projeto = new Projeto("Portal", "portal", null, true);
    PaginaTemplate template = new PaginaTemplate(
        "CUSTOM_1", "Cadastro", null, "<h2>Cadastro</h2>", 1000, projeto, null);
    when(paginaTemplateRepository.findById(id)).thenReturn(Optional.of(template));
    when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));

    PaginaTemplate atualizado = paginaTemplateService.atualizar(id,
        new PaginaTemplateRequest("Cadastro revisado", "Nova descrição", "<h2>Cadastro v2</h2>", projetoId, null),
        () -> "editor");

    assertThat(atualizado.getVersaoAtual()).isEqualTo(2);
    assertThat(atualizado.getNome()).isEqualTo("Cadastro revisado");
    verify(paginaTemplateVersaoRepository).save(any(PaginaTemplateVersao.class));
    verify(paginaRepository, never()).save(any());
  }

  @Test
  void duplicar_deveCriarModeloPersonalizadoNaVersaoInicial() {
    UUID id = UUID.randomUUID();
    UUID projetoId = UUID.randomUUID();
    Projeto projeto = new Projeto("Portal", "portal", null, true);
    PaginaTemplate origem = new PaginaTemplate(
        "FAQ", "FAQ", "Perguntas", "<h2>FAQ</h2>", 10, true);
    when(paginaTemplateRepository.findById(id)).thenReturn(Optional.of(origem));
    when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
    when(paginaTemplateRepository.save(any(PaginaTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PaginaTemplate copia = paginaTemplateService.duplicar(id,
        new PaginaTemplateDuplicarRequest("FAQ financeiro", projetoId, null), null);

    assertThat(copia.isPersonalizado()).isTrue();
    assertThat(copia.getNome()).isEqualTo("FAQ financeiro");
    assertThat(copia.getConteudoHtml()).isEqualTo(origem.getConteudoHtml());
    assertThat(copia.getVersaoAtual()).isEqualTo(1);
  }

  @Test
  void definirArquivado_deveManterModeloEGerarVersao() {
    UUID id = UUID.randomUUID();
    Projeto projeto = new Projeto("Portal", "portal", null, true);
    PaginaTemplate template = new PaginaTemplate(
        "CUSTOM_1", "Cadastro", null, "<h2>Cadastro</h2>", 1000, projeto, null);
    when(paginaTemplateRepository.findById(id)).thenReturn(Optional.of(template));

    PaginaTemplate arquivado = paginaTemplateService.definirArquivado(id, true, null);

    assertThat(arquivado.isAtivo()).isFalse();
    assertThat(arquivado.getVersaoAtual()).isEqualTo(2);
    verify(paginaTemplateVersaoRepository).save(any(PaginaTemplateVersao.class));
    verify(paginaTemplateRepository, never()).delete(any());
  }
}
