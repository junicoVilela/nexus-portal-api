package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.entity.StatusPagina;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class PaginaEventServiceTest {

  private final PaginaEventService service = new PaginaEventService();

  @Test
  void inscrever_deveRetornarEmitterConfigurado() {
    SseEmitter emitter = service.inscrever();
    assertThat(emitter).isNotNull();
    assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L);
  }

  @Test
  void publicar_naoDeveFalharSemAssinantes() {
    Pagina pagina = paginaExemplo();
    service.publicar(pagina, "APROVAR", "revisor");
  }

  private Pagina paginaExemplo() {
    Projeto projeto = new Projeto("Projeto", "projeto", null, true);
    Modulo modulo = new Modulo("Módulo", "modulo", null, 1, true, projeto);
    Pagina pagina = new Pagina("Página teste", "pagina-teste", "TELA-01", "Resumo", "<p>ok</p>", 0, true,
        modulo, null);
    pagina.enviarRevisao();
    pagina.aprovar();
    try {
      setField(pagina, "id", UUID.randomUUID());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    assertThat(pagina.getStatus()).isEqualTo(StatusPagina.APROVADO);
    return pagina;
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
