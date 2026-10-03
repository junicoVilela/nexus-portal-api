package com.nexus.portal.ai.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Guarda-corpo das permissões do assistente (AI-704): todo endpoint declara {@code @PreAuthorize};
 * o que consome a IA ou muda estado exige uma ação {@code PAGINA:AI_*}; o que grava página do
 * DocFlow exige {@code PAGINA:AI_APLICAR} além de criar/editar páginas.
 */
class AiPermissoesDosEndpointsTest {

  private static final List<Class<?>> CONTROLLERS = List.of(
      AiSessaoController.class,
      AiDocumentoImportacaoController.class,
      AiAjustePaginaController.class,
      AiTemplateController.class,
      AiMetricasController.class,
      AiStatusController.class,
      AiEventController.class);

  /** POSTs que só leem: recomendação de modelo e recarga do estado da importação. */
  private static final Set<String> ESCRITA_SEM_IA = Set.of(
      "AiTemplateController#recomendar",
      "AiDocumentoImportacaoController#sincronizarImportacaoDocumento");

  /** Endpoints que criam/alteram páginas do DocFlow a partir de uma proposta. */
  private static final Set<String> GRAVAM_PAGINA = Set.of(
      "AiSessaoController#aplicar",
      "AiDocumentoImportacaoController#aceitarPaginaImportada");

  @Test
  void todoEndpointDeclaraPermissao() {
    List<String> semPermissao = new ArrayList<>();
    endpoints().forEach(m -> {
      if (m.getAnnotation(PreAuthorize.class) == null) {
        semPermissao.add(nome(m));
      }
    });
    assertThat(semPermissao).as("endpoints de IA sem @PreAuthorize").isEmpty();
  }

  @Test
  void escritaExigeAcaoDaIa() {
    List<String> divergentes = new ArrayList<>();
    endpoints().stream()
        .filter(AiPermissoesDosEndpointsTest::escrita)
        .filter(m -> !ESCRITA_SEM_IA.contains(nome(m)))
        .filter(m -> !permissao(m).contains("PAGINA:AI_"))
        .forEach(m -> divergentes.add(nome(m) + " -> " + permissao(m)));
    assertThat(divergentes).as("escritas do assistente sem PAGINA:AI_*").isEmpty();
  }

  @Test
  void quemGravaPaginaExigeAplicarEPermissaoDePagina() {
    List<String> divergentes = new ArrayList<>();
    endpoints().stream()
        .filter(m -> GRAVAM_PAGINA.contains(nome(m)))
        .filter(m -> !permissao(m).contains("PAGINA:AI_APLICAR")
            || !(permissao(m).contains("PAGINA:CRIAR") || permissao(m).contains("PAGINA:EDITAR")))
        .forEach(m -> divergentes.add(nome(m) + " -> " + permissao(m)));
    assertThat(divergentes).isEmpty();
    assertThat(endpoints().stream().map(AiPermissoesDosEndpointsTest::nome))
        .as("lista GRAVAM_PAGINA desatualizada")
        .containsAll(GRAVAM_PAGINA);
  }

  private static List<Method> endpoints() {
    return CONTROLLERS.stream()
        .flatMap(c -> java.util.Arrays.stream(c.getDeclaredMethods()))
        .filter(AiPermissoesDosEndpointsTest::ehEndpoint)
        .toList();
  }

  private static boolean ehEndpoint(Method m) {
    return escrita(m) || m.isAnnotationPresent(GetMapping.class);
  }

  private static boolean escrita(Method m) {
    return m.isAnnotationPresent(PostMapping.class)
        || m.isAnnotationPresent(PutMapping.class)
        || m.isAnnotationPresent(PatchMapping.class)
        || m.isAnnotationPresent(DeleteMapping.class);
  }

  private static String permissao(Method m) {
    PreAuthorize preAuthorize = m.getAnnotation(PreAuthorize.class);
    return preAuthorize == null ? "" : preAuthorize.value();
  }

  private static String nome(Method m) {
    return m.getDeclaringClass().getSimpleName() + "#" + m.getName();
  }
}
