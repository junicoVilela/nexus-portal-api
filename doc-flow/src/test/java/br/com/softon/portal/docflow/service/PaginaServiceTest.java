package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.service.ModuloService;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.dto.request.PaginaRequest;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.StatusPagina;
import br.com.softon.portal.docflow.repository.PaginaRepository;
import br.com.softon.portal.docflow.repository.PaginaRevisaoRepository;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.service.AuditoriaService;
import br.com.softon.portal.shared.exception.BusinessException;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  PaginaService service;

  Projeto projetoPadrao;
  Modulo moduloPadrao;
  Principal principal;

  @BeforeEach
  void setUp() {
    service = new PaginaService(paginaRepository, paginaRevisaoRepository,
        moduloService, auditoriaService);

    projetoPadrao = new Projeto("Projeto Teste", "projeto-teste", null, true);
    moduloPadrao = new Modulo("Módulo Teste", "modulo-teste", null, 1, true, projetoPadrao);

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

    PaginaRequest request = new PaginaRequest("Título Teste", null, "TELA-001", "resumo",
        "<p>conteúdo</p>", 1, true, moduloId, null);

    Pagina pagina = service.criar(request, principal);

    assertThat(pagina.getTitulo()).isEqualTo("Título Teste");
    assertThat(pagina.getStatus()).isEqualTo(StatusPagina.RASCUNHO);
  }

  @Test
  void criar_comSlugDuplicado_deveLancarBusinessException() {
    UUID moduloId = UUID.randomUUID();
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlug(any())).thenReturn(true);

    PaginaRequest request = new PaginaRequest("Título", "slug-existente", "TELA-001",
        null, null, 0, true, moduloId, null);

    assertThatThrownBy(() -> service.criar(request, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("slug");
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
    when(paginaRevisaoRepository.countByPagina_Id(id)).thenReturn(1);

    Pagina resultado = service.arquivar(id, principal);

    assertThat(resultado.getStatus()).isEqualTo(StatusPagina.ARQUIVADO);
  }

  @Test
  void criar_semCodigoTela_deveLancarBusinessException() {
    UUID moduloId = UUID.randomUUID();
    when(moduloService.buscar(moduloId)).thenReturn(moduloPadrao);
    when(paginaRepository.existsBySlug(any())).thenReturn(false);

    PaginaRequest request = new PaginaRequest("Título", null, "", null, null, 0, true, moduloId, null);

    assertThatThrownBy(() -> service.criar(request, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Código da tela");
  }

  private Pagina pagina(UUID id, StatusPagina status) {
    Pagina p = new Pagina("Título", "slug", "TELA", null, "<p>conteúdo</p>",
        0, true, moduloPadrao, null);
    switch (status) {
      case EM_REVISAO -> p.enviarRevisao();
      case APROVADO -> { p.enviarRevisao(); p.aprovar(); }
      case PUBLICADO -> { p.enviarRevisao(); p.aprovar(); p.publicar(); }
      case ARQUIVADO -> p.arquivar();
      default -> { }
    }
    return p;
  }
}
