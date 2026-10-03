package com.nexus.portal.ai.provider;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider determinístico para dev/test quando {@code nexus.ai} está desligado ou sem API key.
 */
public final class FakeLlmProvider implements LlmProvider {

  public static final String ID = "fake";

  private static final Pattern CAMPO = Pattern.compile("(?im)^(?:titulo|código tela|codigoTela|resumo)\\s*[:=]\\s*(.+)$");
  @Override
  public String id() {
    return ID;
  }

  @Override
  public LlmCompletion completar(String systemPrompt, String userPrompt) {
    String prompt = (systemPrompt == null ? "" : systemPrompt) + "\n" + (userPrompt == null ? "" : userPrompt);
    String content;
    if (prompt.contains("GERAR_PAGE_SPEC")) {
      content = gerarPageSpec(userPrompt == null ? "" : userPrompt);
    } else {
      content = """
          {
            "tipoResposta": "PERGUNTAS",
            "mensagem": "Provider fake ativo. Configure NEXUS_AI_API_KEY para geração real.",
            "perguntas": [
              {
                "id": "codigoTela",
                "texto": "Qual o código da tela (codigoTela)?",
                "obrigatoria": true
              }
            ]
          }
          """;
    }
    int tokens = Math.max(1, content.length() / 4);
    return LlmCompletion.of(content, tokens / 2, tokens / 2);
  }

  private String gerarPageSpec(String userPrompt) {
    String titulo = extrair(userPrompt, "tituloSugerido", "Página gerada pela IA");
    String codigo = extrair(userPrompt, "codigoTelaSugerido", "AI-DEMO");
    String resumo = extrair(userPrompt, "resumoSugerido",
        "Guia gerado automaticamente a partir do briefing informado no assistente Nexus AI.");
    List<String> componentes = new ArrayList<>();
    Matcher matcher = Pattern.compile("(?m)^-\\s+([a-z0-9-]+)\\s+\\|").matcher(userPrompt);
    while (matcher.find() && componentes.size() < 12) {
      componentes.add(matcher.group(1));
    }
    String blocos = componentes.stream()
        .map(id -> "{\"componenteId\":" + json(id) + ",\"textos\":[]}")
        .reduce((a, b) -> a + "," + b)
        .orElse("");
    return """
        {
          "titulo": %s,
          "slug": %s,
          "codigoTela": %s,
          "resumo": %s,
          "blocos": [%s]
        }
        """.formatted(
        json(titulo),
        json(slugify(titulo)),
        json(codigo),
        json(resumo),
        blocos);
  }

  private static String extrair(String texto, String chave, String padrao) {
    Matcher matcher = Pattern.compile("(?im)^" + Pattern.quote(chave) + "\\s*[:=]\\s*(.+)$").matcher(texto);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    if ("titulo".equals(chave)) {
      Matcher generico = CAMPO.matcher(texto);
      while (generico.find()) {
        if (generico.group(0).toLowerCase(Locale.ROOT).startsWith("titulo")) {
          return generico.group(1).trim();
        }
      }
    }
    return padrao;
  }

  private static String slugify(String value) {
    String slug = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    return slug.isBlank() ? "pagina-ai" : slug;
  }

  private static String json(String value) {
    String escaped = value == null ? "" : value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "");
    return "\"" + escaped + "\"";
  }
}
