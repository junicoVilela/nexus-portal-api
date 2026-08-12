package com.nexus.portal.docflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintSecaoResponse;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Catálogo canônico de receitas editoriais.
 *
 * <p>Blueprints guardam apenas composição e regras. O HTML continua pertencendo aos componentes
 * validados por {@link PaginaBlocoCatalogoService}.
 */
@Service
public class PaginaBlueprintCatalogoService {

  private static final String RECURSO = "docflow/pagina-blueprints.json";
  private static final Set<String> NECESSIDADES =
      Set.of("OBRIGATORIA", "RECOMENDADA", "OPCIONAL");

  private final List<PaginaBlueprintResponse> blueprints;
  private final Map<String, PaginaBlueprintResponse> blueprintsPorId;
  private final Map<String, PaginaBlueprintResponse> blueprintsPorTemplate;

  public PaginaBlueprintCatalogoService(
      ObjectMapper objectMapper, PaginaBlocoCatalogoService blocoCatalogoService) {
    Set<String> componentes = blocoCatalogoService.listar().stream()
        .map(bloco -> bloco.id())
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    this.blueprints = carregar(objectMapper, componentes);

    Map<String, PaginaBlueprintResponse> porId = new LinkedHashMap<>();
    Map<String, PaginaBlueprintResponse> porTemplate = new LinkedHashMap<>();
    for (PaginaBlueprintResponse blueprint : blueprints) {
      if (porId.putIfAbsent(blueprint.id(), blueprint) != null) {
        throw new IllegalStateException("Blueprint duplicado no catálogo: " + blueprint.id());
      }
      for (String template : blueprint.templatesCompativeis()) {
        String codigo = normalizarCodigo(template);
        if (porTemplate.putIfAbsent(codigo, blueprint) != null) {
          throw new IllegalStateException(
              "Template associado a mais de um blueprint: " + codigo);
        }
      }
    }
    this.blueprintsPorId = Map.copyOf(porId);
    this.blueprintsPorTemplate = Map.copyOf(porTemplate);
  }

  public List<PaginaBlueprintResponse> listar() {
    return blueprints;
  }

  public PaginaBlueprintResponse buscar(String id) {
    PaginaBlueprintResponse blueprint = blueprintsPorId.get(id);
    if (blueprint == null) {
      throw new BusinessException("Blueprint de página não encontrado no catálogo: " + id);
    }
    return blueprint;
  }

  public Optional<PaginaBlueprintResponse> buscarPorTemplate(String templateCodigo) {
    return Optional.ofNullable(blueprintsPorTemplate.get(normalizarCodigo(templateCodigo)));
  }

  private static List<PaginaBlueprintResponse> carregar(
      ObjectMapper objectMapper, Set<String> componentes) {
    try (InputStream input = new ClassPathResource(RECURSO).getInputStream()) {
      List<PaginaBlueprintRecurso> recursos = objectMapper.readValue(
          input, new TypeReference<List<PaginaBlueprintRecurso>>() {});
      if (recursos.isEmpty()) {
        throw new IllegalStateException("Catálogo de blueprints vazio: " + RECURSO);
      }
      return recursos.stream()
          .map(recurso -> mapear(recurso, componentes))
          .toList();
    } catch (Exception ex) {
      throw new IllegalStateException("Falha ao carregar catálogo de blueprints " + RECURSO, ex);
    }
  }

  private static PaginaBlueprintResponse mapear(
      PaginaBlueprintRecurso recurso, Set<String> componentes) {
    if (recurso.id() == null || recurso.id().isBlank()
        || recurso.nome() == null || recurso.nome().isBlank()
        || recurso.tipoConteudo() == null || recurso.tipoConteudo().isBlank()
        || recurso.versao() < 1
        || recurso.minimoComponentes() < 1
        || recurso.maximoComponentes() < recurso.minimoComponentes()
        || recurso.secoes() == null || recurso.secoes().isEmpty()) {
      throw new IllegalStateException("Blueprint inválido no catálogo: " + recurso);
    }

    Set<String> slots = new LinkedHashSet<>();
    List<PaginaBlueprintSecaoResponse> secoes = recurso.secoes().stream()
        .map(secao -> mapearSecao(recurso.id(), secao, componentes, slots))
        .toList();
    long obrigatorias = secoes.stream()
        .filter(secao -> "OBRIGATORIA".equals(secao.necessidade()))
        .count();
    if (obrigatorias > recurso.maximoComponentes()) {
      throw new IllegalStateException(
          "Blueprint possui mais seções obrigatórias que o máximo: " + recurso.id());
    }

    return new PaginaBlueprintResponse(
        recurso.id(),
        recurso.nome(),
        recurso.descricao(),
        recurso.tipoConteudo(),
        recurso.versao(),
        recurso.status() == null || recurso.status().isBlank() ? "PUBLICADO" : recurso.status(),
        recurso.minimoComponentes(),
        recurso.maximoComponentes(),
        copiar(recurso.templatesCompativeis()),
        secoes);
  }

  private static PaginaBlueprintSecaoResponse mapearSecao(
      String blueprintId,
      PaginaBlueprintSecaoRecurso secao,
      Set<String> componentes,
      Set<String> slots) {
    String necessidade = normalizarCodigo(secao.necessidade());
    if (secao.slot() == null || secao.slot().isBlank() || !slots.add(secao.slot())
        || secao.componenteId() == null || !componentes.contains(secao.componenteId())
        || !NECESSIDADES.contains(necessidade)
        || secao.maximoInstancias() < 1) {
      throw new IllegalStateException(
          "Seção inválida no blueprint " + blueprintId + ": " + secao);
    }
    List<String> alternativas = copiar(secao.alternativas());
    if (!componentes.containsAll(alternativas)) {
      throw new IllegalStateException(
          "Blueprint referencia componente alternativo inexistente: " + blueprintId);
    }
    return new PaginaBlueprintSecaoResponse(
        secao.slot(),
        secao.componenteId(),
        necessidade,
        secao.repetivel(),
        secao.maximoInstancias(),
        alternativas);
  }

  private static List<String> copiar(List<String> valores) {
    return valores == null ? List.of() : List.copyOf(valores);
  }

  private static String normalizarCodigo(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private record PaginaBlueprintRecurso(
      String id,
      String nome,
      String descricao,
      String tipoConteudo,
      int versao,
      String status,
      int minimoComponentes,
      int maximoComponentes,
      List<String> templatesCompativeis,
      List<PaginaBlueprintSecaoRecurso> secoes) {
  }

  private record PaginaBlueprintSecaoRecurso(
      String slot,
      String componenteId,
      String necessidade,
      boolean repetivel,
      int maximoInstancias,
      List<String> alternativas) {
  }
}
