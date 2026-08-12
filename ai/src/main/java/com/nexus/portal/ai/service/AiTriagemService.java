package com.nexus.portal.ai.service;

import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.response.AiPerguntaResponse;
import com.nexus.portal.ai.entity.AiObjetivo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Triagem determinística (heurística) do briefing + respostas.
 * LLM entra depois como enriquecimento opcional; S1 não depende de rede.
 */
@Service
public class AiTriagemService {

  private static final Pattern CODIGO_EXPLICITO = Pattern.compile(
      "(?i)(?:codigo\\s*tela|código\\s*tela|codigoTela)\\s*[=:]\\s*([A-Z0-9][A-Z0-9._-]{1,40})");
  private static final Pattern CODIGO_TOKEN = Pattern.compile("\\b([A-Z]{2,}(?:[-_][A-Z0-9]{2,})+)\\b");
  private static final Pattern TITULO_EXPLICITO = Pattern.compile(
      "(?i)(?:titulo|título)\\s*[=:]\\s*(.+)");

  private final AiProperties properties;

  public AiTriagemService(AiProperties properties) {
    this.properties = properties;
  }

  public ResultadoTriagem avaliar(AiObjetivo objetivo, String briefing, Map<String, String> respostas) {
    Map<String, String> ctx = new LinkedHashMap<>();
    if (respostas != null) {
      respostas.forEach((k, v) -> {
        if (k != null && v != null && !v.isBlank()) {
          ctx.put(k, v.trim());
        }
      });
    }

    String texto = briefing == null ? "" : briefing.trim();
    extrairDoBriefing(texto, ctx);

    List<AiPerguntaResponse> perguntas = new ArrayList<>();

    if (!ctx.containsKey("titulo")) {
      perguntas.add(new AiPerguntaResponse(
          "titulo",
          "Qual o título da página do manual?",
          List.of(),
          true));
    }
    if (!ctx.containsKey("codigoTela")) {
      perguntas.add(new AiPerguntaResponse(
          "codigoTela",
          "Qual o código da tela (codigoTela)?",
          List.of(),
          true));
    }
    if (!ctx.containsKey("publico")) {
      perguntas.add(new AiPerguntaResponse(
          "publico",
          "Quem é o público desta tela?",
          List.of("Operador", "Gestor", "Ambos"),
          false));
    }
    if (objetivo == AiObjetivo.ATUALIZAR_PAGINA && !ctx.containsKey("escopoAlteracao")) {
      perguntas.add(new AiPerguntaResponse(
          "escopoAlteracao",
          "O que mudou e deve ser refletido no manual?",
          List.of(),
          true));
    }
    if (texto.length() < 120 && !ctx.containsKey("fluxo")) {
      perguntas.add(new AiPerguntaResponse(
          "fluxo",
          "Descreva o fluxo principal (passos do usuário) nesta tela.",
          List.of(),
          true));
    }

    int max = properties.maxPerguntas();
    if (perguntas.size() > max) {
      perguntas = new ArrayList<>(perguntas.subList(0, max));
    }

    boolean completa = perguntas.stream().noneMatch(AiPerguntaResponse::obrigatoria);
    String mensagem = completa
        ? "Contexto suficiente. Sessão pronta para gerar o rascunho (S2)."
        : "Para montar o guia, preciso de alguns detalhes:";

    return new ResultadoTriagem(completa, mensagem, perguntas, Map.copyOf(ctx));
  }

  private void extrairDoBriefing(String texto, Map<String, String> ctx) {
    Matcher codigoExplicito = CODIGO_EXPLICITO.matcher(texto);
    if (codigoExplicito.find()) {
      ctx.putIfAbsent("codigoTela", codigoExplicito.group(1).toUpperCase(Locale.ROOT));
    } else {
      Matcher token = CODIGO_TOKEN.matcher(texto);
      if (token.find()) {
        ctx.putIfAbsent("codigoTela", token.group(1).toUpperCase(Locale.ROOT));
      }
    }

    Matcher tituloExplicito = TITULO_EXPLICITO.matcher(texto);
    if (tituloExplicito.find()) {
      ctx.putIfAbsent("titulo", tituloExplicito.group(1).trim());
    } else {
      String primeiraLinha = texto.lines()
          .map(String::trim)
          .filter(linha -> !linha.isBlank())
          .findFirst()
          .orElse("")
          .replaceFirst("^#{1,6}\\s+", "")
          .trim();
      if (primeiraLinha.length() >= 8 && primeiraLinha.length() <= 120 && !primeiraLinha.contains(":")) {
        ctx.putIfAbsent("titulo", primeiraLinha);
      }
    }

    String lower = texto.toLowerCase(Locale.ROOT);
    if (lower.contains("gestor") || lower.contains("gerente")) {
      ctx.putIfAbsent("publico", "Gestor");
    } else if (lower.contains("operador") || lower.contains("usuário final") || lower.contains("usuario final")) {
      ctx.putIfAbsent("publico", "Operador");
    }
  }

  public record ResultadoTriagem(
      boolean completa,
      String mensagem,
      List<AiPerguntaResponse> perguntas,
      Map<String, String> contextoExtraido) {
  }
}
