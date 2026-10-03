package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.dto.response.AiPerguntaResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AiPayloadJson {

  private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

  private AiPayloadJson() {}

  static String perguntas(ObjectMapper mapper, List<AiPerguntaResponse> perguntas, Map<String, String> contexto) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("perguntas", perguntas);
      payload.put("contexto", contexto);
      return mapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao serializar payload AI", ex);
    }
  }

  static String respostas(ObjectMapper mapper, Map<String, String> respostas) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("respostas", respostas == null ? Map.of() : respostas);
      return mapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao serializar respostas AI", ex);
    }
  }

  /** Pedido de ajuste do autor antes de regenerar ("mais curto", "foque na exportação"…). */
  static String instrucao(ObjectMapper mapper, String instrucao) {
    try {
      return mapper.writeValueAsString(Map.of("instrucao", instrucao));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao serializar instrução AI", ex);
    }
  }

  static String lerInstrucao(ObjectMapper mapper, String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      Object raw = mapper.readValue(json, MAP).get("instrucao");
      return raw instanceof String texto && !texto.isBlank() ? texto : null;
    } catch (Exception ex) {
      return null;
    }
  }

  static Map<String, String> lerContexto(ObjectMapper mapper, String json) {
    return lerMapa(mapper, json, "contexto");
  }

  @SuppressWarnings("unchecked")
  static List<AiPerguntaResponse> lerPerguntas(ObjectMapper mapper, String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      Map<String, Object> map = mapper.readValue(json, MAP);
      Object raw = map.get("perguntas");
      if (raw == null) {
        return List.of();
      }
      return mapper.convertValue(raw, new TypeReference<List<AiPerguntaResponse>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  static Map<String, String> lerRespostas(ObjectMapper mapper, String json) {
    return lerMapa(mapper, json, "respostas");
  }

  private static Map<String, String> lerMapa(ObjectMapper mapper, String json, String chave) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      Map<String, Object> map = mapper.readValue(json, MAP);
      Object raw = map.get(chave);
      if (!(raw instanceof Map<?, ?> valores)) {
        return Map.of();
      }
      Map<String, String> out = new LinkedHashMap<>();
      valores.forEach((k, v) -> {
        if (k != null && v != null) {
          out.put(String.valueOf(k), String.valueOf(v));
        }
      });
      return out;
    } catch (Exception ex) {
      return Map.of();
    }
  }
}
