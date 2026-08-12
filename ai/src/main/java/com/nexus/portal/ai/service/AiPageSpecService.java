package com.nexus.portal.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlueprintResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Valida a saída estruturada da IA e renderiza somente componentes do catálogo confiável. */
@Service
public class AiPageSpecService {

  private static final int MAX_BLOCOS = 12;

  private final ObjectMapper objectMapper;
  private final DocFlowAiBridge docFlowAiBridge;

  public AiPageSpecService(ObjectMapper objectMapper, DocFlowAiBridge docFlowAiBridge) {
    this.objectMapper = objectMapper;
    this.docFlowAiBridge = docFlowAiBridge;
  }

  public JsonNode schema(List<PaginaBlocoResponse> candidatos) {
    ObjectNode texto = objectMapper.createObjectNode();
    texto.put("type", "object");
    texto.set("properties", objectMapper.createObjectNode()
        .set("slotId", tipo("string")));
    ((ObjectNode) texto.path("properties")).set("valor", tipo("string"));
    texto.set("required", array("slotId", "valor"));
    texto.put("additionalProperties", false);

    ObjectNode bloco = objectMapper.createObjectNode();
    bloco.put("type", "object");
    ObjectNode propriedadesBloco = objectMapper.createObjectNode();
    ObjectNode componenteId = tipo("string");
    ArrayNode ids = objectMapper.createArrayNode();
    candidatos.forEach(candidato -> ids.add(candidato.id()));
    componenteId.set("enum", ids);
    propriedadesBloco.set("componenteId", componenteId);
    ObjectNode textos = tipo("array");
    textos.set("items", texto);
    propriedadesBloco.set("textos", textos);
    bloco.set("properties", propriedadesBloco);
    bloco.set("required", array("componenteId", "textos"));
    bloco.put("additionalProperties", false);

    ObjectNode raiz = objectMapper.createObjectNode();
    raiz.put("type", "object");
    ObjectNode propriedades = objectMapper.createObjectNode();
    for (String campo : List.of("titulo", "slug", "codigoTela", "resumo")) {
      propriedades.set(campo, tipo("string"));
    }
    ObjectNode blocos = tipo("array");
    blocos.put("minItems", 1);
    blocos.put("maxItems", MAX_BLOCOS);
    blocos.set("items", bloco);
    propriedades.set("blocos", blocos);
    raiz.set("properties", propriedades);
    raiz.set("required", array("titulo", "slug", "codigoTela", "resumo", "blocos"));
    raiz.put("additionalProperties", false);
    return raiz;
  }

  public AiPageSpec interpretar(JsonNode json, List<PaginaBlocoResponse> candidatos) {
    return interpretar(json, candidatos, null);
  }

  public AiPageSpec interpretar(
      JsonNode json,
      List<PaginaBlocoResponse> candidatos,
      PaginaBlueprintResponse blueprint) {
    Map<String, PaginaBlocoResponse> permitidos = candidatos.stream()
        .collect(java.util.stream.Collectors.toMap(PaginaBlocoResponse::id, bloco -> bloco));
    JsonNode blocosNode = json.path("blocos");
    if (!blocosNode.isArray() || blocosNode.isEmpty() || blocosNode.size() > MAX_BLOCOS) {
      throw new IllegalArgumentException("PageSpec deve conter de 1 a " + MAX_BLOCOS + " blocos.");
    }

    List<AiPageSpec.Bloco> blocos = new ArrayList<>();
    Set<String> usados = new LinkedHashSet<>();
    for (JsonNode blocoNode : blocosNode) {
      String componenteId = blocoNode.path("componenteId").asText("").trim();
      PaginaBlocoResponse componente = permitidos.get(componenteId);
      if (componente == null) {
        throw new IllegalArgumentException("Componente não permitido na PageSpec: " + componenteId);
      }
      if (!usados.add(componenteId)) {
        continue;
      }
      Set<String> slots = componente.slots().stream()
          .map(slot -> slot.id())
          .collect(java.util.stream.Collectors.toSet());
      List<AiPageSpec.Texto> textos = new ArrayList<>();
      JsonNode textosNode = blocoNode.path("textos");
      if (!textosNode.isArray()) {
        throw new IllegalArgumentException("Textos inválidos no componente " + componenteId);
      }
      for (JsonNode textoNode : textosNode) {
        String slotId = textoNode.path("slotId").asText("").trim();
        String valor = textoNode.path("valor").asText("").trim();
        if (!slots.contains(slotId)) {
          throw new IllegalArgumentException(
              "Slot não permitido no componente " + componenteId + ": " + slotId);
        }
        if (!valor.isBlank()) {
          textos.add(new AiPageSpec.Texto(slotId, valor));
        }
      }
      blocos.add(new AiPageSpec.Bloco(componenteId, List.copyOf(textos)));
    }
    if (blocos.isEmpty()) {
      throw new IllegalArgumentException("PageSpec não possui componentes utilizáveis.");
    }
    return new AiPageSpec(
        2,
        blueprint == null ? null : blueprint.id(),
        textoObrigatorio(json, "titulo"),
        textoObrigatorio(json, "slug"),
        textoObrigatorio(json, "codigoTela"),
        textoObrigatorio(json, "resumo"),
        List.copyOf(blocos));
  }

  public String renderizar(AiPageSpec spec) {
    StringBuilder html = new StringBuilder();
    for (AiPageSpec.Bloco bloco : spec.blocos()) {
      Map<String, String> textos = new LinkedHashMap<>();
      bloco.textos().forEach(item -> textos.putIfAbsent(item.slotId(), item.valor()));
      if (!html.isEmpty()) {
        html.append('\n');
      }
      html.append(docFlowAiBridge.renderizarBloco(bloco.componenteId(), textos));
    }
    return html.toString();
  }

  public AiPageSpec fallback(
      String titulo,
      String slug,
      String codigoTela,
      String resumo,
      List<PaginaBlocoResponse> candidatos) {
    return fallback(titulo, slug, codigoTela, resumo, candidatos, null);
  }

  public AiPageSpec fallback(
      String titulo,
      String slug,
      String codigoTela,
      String resumo,
      List<PaginaBlocoResponse> candidatos,
      PaginaBlueprintResponse blueprint) {
    List<AiPageSpec.Bloco> blocos = candidatos.stream()
        .limit(6)
        .map(bloco -> new AiPageSpec.Bloco(
            bloco.id(), textosFallback(bloco, titulo, codigoTela, resumo)))
        .toList();
    if (blocos.isEmpty()) {
      throw new IllegalStateException("Catálogo não forneceu componentes para a página.");
    }
    return new AiPageSpec(
        2,
        blueprint == null ? null : blueprint.id(),
        titulo,
        slug,
        codigoTela,
        resumo,
        blocos);
  }

  private static List<AiPageSpec.Texto> textosFallback(
      PaginaBlocoResponse bloco, String titulo, String codigoTela, String resumo) {
    List<AiPageSpec.Texto> textos = new ArrayList<>();
    boolean corpoPreenchido = false;
    for (var slot : bloco.slots()) {
      String valor = null;
      if ("introducao".equals(bloco.id()) && ("h1".equals(slot.elemento())
          || "h2".equals(slot.elemento()))) {
        valor = titulo;
      } else if (!corpoPreenchido && "p".equals(slot.elemento())
          && slot.textoPadrao().length() >= 30) {
        valor = resumo;
        corpoPreenchido = true;
      } else if (slot.textoPadrao().contains("T-000")) {
        valor = slot.textoPadrao().replace("T-000", codigoTela);
      }
      if (valor != null && !valor.isBlank()) {
        textos.add(new AiPageSpec.Texto(slot.id(), valor));
      }
    }
    return List.copyOf(textos);
  }

  private ObjectNode tipo(String tipo) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("type", tipo);
    return node;
  }

  private ArrayNode array(String... valores) {
    ArrayNode array = objectMapper.createArrayNode();
    for (String valor : valores) {
      array.add(valor);
    }
    return array;
  }

  private static String textoObrigatorio(JsonNode node, String campo) {
    String valor = node.path(campo).asText("").trim();
    if (valor.isBlank()) {
      throw new IllegalArgumentException("Campo obrigatório ausente na PageSpec: " + campo);
    }
    return valor;
  }
}
