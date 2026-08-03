package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.entity.Publicacao;
import br.com.softon.portal.docflow.entity.PublicacaoChangelog;
import br.com.softon.portal.docflow.entity.StatusPublicacao;
import br.com.softon.portal.docflow.repository.PublicacaoChangelogRepository;
import br.com.softon.portal.docflow.repository.PublicacaoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicacaoServiceTest {

  @Mock PublicacaoRepository publicacaoRepository;
  @Mock ClienteService clienteService;
  @Mock GeradorPacoteService geradorPacoteService;
  @Mock GeradorManualPdfService geradorManualPdfService;
  @Mock PublicacaoWorkerService publicacaoWorkerService;
  @Mock PublicacaoChangelogRepository changelogRepository;
  @Mock br.com.softon.rbac.service.EscopoResolver escopoResolver;
  @Mock br.com.softon.rbac.service.AuditoriaService auditoriaService;

  PublicacaoService service;

  UUID clienteId;
  UUID publicacaoId;
  Cliente cliente;
  Principal principal;

  @BeforeEach
  void setUp() throws Exception {
    service = new PublicacaoService(publicacaoRepository, clienteService,
        geradorPacoteService, geradorManualPdfService, publicacaoWorkerService, changelogRepository,
        escopoResolver, auditoriaService, new ArquivoRemocaoService(), new ObjectMapper());
    when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(java.util.Optional.empty());
    when(escopoResolver.podeAcessarCliente(any())).thenReturn(true);
    org.mockito.Mockito.doNothing().when(escopoResolver).assertPodeEscreverEmCliente(any());
    clienteId = UUID.randomUUID();
    publicacaoId = UUID.randomUUID();
    cliente = new Cliente("ACME", "acme", true);
    setId(cliente, clienteId);
    principal = () -> "editor";

    when(publicacaoRepository.save(any(Publicacao.class))).thenAnswer(inv -> {
      Publicacao p = inv.getArgument(0);
      setId(p, publicacaoId);
      return p;
    });
  }

  @Test
  void gerar_criaPublicacaoEEnfileiraProcessamento() {
    when(clienteService.buscar(clienteId)).thenReturn(cliente);

    Publicacao p = service.gerar(clienteId, " 1.0.0 ", "obs", principal);

    assertThat(p.getVersao()).isEqualTo("1.0.0");
    assertThat(p.getCliente()).isEqualTo(cliente);
    verify(publicacaoWorkerService).processar(publicacaoId, "editor");
  }

  @Test
  void gerar_falhaSeClienteInativo() {
    Cliente inativo = new Cliente("X", "x", false);
    when(clienteService.buscar(clienteId)).thenReturn(inativo);

    assertThatThrownBy(() -> service.gerar(clienteId, "1.0.0", null, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("inativo");
    verifyNoInteractions(publicacaoWorkerService);
  }

  @Test
  void gerar_usaSystemQuandoPrincipalNulo() {
    when(clienteService.buscar(clienteId)).thenReturn(cliente);

    service.gerar(clienteId, "1.0.0", null, null);

    verify(publicacaoWorkerService).processar(publicacaoId, "system");
  }

  @Test
  void reprocessar_reenfileiraQuandoStatusNaoEhGerando() {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    p.registrarErro("falha anterior");
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    Publicacao out = service.reprocessar(publicacaoId, principal);

    assertThat(out.getStatus()).isEqualTo(StatusPublicacao.GERANDO);
    verify(publicacaoWorkerService).processar(p.getId(), "editor");
  }

  @Test
  void reprocessar_bloqueiaQuandoAindaGerando() {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.reprocessar(publicacaoId, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("já está em geração");
    verifyNoInteractions(publicacaoWorkerService);
  }

  @Test
  void reprocessar_falhaSeInexistente() {
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reprocessar(publicacaoId, principal))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void buscar_falhaSeInexistente() {
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(publicacaoId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Publicação");
  }

  @Test
  void excluir_removeRegistroEArquivo(@TempDir Path tmp) throws Exception {
    Path zip = tmp.resolve("manual.zip");
    Files.writeString(zip, "pacote");
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    p.registrarSucesso(1, 1, "manual.zip", zip.toString(), "sha", "{}");
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    service.excluir(publicacaoId, principal);

    verify(escopoResolver).assertPodeEscreverEmCliente(clienteId);
    verify(publicacaoRepository).delete(p);
    verify(auditoriaService).registrar("PUBLICACAO", publicacaoId, "EXCLUIR",
        "Publicação 1.0.0 de ACME", principal);
    assertThat(zip).doesNotExist();
  }

  @Test
  void excluir_bloqueiaEnquantoPublicacaoEstaGerando() {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.excluir(publicacaoId, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("geração terminar");

    verify(publicacaoRepository, never()).delete(any());
    verifyNoInteractions(auditoriaService);
  }

  @Test
  void listarPageable_semClienteUsaFindAll() {
    Pageable pageable = Pageable.unpaged();
    Page<Publicacao> page = new PageImpl<>(List.of());
    when(publicacaoRepository.findAll(pageable)).thenReturn(page);

    Page<Publicacao> out = service.listar(null, null, pageable);

    assertThat(out).isSameAs(page);
  }

  @Test
  void listarPageable_comClienteFiltra() {
    Pageable pageable = Pageable.unpaged();
    Page<Publicacao> page = new PageImpl<>(List.of());
    when(publicacaoRepository.findByCliente_Id(clienteId, pageable)).thenReturn(page);

    Page<Publicacao> out = service.listar(clienteId, null, pageable);

    assertThat(out).isSameAs(page);
  }

  @Test
  void listarPageable_comStatusFiltraNoRepositorio() {
    Pageable pageable = Pageable.unpaged();
    Page<Publicacao> page = new PageImpl<>(List.of());
    when(publicacaoRepository.findByStatus(StatusPublicacao.ERRO, pageable)).thenReturn(page);

    Page<Publicacao> out = service.listar(null, StatusPublicacao.ERRO, pageable);

    assertThat(out).isSameAs(page);
  }

  @Test
  void reprocessarLote_ignoraGerandoERemoveDuplicados() throws Exception {
    UUID outroId = UUID.randomUUID();
    Publicacao comErro = new Publicacao(cliente, "1.0.0", null);
    comErro.registrarErro("falha");
    setId(comErro, publicacaoId);
    Publicacao gerando = new Publicacao(cliente, "2.0.0", null);
    setId(gerando, outroId);
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(comErro));
    when(publicacaoRepository.findById(outroId)).thenReturn(Optional.of(gerando));

    List<Publicacao> resultado = service.reprocessarLote(
        List.of(publicacaoId, publicacaoId, outroId), principal);

    assertThat(resultado).containsExactly(comErro);
    assertThat(comErro.getStatus()).isEqualTo(StatusPublicacao.GERANDO);
    verify(publicacaoWorkerService).processar(publicacaoId, "editor");
  }

  @Test
  void previewHtml_usaPreviaQuandoVersaoAusente() {
    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(geradorPacoteService.previewHtml(cliente, "prévia")).thenReturn("<html/>");

    String html = service.previewHtml(clienteId, null);

    assertThat(html).isEqualTo("<html/>");
  }

  @Test
  void manualPdfHtml_delegaAoGeradorDeManual() throws Exception {
    Publicacao publicacao = new Publicacao(cliente, "3.0.0", null);
    setId(publicacao, publicacaoId);
    when(geradorManualPdfService.montarHtmlManual(cliente, "3.0.0"))
        .thenReturn("<html>manual</html>");

    String html = service.manualPdfHtml(publicacao);

    assertThat(html).isEqualTo("<html>manual</html>");
    verify(geradorManualPdfService).montarHtmlManual(cliente, "3.0.0");
  }

  @Test
  void previewHtml_fazTrimDaVersao() {
    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(geradorPacoteService.previewHtml(cliente, "2.0.0")).thenReturn("<html/>");

    service.previewHtml(clienteId, "  2.0.0  ");

    verify(geradorPacoteService).previewHtml(cliente, "2.0.0");
  }

  @Test
  void diagnosticar_semPaginasRetornaErro() {
    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(geradorPacoteService.selecionarPaginas(clienteId)).thenReturn(List.of());

    var diag = service.diagnosticar(clienteId);

    assertThat(diag).hasSize(1);
    assertThat(diag.get(0).severidade()).isEqualTo("ERRO");
    assertThat(diag.get(0).mensagem()).contains("Não há páginas");
  }

  @Test
  void diagnosticar_subpaginaSemPaiNoPacote_emiteAviso() throws Exception {
    Projeto projeto = new Projeto("Projeto", "projeto", null, true);
    Modulo modulo = new Modulo("Módulo", "modulo", null, 0, true, projeto);
    Pagina pai = new Pagina("Pai", "pai", "PAI", "Resumo editorial com texto suficiente para diagnóstico.",
        "<p>Conteúdo útil com texto suficiente para passar na validação de diagnóstico editorial.</p>",
        0, true, modulo, null);
    UUID paiId = UUID.randomUUID();
    UUID filhoId = UUID.randomUUID();
    setId(pai, paiId);
    Pagina filho = new Pagina("Filho", "filho", "FILHO", "Resumo editorial com texto suficiente para diagnóstico.",
        "<p>Conteúdo útil com texto suficiente para passar na validação de diagnóstico editorial.</p>",
        1, true, modulo, pai);
    setId(filho, filhoId);

    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(geradorPacoteService.selecionarPaginas(clienteId)).thenReturn(List.of(filho));

    var diag = service.diagnosticar(clienteId);

    assertThat(diag).anyMatch(item -> "AVISO".equals(item.severidade())
        && item.mensagem().contains("pai no pacote")
        && filhoId.equals(item.paginaId()));
  }

  @Test
  void listarChangelog_delegaAoRepositorio() {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));
    var itens = List.of(new PublicacaoChangelog(publicacaoId, UUID.randomUUID(), "P", "ADICIONADO"));
    when(changelogRepository.findByPublicacaoIdOrderByCreatedAtAsc(publicacaoId)).thenReturn(itens);

    var out = service.listarChangelog(publicacaoId);

    assertThat(out).isSameAs(itens);
  }

  @Test
  void arvorePaginas_semSnapshotRetornaListaVazia() {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    assertThat(service.arvorePaginas(publicacaoId)).isEmpty();
  }

  @Test
  void arvorePaginas_comJsonValidoRetornaItens() throws Exception {
    UUID paginaId = UUID.randomUUID();
    String json = new ObjectMapper().writeValueAsString(List.of(
        new br.com.softon.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem(
            paginaId, null, "Página", "TELA", "pagina", 0, 0)));
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    p.definirArvorePaginas(json);
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    var itens = service.arvorePaginas(publicacaoId);

    assertThat(itens).hasSize(1);
    assertThat(itens.get(0).id()).isEqualTo(paginaId);
    assertThat(itens.get(0).nivel()).isZero();
  }

  @Test
  void listarChangelog_falhaSePublicacaoInexistente() {
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.listarChangelog(publicacaoId))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(changelogRepository);
  }

  @Test
  void arquivo_semCaminhoSalvo_falha() {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.arquivo(publicacaoId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("ainda não possui arquivo");
  }

  @Test
  void arquivo_pathInexistente_falha(@TempDir Path tmp) {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    p.registrarSucesso(1, 1, "z.zip", tmp.resolve("nao-existe.zip").toString(), "sha", "{}");
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.arquivo(publicacaoId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("disco");
  }

  @Test
  void arquivo_pathExistente_retornaResource(@TempDir Path tmp) throws Exception {
    Path zip = tmp.resolve("manual.zip");
    Files.writeString(zip, "PK");
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    p.registrarSucesso(1, 1, "manual.zip", zip.toString(), "sha", "{}");
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    var res = service.arquivo(publicacaoId);

    assertThat(res.exists()).isTrue();
    assertThat(res.getFilename()).isEqualTo("manual.zip");
  }

  @Test
  void downloadPublico_bloqueiaSeStatusNaoEhSucesso() {
    Publicacao p = new Publicacao(cliente, "1.0.0", null);
    p.registrarErro("x");
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.recursoPacoteDownloadPublico(publicacaoId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("apenas para publicações concluídas");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
