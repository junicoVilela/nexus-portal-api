package com.nexus.portal.docflow.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.shared.security.Permissoes;
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
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Guarda-corpo das permissões: todo endpoint dos controllers do DocFlow precisa
 * declarar {@code @PreAuthorize} com uma constante do catálogo, salvo os que são
 * públicos por decisão explícita (download por token, logos, preview).
 *
 * <p>Sem isto, esquecer a anotação em um endpoint novo passa despercebido — ele
 * nasce exigindo apenas autenticação, sem checagem de permissão.
 */
class PermissoesDosEndpointsTest {

  private static final List<Class<?>> CONTROLLERS = List.of(
      PaginaController.class,
      PublicacaoController.class,
      ClienteController.class,
      ProjetoController.class,
      ModuloController.class,
      AjudaController.class,
      EmpresaController.class,
      PreviewController.class,
      DocFlowDashboardController.class,
      PublicDownloadController.class,
      ManualMcpController.class,
      ManualPublicoController.class,
      ManualAcessoController.class);

  /**
   * Endpoints intencionalmente públicos — liberados no {@code SecurityConfig}
   * porque a autorização vem de outro lugar (token assinado, token de preview)
   * ou porque o recurso é o branding exibido no manual gerado.
   */
  private static final Set<String> PUBLICOS_POR_DECISAO = Set.of(
      "PublicDownloadController#downloadZip",
      "PreviewController#preview",
      "PaginaController#baixarAnexo",
      "ClienteController#getLogo",
      "EmpresaController#getLogo",
      // MCP do manual: autenticado pelo token de prévia do cliente dentro do controller.
      "ManualMcpController#semStream",
      "ManualMcpController#mensagem",
      // Manual vigente para os sistemas do cliente: chave de integração no caminho.
      "ManualPublicoController#helpBridge",
      "ManualPublicoController#vigente",
      "ManualPublicoController#tela",
      "ManualPublicoController#raiz",
      "ManualPublicoController#site");

  @Test
  void todoEndpointDeclaraPermissaoOuEstaNaListaDePublicos() {
    List<String> semPermissao = new ArrayList<>();
    for (Class<?> controller : CONTROLLERS) {
      for (Method metodo : controller.getDeclaredMethods()) {
        if (!ehEndpoint(metodo)) {
          continue;
        }
        String nome = controller.getSimpleName() + "#" + metodo.getName();
        if (PUBLICOS_POR_DECISAO.contains(nome)) {
          continue;
        }
        if (metodo.getAnnotation(PreAuthorize.class) == null) {
          semPermissao.add(nome);
        }
      }
    }

    assertThat(semPermissao)
        .as("endpoints sem @PreAuthorize e fora da lista de públicos")
        .isEmpty();
  }

  @Test
  void permissoesUsamAsConstantesDoCatalogo() throws Exception {
    Set<String> catalogo = catalogoDePermissoes();
    List<String> foraDoCatalogo = new ArrayList<>();

    for (Class<?> controller : CONTROLLERS) {
      for (Method metodo : controller.getDeclaredMethods()) {
        PreAuthorize preAuthorize = metodo.getAnnotation(PreAuthorize.class);
        if (preAuthorize != null && !catalogo.contains(preAuthorize.value())) {
          foraDoCatalogo.add(controller.getSimpleName() + "#" + metodo.getName()
              + " -> " + preAuthorize.value());
        }
      }
    }

    assertThat(foraDoCatalogo)
        .as("expressões digitadas à mão em vez de constantes de Permissoes")
        .isEmpty();
  }

  @Test
  void escritaDePaginaNaoUsaPermissaoDeLeitura() {
    List<String> suspeitos = new ArrayList<>();
    for (Method metodo : PaginaController.class.getDeclaredMethods()) {
      PreAuthorize preAuthorize = metodo.getAnnotation(PreAuthorize.class);
      boolean escrita = metodo.isAnnotationPresent(PostMapping.class)
          || metodo.isAnnotationPresent(PutMapping.class)
          || metodo.isAnnotationPresent(DeleteMapping.class);
      // "aplicar" modelo só devolve o HTML preenchido; não grava nada.
      if (escrita && !"aplicarTemplate".equals(metodo.getName())
          && preAuthorize != null && preAuthorize.value().equals(Permissoes.PAGINA_LER)) {
        suspeitos.add(metodo.getName());
      }
    }

    assertThat(suspeitos).as("endpoints de escrita exigindo apenas PAGINA:LER").isEmpty();
  }

  /**
   * Decidir sobre a revisão não pode exigir poder de edição: o grupo REVISOR
   * tem PAGINA:APROVAR e nenhuma permissão de escrita de conteúdo.
   */
  @Test
  void decisoesDaRevisaoUsamPaginaAprovar() {
    List<String> endpointsDaRevisao = List.of("aprovar", "salvarRascunho", "atribuirRevisor",
        "comentarRevisao");
    List<String> divergentes = new ArrayList<>();

    for (Method metodo : PaginaController.class.getDeclaredMethods()) {
      if (!endpointsDaRevisao.contains(metodo.getName())) {
        continue;
      }
      PreAuthorize preAuthorize = metodo.getAnnotation(PreAuthorize.class);
      if (preAuthorize == null || !preAuthorize.value().equals(Permissoes.PAGINA_APROVAR)) {
        divergentes.add(metodo.getName() + " -> "
            + (preAuthorize == null ? "sem @PreAuthorize" : preAuthorize.value()));
      }
    }

    assertThat(divergentes).as("endpoints de decisão editorial fora de PAGINA_APROVAR").isEmpty();
  }

  @Test
  void publicarEArquivarContinuamExigindoEdicao() {
    for (Method metodo : PaginaController.class.getDeclaredMethods()) {
      if (!List.of("publicar", "arquivar", "enviarRevisao").contains(metodo.getName())) {
        continue;
      }
      PreAuthorize preAuthorize = metodo.getAnnotation(PreAuthorize.class);
      assertThat(preAuthorize).isNotNull();
      assertThat(preAuthorize.value())
          .as("%s deve continuar sob PAGINA:EDITAR", metodo.getName())
          .isEqualTo(Permissoes.PAGINA_EDITAR);
    }
  }

  private static Set<String> catalogoDePermissoes() throws Exception {
    Set<String> valores = new java.util.HashSet<>();
    for (var campo : Permissoes.class.getDeclaredFields()) {
      if (campo.getType() == String.class) {
        valores.add((String) campo.get(null));
      }
    }
    return valores;
  }

  private static boolean ehEndpoint(Method metodo) {
    return metodo.isAnnotationPresent(GetMapping.class)
        || metodo.isAnnotationPresent(PostMapping.class)
        || metodo.isAnnotationPresent(PutMapping.class)
        || metodo.isAnnotationPresent(PatchMapping.class)
        || metodo.isAnnotationPresent(DeleteMapping.class)
        || metodo.isAnnotationPresent(RequestMapping.class);
  }
}
