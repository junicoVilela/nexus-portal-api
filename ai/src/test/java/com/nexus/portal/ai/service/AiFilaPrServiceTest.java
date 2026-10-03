package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest.ModoAplicacao;
import com.nexus.portal.ai.dto.request.RejeitarAiPropostaRequest;
import com.nexus.portal.ai.entity.AiCategoriaRejeicao;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.repository.AiPrEventoRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiFilaPrServiceTest {

  private final AiPrEventoRepository eventoRepository = mock(AiPrEventoRepository.class);
  private final AiSessaoRepository sessaoRepository = mock(AiSessaoRepository.class);
  private final AiPropostaService propostaService = mock(AiPropostaService.class);
  private final AiFilaPrService service = new AiFilaPrService(eventoRepository, sessaoRepository, propostaService,
      mock(AiPrIngestaoService.class), mock(AuditoriaService.class));
  private final Principal autora = () -> "ana";
  private AiPrEvento evento;
  private AiSessao sessao;

  @BeforeEach
  void setUp() throws Exception {
    sessao = new AiSessao(AiObjetivo.CRIAR_PAGINA, "briefing do PR", UUID.randomUUID(), UUID.randomUUID(),
        null, null, null);
    definir(sessao, "id", UUID.randomUUID());
    definir(sessao, "createdBy", AiSessao.SISTEMA);
    evento = new AiPrEvento("d-1", "org/app", 7, "Tela nova", null, List.of(), "u", "dev", "main", null,
        OffsetDateTime.now());
    evento.emFila(sessao.getId(), null, "em geração");
    when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));
    when(sessaoRepository.findById(sessao.getId())).thenReturn(Optional.of(sessao));
    when(propostaService.ultimaPropostaDaFila(any())).thenReturn(Optional.empty());
  }

  @Test
  void assumirPassaASessaoParaQuemAgiu() {
    assertThat(sessao.pertenceA("ana")).isFalse();

    var item = service.assumir(evento.getId(), autora);

    assertThat(sessao.pertenceA("ana")).isTrue();
    assertThat(sessao.pertenceA(AiSessao.SISTEMA)).isFalse();
    assertThat(item.responsavel()).isEqualTo("ana");
  }

  @Test
  void rejeitarAssumeERejeitaComACategoria() {
    service.rejeitar(evento.getId(), new RejeitarAiPropostaRequest(AiCategoriaRejeicao.MODELO_ERRADO, null), autora);

    verify(propostaService).rejeitar(sessao.getId(), AiCategoriaRejeicao.MODELO_ERRADO, null, autora);
    assertThat(sessao.pertenceA("ana")).isTrue();
  }

  @Test
  void aceitarPaginaNovaCriaORascunho() {
    service.aceitar(evento.getId(), null, autora);

    ArgumentCaptor<AplicarAiPropostaRequest> request = ArgumentCaptor.forClass(AplicarAiPropostaRequest.class);
    verify(propostaService).aplicar(eq(sessao.getId()), request.capture(), eq(autora));
    assertThat(request.getValue().modo()).isEqualTo(ModoAplicacao.PERSISTIR);
  }

  @Test
  void ajusteNaoEhAceitoDaFila() throws Exception {
    definir(sessao, "objetivo", AiObjetivo.ATUALIZAR_PAGINA);

    assertThatThrownBy(() -> service.aceitar(evento.getId(), null, autora))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("editor");
  }

  @Test
  void itemComPropostaNaoReprocessa() {
    assertThatThrownBy(() -> service.reprocessar(evento.getId())).isInstanceOf(BusinessException.class);
  }

  private static void definir(Object alvo, String campo, Object valor) throws Exception {
    Class<?> tipo = alvo.getClass();
    while (tipo != null) {
      try {
        Field field = tipo.getDeclaredField(campo);
        field.setAccessible(true);
        field.set(alvo, valor);
        return;
      } catch (NoSuchFieldException ex) {
        tipo = tipo.getSuperclass();
      }
    }
    throw new NoSuchFieldException(campo);
  }
}
