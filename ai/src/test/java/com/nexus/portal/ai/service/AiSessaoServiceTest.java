package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.request.AiMensagemRequest;
import com.nexus.portal.ai.dto.request.CriarAiSessaoRequest;
import com.nexus.portal.ai.dto.response.AiSessaoResponse;
import com.nexus.portal.ai.entity.AiMensagem;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiPapelMensagem;
import com.nexus.portal.ai.entity.AiSessao;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.repository.AiMensagemRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AiSessaoServiceTest {

  @Mock AiSessaoRepository sessaoRepository;
  @Mock AiMensagemRepository mensagemRepository;
  @Mock AuditoriaService auditoriaService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final List<AiMensagem> mensagens = new ArrayList<>();
  private AiSessaoService service;
  private AiProperties propsEnabled;

  @BeforeEach
  void setUp() {
    propsEnabled = new AiProperties(true, null, "sk-test", "gpt-test", null, null, 30, 5, 1000, 20);
    service = new AiSessaoService(
        sessaoRepository,
        mensagemRepository,
        new AiTriagemService(propsEnabled),
        propsEnabled,
        objectMapper,
        auditoriaService);

    lenient().when(sessaoRepository.save(any(AiSessao.class))).thenAnswer(inv -> {
      AiSessao s = inv.getArgument(0);
      if (s.getId() == null) {
        setId(s, UUID.randomUUID());
      }
      return s;
    });
    lenient().when(mensagemRepository.save(any(AiMensagem.class))).thenAnswer(inv -> {
      AiMensagem m = inv.getArgument(0);
      if (m.getId() == null) {
        try {
          var idField = AiMensagem.class.getDeclaredField("id");
          idField.setAccessible(true);
          idField.set(m, UUID.randomUUID());
        } catch (Exception ex) {
          throw new IllegalStateException(ex);
        }
      }
      mensagens.add(m);
      return m;
    });
    lenient().when(mensagemRepository.countBySessaoId(any())).thenAnswer(inv -> mensagens.size());
    lenient().when(mensagemRepository.findBySessaoIdOrderByOrdemAsc(any()))
        .thenAnswer(inv -> List.copyOf(mensagens));
  }

  @Test
  void criarComBriefingCurtoAguardaUsuario() {
    CriarAiSessaoRequest request = new CriarAiSessaoRequest(
        AiObjetivo.CRIAR_PAGINA,
        "Documentar a nova tela operacional de pedidos do portal Nexus para o time de atendimento.",
        null, null, null, null, null);

    AiSessaoResponse response = service.criar(request, null);

    assertThat(response.status()).isEqualTo(AiSessaoStatus.AGUARDANDO_USUARIO);
    assertThat(response.mensagens()).anyMatch(m ->
        m.papel() == AiPapelMensagem.ASSISTENTE && !m.perguntas().isEmpty());
  }

  @Test
  void criarComBriefingRicoFicaProntoParaGerar() {
    String briefing = """
        Consulta de pedidos
        codigoTela: PED-CONSULTA
        Público operador. Fluxo completo: filtrar por período, listar resultados e exportar CSV.
        Pré-requisito PEDIDO:LER. Resultado esperado: planilha baixada com os filtros aplicados.
        """;

    AiSessaoResponse response = service.criar(new CriarAiSessaoRequest(
        AiObjetivo.CRIAR_PAGINA, briefing, null, null, null, null, null), null);

    assertThat(response.status()).isEqualTo(AiSessaoStatus.PRONTA_PARA_GERAR);
  }

  @Test
  void criarComModuloDesligadoRetorna503() {
    AiProperties off = new AiProperties(false, null, null, null, null, null, 30, 5, 1000, 20);
    AiSessaoService offService = new AiSessaoService(
        sessaoRepository, mensagemRepository, new AiTriagemService(off), off, objectMapper, auditoriaService);

    assertThatThrownBy(() -> offService.criar(new CriarAiSessaoRequest(
        AiObjetivo.CRIAR_PAGINA,
        "Briefing longo o suficiente para passar na validação mínima do request DTO.",
        null, null, null, null, null), null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void enviarRespostasCompletaSessao() {
    AiSessao sessao = new AiSessao(
        AiObjetivo.CRIAR_PAGINA,
        "Documentar a nova tela operacional de pedidos do portal Nexus.",
        null, null, null, null, null);
    UUID id = UUID.randomUUID();
    setId(sessao, id);
    sessao.aguardarUsuario();
    when(sessaoRepository.findById(id)).thenReturn(Optional.of(sessao));

    AiSessaoResponse response = service.enviarMensagem(id, new AiMensagemRequest(
        "Seguem as respostas.",
        Map.of(
            "titulo", "Consulta de pedidos",
            "codigoTela", "PED-CONSULTA",
            "fluxo", "Filtrar, listar e exportar.")));

    assertThat(response.status()).isEqualTo(AiSessaoStatus.PRONTA_PARA_GERAR);
  }

  @Test
  void cancelarMarcaSessao() {
    AiSessao sessao = new AiSessao(
        AiObjetivo.CRIAR_PAGINA,
        "Documentar a nova tela operacional de pedidos do portal Nexus.",
        null, null, null, null, null);
    UUID id = UUID.randomUUID();
    setId(sessao, id);
    when(sessaoRepository.findById(id)).thenReturn(Optional.of(sessao));

    AiSessaoResponse response = service.cancelar(id, null);

    assertThat(response.status()).isEqualTo(AiSessaoStatus.CANCELADA);
    ArgumentCaptor<AiMensagem> captor = ArgumentCaptor.forClass(AiMensagem.class);
    verify(mensagemRepository).save(captor.capture());
    assertThat(captor.getValue().getPapel()).isEqualTo(AiPapelMensagem.SISTEMA);
  }

  private static void setId(AiSessao sessao, UUID id) {
    try {
      var field = AiSessao.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(sessao, id);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
