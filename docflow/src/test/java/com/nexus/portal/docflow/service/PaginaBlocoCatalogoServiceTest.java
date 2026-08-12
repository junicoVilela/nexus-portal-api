package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaginaBlocoCatalogoServiceTest {

  private final PaginaBlocoCatalogoService service =
      new PaginaBlocoCatalogoService(new ObjectMapper());

  @Test
  void carregaCatalogoCompletoComIdsUnicosESlots() {
    var catalogo = service.listar();

    assertThat(catalogo).hasSize(45);
    assertThat(catalogo).extracting(bloco -> bloco.id()).doesNotHaveDuplicates();
    assertThat(service.buscar("introducao").slots())
        .extracting(slot -> slot.id())
        .containsExactly("t1", "t2", "t3");
  }

  @Test
  void renderizaTextosSemPermitirInjecaoDeHtmlNemAlterarEstrutura() {
    String html = service.renderizar(
        "introducao",
        Map.of("t2", "Consulta de pedidos", "t3", "<script>alert('x')</script>"));

    assertThat(html)
        .contains("class=\"doc-intro\"")
        .contains("Consulta de pedidos")
        .contains("&lt;script&gt;")
        .doesNotContain("<script>");
  }
}
