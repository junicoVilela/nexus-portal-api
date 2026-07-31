package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.service.ModuloService;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.dto.request.PaginaRequest;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.PaginaAnexo;
import br.com.softon.portal.docflow.entity.StatusPagina;
import br.com.softon.portal.docflow.repository.PaginaRepository;
import br.com.softon.portal.docflow.repository.PaginaAnexoRepository;
import br.com.softon.portal.docflow.repository.PaginaRevisaoRepository;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.rbac.service.AuditoriaService;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.ConflictException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaginaServiceTest {

  @Mock PaginaRepository paginaRepository;
  @Mock PaginaRevisaoRepository paginaRevisaoRepository;
  @Mock ModuloService moduloService;
  @Mock AuditoriaService auditoriaService;
  @Mock PaginaAnexoRepository paginaAnexoRepository;
  @Mock PaginaEventService paginaEventService;

  PaginaService service;

  Projeto projetoPadrao;
  Modulo moduloPadrao;
  Principal principal;

  @BeforeEach
  void setUp() {
    service = new PaginaService(paginaRepository, paginaRevisaoRepository,
        moduloService, auditoriaService, new PaginaQualidadeService(), paginaAnexoRepository,
        new ArquivoRemocaoService(), paginaEventService);

    projetoPadrao = new Projeto("Projeto Teste", "projeto-teste", null, true);
    moduloPadrao = new Modulo("Módulo Teste", "modulo-teste", null, 1, true, projetoPadrao);
    try {
      setField(moduloPadrao, "id", UUID.randomUUID());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    principal = () -> "editor";

    when(paginaRepository.save(any())).thenAnswer((Answer<Pagina>) inv -> inv.getArgument(0));
    when(paginaRevisaoRepository.countByPagina_Id(any())).thenReturn(0);
    when(paginaRevisaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void criar_comDadosValidos_deveCriarPaginaComStatusRascunho() {
    UUID moduloId = UUID.randomUUID();
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlug(any())).thenReturn(false);
    when(paginaRepository.existsByCodigoTela(any())).thenReturn(false);
    UUID templateId = UUID.randomUUID();

    PaginaRequest request = new PaginaRequest("Título Teste", null, "TELA-001", "resumo",
        "<p>conteúdo</p>", 1, true, moduloId, null, null, templateId, 3);

    Pagina pagina = service.criar(request, principal);

    assertThat(pagina.getTitulo()).isEqualTo("Título Teste");
    assertThat(pagina.getStatus()).isEqualTo(StatusPagina.RASCUNHO);
    assertThat(pagina.getTemplateOrigemId()).isEqualTo(templateId);
    assertThat(pagina.getTemplateOrigemVersao()).isEqualTo(3);
  }

  @Test
  void criar_comBlocosVisuais_devePreservarEstruturaSeguraDoTemplate() {
    UUID moduloId = UUID.randomUUID();
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlug(any())).thenReturn(false);
    when(paginaRepository.existsByCodigoTela(any())).thenReturn(false);

    PaginaRequest request = new PaginaRequest("Cadastro", null, "CAD-001", null,
        "<section class=\"doc-intro\"><h2>Introdução</h2></section>"
            + "<div class=\"table-wrap\"><table><tr><td>Campo</td></tr></table></div>"
            + "<script>alert('xss')</script>",
        0, true, moduloId, null, null);

    Pagina pagina = service.criar(request, principal);

    assertThat(pagina.getConteudoHtml())
        .contains("class=\"doc-intro\"", "class=\"table-wrap\"", "<table>")
        .doesNotContain("<script>", "alert('xss')");
  }

  @Test
  void criar_comSlugDuplicado_deveLancarBusinessException() {
    UUID moduloId = UUID.randomUUID();
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlug(any())).thenReturn(true);

    PaginaRequest request = new PaginaRequest("Título", "slug-existente", "TELA-001",
        null, null, 0, true, moduloId, null, null);

    assertThatThrownBy(() -> service.criar(request, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("slug");
  }

  @Test
  void autosave_comMesmaVersao_deveAtualizarSemCriarRevisao() {
    UUID id = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.RASCUNHO);
    pagina.setVersion(3);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlugAndIdNot(any(), any())).thenReturn(false);
    when(paginaRepository.existsByCodigoTelaAndIdNot(any(), any())).thenReturn(false);
    PaginaRequest request = new PaginaRequest("Título atualizado", "titulo-atualizado", "TELA-002",
        "Resumo atualizado com detalhes suficientes.",
        "<h2>Orientação</h2><p>Conteúdo atualizado automaticamente no servidor.</p>",
        1, true, moduloId, null, 3L);

    Pagina resultado = service.autosave(id, request);

    assertThat(resultado.getTitulo()).isEqualTo("Título atualizado");
    verify(paginaRepository).flush();
    verify(paginaRevisaoRepository, never()).save(any());
  }

  @Test
  void autosave_comVersaoAntiga_deveRecusarSobrescrita() {
    UUID id = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.RASCUNHO);
    pagina.setVersion(4);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));
    PaginaRequest request = new PaginaRequest("Título", "titulo", "TELA", null,
        "<p>Conteúdo</p>", 0, true, UUID.randomUUID(), null, 3L);

    assertThatThrownBy(() -> service.autosave(id, request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("outro usuário");
  }

  @Test
  void publicar_comPaginaAprovada_deveTransicionarParaPublicado() {
    UUID id = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.APROVADO);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));
    when(paginaRevisaoRepository.countByPagina_Id(id)).thenReturn(1);

    Pagina resultado = service.publicar(id, principal);

    assertThat(resultado.getStatus()).isEqualTo(StatusPagina.PUBLICADO);
    assertThat(resultado.getPublishedAt()).isNotNull();
  }

  @Test
  void publicar_comPaginaRascunho_deveLancarBusinessException() {
    UUID id = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.RASCUNHO);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));

    assertThatThrownBy(() -> service.publicar(id, principal))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void aprovar_comPaginaEmRevisao_deveTransicionarParaAprovado() {
    UUID id = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.EM_REVISAO);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));
    when(paginaRevisaoRepository.countByPagina_Id(id)).thenReturn(1);

    Pagina resultado = service.aprovar(id, principal);

    assertThat(resultado.getStatus()).isEqualTo(StatusPagina.APROVADO);
  }

  @Test
  void aprovar_comPaginaNaoEmRevisao_deveLancarBusinessException() {
    UUID id = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.RASCUNHO);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));

    assertThatThrownBy(() -> service.aprovar(id, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("revisão");
  }

  @Test
  void salvarRascunho_deveRetornarParaStatusRascunho() {
    UUID id = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.EM_REVISAO);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));
    when(paginaRevisaoRepository.countByPagina_Id(id)).thenReturn(1);

    Pagina resultado = service.salvarRascunho(id, principal);

    assertThat(resultado.getStatus()).isEqualTo(StatusPagina.RASCUNHO);
  }

  @Test
  void arquivar_deveDefinirStatusArquivado() {
    UUID id = UUID.randomUUID();
    Pagina pagina = pagina(id, StatusPagina.PUBLICADO);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));
    when(paginaRepository.findByParent_Id(id)).thenReturn(List.of());
    when(paginaRevisaoRepository.countByPagina_Id(id)).thenReturn(1);

    Pagina resultado = service.arquivar(id, principal);

    assertThat(resultado.getStatus()).isEqualTo(StatusPagina.ARQUIVADO);
  }

  @Test
  void arquivar_cascateiaParaFilhosAntesDoPai() throws Exception {
    UUID paiId = UUID.randomUUID();
    UUID filhoId = UUID.randomUUID();
    Pagina pai = pagina(paiId, StatusPagina.PUBLICADO);
    Pagina filho = pagina(filhoId, StatusPagina.PUBLICADO);
    setField(filho, "parent", pai);

    when(paginaRepository.findById(paiId)).thenReturn(Optional.of(pai));
    when(paginaRepository.findByParent_Id(paiId)).thenReturn(List.of(filho));
    when(paginaRepository.findByParent_Id(filhoId)).thenReturn(List.of());
    when(paginaRevisaoRepository.countByPagina_Id(any())).thenReturn(0);

    service.arquivar(paiId, principal);

    assertThat(filho.getStatus()).isEqualTo(StatusPagina.ARQUIVADO);
    assertThat(pai.getStatus()).isEqualTo(StatusPagina.ARQUIVADO);
    verify(auditoriaService).registrar(eq("PAGINA"), eq(filhoId), eq("ARQUIVAR"), any(), eq(principal));
    verify(auditoriaService).registrar(eq("PAGINA"), eq(paiId), eq("ARQUIVAR"), any(), eq(principal));
    verify(paginaRevisaoRepository, times(2)).save(any());
  }

  @Test
  void arquivar_ignoraFilhosJaArquivados() throws Exception {
    UUID paiId = UUID.randomUUID();
    UUID filhoId = UUID.randomUUID();
    Pagina pai = pagina(paiId, StatusPagina.PUBLICADO);
    Pagina filho = pagina(filhoId, StatusPagina.ARQUIVADO);
    setField(filho, "parent", pai);

    when(paginaRepository.findById(paiId)).thenReturn(Optional.of(pai));
    when(paginaRepository.findByParent_Id(paiId)).thenReturn(List.of(filho));
    when(paginaRevisaoRepository.countByPagina_Id(paiId)).thenReturn(0);

    service.arquivar(paiId, principal);

    assertThat(pai.getStatus()).isEqualTo(StatusPagina.ARQUIVADO);
    verify(paginaRevisaoRepository, times(1)).save(any());
  }

  @Test
  void atualizar_comCicloNaHierarquia_deveLancarBusinessException() throws Exception {
    UUID paginaId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    Pagina pagina = pagina(paginaId, StatusPagina.RASCUNHO);
    Pagina parent = pagina(parentId, StatusPagina.RASCUNHO);
    setField(parent, "parent", pagina);

    when(paginaRepository.findById(paginaId)).thenReturn(Optional.of(pagina));
    when(paginaRepository.findById(parentId)).thenReturn(Optional.of(parent));
    UUID moduloId = UUID.randomUUID();
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlugAndIdNot(any(), any())).thenReturn(false);
    when(paginaRepository.existsByCodigoTelaAndIdNot(any(), any())).thenReturn(false);

    PaginaRequest request = new PaginaRequest("Título", "slug", "TELA", null,
        "<p>conteúdo</p>", 0, true, moduloId, parentId, 0L);

    assertThatThrownBy(() -> service.atualizar(paginaId, request, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("ciclo");
  }

  @Test
  void reordenar_paginasDoMesmoPai_deveAtualizarOrdem() throws Exception {
    UUID parentId = UUID.randomUUID();
    Pagina parent = pagina(parentId, StatusPagina.RASCUNHO);
    UUID idA = UUID.randomUUID();
    UUID idB = UUID.randomUUID();
    Pagina paginaA = pagina(idA, StatusPagina.RASCUNHO);
    Pagina paginaB = pagina(idB, StatusPagina.RASCUNHO);
    setField(paginaA, "parent", parent);
    setField(paginaB, "parent", parent);
    setField(paginaA, "ordem", 0);
    setField(paginaB, "ordem", 1);

    when(paginaRepository.findAllById(List.of(idB, idA))).thenReturn(List.of(paginaB, paginaA));

    service.reordenar(List.of(idB, idA), principal);

    assertThat(paginaB.getOrdem()).isZero();
    assertThat(paginaA.getOrdem()).isEqualTo(1);
  }

  @Test
  void excluir_semSubpaginas_removePaginaAnexosFisicosEAudita(@TempDir Path tmp) throws Exception {
    UUID id = UUID.randomUUID();
    Path arquivo = tmp.resolve("anexo.png");
    Files.writeString(arquivo, "imagem");
    Pagina pagina = pagina(id, StatusPagina.RASCUNHO);
    when(paginaRepository.findById(id)).thenReturn(Optional.of(pagina));
    when(paginaRepository.findByParent_Id(id)).thenReturn(List.of());
    when(paginaAnexoRepository.findByPagina_Id(id))
        .thenReturn(java.util.List.of(new PaginaAnexo(pagina, "anexo.png", "image/png", 6, arquivo.toString())));

    service.excluir(id, principal);

    verify(paginaRepository).delete(pagina);
    verify(auditoriaService).registrar("PAGINA", id, "EXCLUIR", "Página excluída: Título", principal);
    assertThat(arquivo).doesNotExist();
  }

  @Test
  void excluir_cascateiaParaFilhos() throws Exception {
    UUID paiId = UUID.randomUUID();
    UUID filhoId = UUID.randomUUID();
    Pagina pai = pagina(paiId, StatusPagina.RASCUNHO);
    Pagina filho = pagina(filhoId, StatusPagina.RASCUNHO);
    setField(filho, "parent", pai);

    when(paginaRepository.findById(paiId)).thenReturn(Optional.of(pai));
    when(paginaRepository.findByParent_Id(paiId)).thenReturn(List.of(filho));
    when(paginaRepository.findByParent_Id(filhoId)).thenReturn(List.of());
    when(paginaAnexoRepository.findByPagina_Id(any())).thenReturn(List.of());

    service.excluir(paiId, principal);

    verify(paginaRepository).delete(filho);
    verify(paginaRepository).delete(pai);
    verify(auditoriaService).registrar(eq("PAGINA"), eq(filhoId), eq("EXCLUIR"), any(), eq(principal));
    verify(auditoriaService).registrar(eq("PAGINA"), eq(paiId), eq("EXCLUIR"), any(), eq(principal));
  }

  @Test
  void excluir_comSubpaginasCascateiaRemocao() throws Exception {
    UUID paiId = UUID.randomUUID();
    UUID filhoId = UUID.randomUUID();
    Pagina pai = pagina(paiId, StatusPagina.RASCUNHO);
    Pagina filho = pagina(filhoId, StatusPagina.RASCUNHO);
    setField(filho, "parent", pai);

    when(paginaRepository.findById(paiId)).thenReturn(Optional.of(pai));
    when(paginaRepository.findByParent_Id(paiId)).thenReturn(List.of(filho));
    when(paginaRepository.findByParent_Id(filhoId)).thenReturn(List.of());
    when(paginaAnexoRepository.findByPagina_Id(any())).thenReturn(List.of());

    service.excluir(paiId, principal);

    verify(paginaRepository).delete(filho);
    verify(paginaRepository).delete(pai);
    verify(paginaRepository, never()).existsByParent_Id(any());
  }

  @Test
  void publicar_comPaiComSecaoGuias_sincronizaIndice() throws Exception {
    UUID paiId = UUID.randomUUID();
    UUID filhoId = UUID.randomUUID();
    String htmlPai = """
        <section class="doc-intro"><h2>Operações</h2></section>
        <section class="doc-section"><h2>Guias disponíveis</h2><div class="resource-list resource-list--large"></div></section>
        """;
    Pagina pai = pagina(paiId, StatusPagina.APROVADO);
    setField(pai, "conteudoHtml", htmlPai);
    Pagina filho = pagina(filhoId, StatusPagina.APROVADO);
    setField(filho, "parent", pai);
    setField(filho, "titulo", "Lista de registros");
    setField(filho, "codigoTela", "EXEMPLO-LISTA");
    setField(filho, "resumo", "Como consultar registros na listagem.");
    setField(filho, "ordem", 1);

    when(paginaRepository.findById(filhoId)).thenReturn(Optional.of(filho));
    when(paginaRepository.findByParent_Id(paiId)).thenReturn(List.of(filho));
    when(paginaRevisaoRepository.countByPagina_Id(any())).thenReturn(0);

    service.publicar(filhoId, principal);

    assertThat(pai.getConteudoHtml())
        .contains("Lista de registros", "EXEMPLO-LISTA", "resource-item");
    verify(paginaRepository).save(pai);
    verify(paginaRevisaoRepository, times(2)).save(any());
  }

  @Test
  void criar_semCodigoTela_deveLancarBusinessException() {
    UUID moduloId = UUID.randomUUID();
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlug(any())).thenReturn(false);

    PaginaRequest request = new PaginaRequest("Título", null, "", null, null, 0, true, moduloId, null, null);

    assertThatThrownBy(() -> service.criar(request, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Código da tela");
  }

  private Pagina pagina(UUID id, StatusPagina status) {
    Pagina p = new Pagina("Título", "slug", "TELA", null, "<p>conteúdo</p>",
        0, true, moduloPadrao, null);
    try {
      setField(p, "id", id);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    switch (status) {
      case EM_REVISAO -> p.enviarRevisao();
      case APROVADO -> { p.enviarRevisao(); p.aprovar(); }
      case PUBLICADO -> { p.enviarRevisao(); p.aprovar(); p.publicar(); }
      case ARQUIVADO -> p.arquivar();
      default -> { }
    }
    return p;
  }

  private static void setField(Object entity, String field, Object value) throws Exception {
    Field f = entity.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(entity, value);
  }
}
