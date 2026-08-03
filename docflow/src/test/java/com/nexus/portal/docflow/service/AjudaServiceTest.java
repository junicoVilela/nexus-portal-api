package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.dto.request.AjudaConteudoRequest;
import com.nexus.portal.docflow.dto.request.AjudaEventoRequest;
import com.nexus.portal.docflow.entity.AjudaConteudo;
import com.nexus.portal.docflow.entity.AjudaEvento;
import com.nexus.portal.docflow.entity.TipoAjudaConteudo;
import com.nexus.portal.docflow.entity.TipoAjudaEvento;
import com.nexus.portal.docflow.entity.TipoAjudaMedia;
import com.nexus.portal.docflow.repository.AjudaConteudoRepository;
import com.nexus.portal.docflow.repository.AjudaEventoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AjudaServiceTest {

  @Mock private AjudaConteudoRepository conteudoRepository;
  @Mock private AjudaEventoRepository eventoRepository;

  private AjudaService service;

  @BeforeEach
  void setUp() {
    service = new AjudaService(conteudoRepository, eventoRepository);
  }

  @Test
  void listar_comBuscaSemAcentoEContexto_deveFiltrarConteudo() {
    AjudaConteudo pagina = conteudo("PAGINA", "Criação de página", "/doc-flow/paginas");
    AjudaConteudo publicacao = conteudo("PUBLICACAO", "Gerar publicação", "/doc-flow/publicacoes");
    when(conteudoRepository.findByAtivoTrueOrderByOrdemAscTituloAsc())
        .thenReturn(List.of(pagina, publicacao));

    List<AjudaConteudo> resultado = service.listar("criacao", "/doc-flow/paginas/novo", false);

    assertThat(resultado).containsExactly(pagina);
  }

  @Test
  void criar_comCodigoDuplicado_deveRecusar() {
    when(conteudoRepository.existsByCodigo("ARTIGO_TESTE")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(request("ARTIGO_TESTE")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Já existe");
  }

  @Test
  void criar_comMidiaInsegura_deveRecusar() {
    AjudaConteudoRequest request = new AjudaConteudoRequest(
        "ARTIGO_VIDEO", TipoAjudaConteudo.ARTIGO, null, "Vídeo", null, null,
        "/doc-flow", null, null, "Play", null, TipoAjudaMedia.VIDEO,
        List.of("javascript:alert(1)"), "Demonstração", 1, true);

    assertThatThrownBy(() -> service.criar(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("HTTP");
  }

  @Test
  void criarEtapa_semJornadaExistente_deveRecusar() {
    AjudaConteudoRequest request = new AjudaConteudoRequest(
        "ETAPA_TESTE", TipoAjudaConteudo.ETAPA, "JORNADA_INEXISTENTE", "Etapa", null, null,
        "/doc-flow", null, null, "ListChecks", null, TipoAjudaMedia.NENHUMA,
        List.of(), null, 1, true);

    assertThatThrownBy(() -> service.criar(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não existe");
  }

  @Test
  void excluirJornada_comEtapasVinculadas_deveRecusar() {
    UUID id = UUID.randomUUID();
    AjudaConteudo jornada = new AjudaConteudo("JORNADA_TESTE", TipoAjudaConteudo.JORNADA, "Jornada");
    when(conteudoRepository.findById(id)).thenReturn(Optional.of(jornada));
    when(conteudoRepository.existsByJornadaCodigo("JORNADA_TESTE")).thenReturn(true);

    assertThatThrownBy(() -> service.excluir(id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("etapas vinculadas");
    verify(conteudoRepository, never()).delete(any());
  }

  @Test
  void registrar_devePersistirEventoSemConteudoSensivel() {
    AjudaEventoRequest request = new AjudaEventoRequest(
        TipoAjudaEvento.BUSCA, null, "como publicar", "/doc-flow/ajuda", "sessao", 3);

    service.registrar(request, "editor");

    verify(eventoRepository).save(any(AjudaEvento.class));
  }

  @Test
  void registrar_eventoRepetidoNaMesmaJanela_deveIgnorar() {
    AjudaEventoRequest request = new AjudaEventoRequest(
        TipoAjudaEvento.ONBOARDING_CONCLUIDO, null, null, "/doc-flow", "sessao", null);
    when(eventoRepository.contarEventoEquivalente(
        any(), any(), any(), any(), any(), any())).thenReturn(1L);

    service.registrar(request, "editor");

    verify(eventoRepository, never()).save(any());
  }

  private AjudaConteudo conteudo(String codigo, String titulo, String rota) {
    AjudaConteudo conteudo = new AjudaConteudo(codigo, TipoAjudaConteudo.ARTIGO, titulo);
    conteudo.atualizar(TipoAjudaConteudo.ARTIGO, null, titulo, "Orientação", null, rota,
        null, null, "BookOpen", null, TipoAjudaMedia.NENHUMA, null, null, 1, true);
    return conteudo;
  }

  private AjudaConteudoRequest request(String codigo) {
    return new AjudaConteudoRequest(codigo, TipoAjudaConteudo.ARTIGO, null, "Artigo", null,
        null, "/doc-flow", null, null, "BookOpen", null, TipoAjudaMedia.NENHUMA,
        List.of(), null, 1, true);
  }
}
