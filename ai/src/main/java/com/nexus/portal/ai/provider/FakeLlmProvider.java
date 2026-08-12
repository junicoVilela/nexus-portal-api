package com.nexus.portal.ai.provider;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider determinístico para dev/test quando {@code nexus.ai} está desligado ou sem API key.
 * Quando há {@code esqueletoHtml} da biblioteca, devolve esse HTML (não inventa layout).
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
    } else if (prompt.contains("GERAR_RASCUNHO") || prompt.contains("conteudoHtml")) {
      content = gerarRascunho(userPrompt == null ? "" : userPrompt);
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

  private String gerarRascunho(String userPrompt) {
    String titulo = extrair(userPrompt, "titulo", "Página gerada pela IA");
    String codigo = extrair(userPrompt, "codigoTela", "AI-DEMO");
    String resumo = extrair(userPrompt, "resumo",
        "Guia gerado automaticamente a partir do briefing informado no assistente Nexus AI.");
    String slug = slugify(titulo);
    String esqueleto = extrairEsqueleto(userPrompt);
    String html = esqueleto != null && !esqueleto.isBlank()
        ? esqueleto.trim()
        : htmlFallback(titulo, codigo, resumo);

    return """
        {
          "titulo": %s,
          "slug": %s,
          "codigoTela": %s,
          "resumo": %s,
          "conteudoHtml": %s
        }
        """.formatted(json(titulo), json(slug), json(codigo), json(resumo), json(html));
  }

  private static String extrairEsqueleto(String userPrompt) {
    if (userPrompt == null || userPrompt.isBlank()) {
      return "";
    }
    String marker = "esqueletoHtml:";
    String end = "INSTRUCAO_FINAL:";
    int start = userPrompt.indexOf(marker);
    if (start < 0) {
      return "";
    }
    start += marker.length();
    int stop = userPrompt.indexOf(end, start);
    String body = (stop < 0 ? userPrompt.substring(start) : userPrompt.substring(start, stop)).trim();
    // Evita tratar texto de instrução como HTML quando o esqueleto veio vazio.
    if (body.isBlank() || body.startsWith("INSTRUCAO_FINAL") || !body.contains("<")) {
      return "";
    }
    return body;
  }

  private static String htmlFallback(String titulo, String codigo, String resumo) {
    return """
        <section class="doc-intro">
          <span class="doc-kicker">Guia da funcionalidade</span>
          <h2>Visão geral</h2>
          <p>%s</p>
        </section>
        <div class="objective-card">
          <p><strong>Objetivo</strong></p>
          <p>Orientar o usuário a utilizar a tela <strong>%s</strong> (%s) com segurança e clareza.</p>
        </div>
        <section class="doc-section">
          <h2>Visão da tela</h2>
          <figure class="screen-frame">
            <div class="screen-placeholder"><strong>Insira captura</strong><span>Substitua pela imagem real da tela.</span></div>
            <figcaption>Tela %s</figcaption>
          </figure>
        </section>
        <section class="doc-section">
          <h2>Passo a passo</h2>
          <ol class="steps">
            <li>Acesse a funcionalidade pelo menu do sistema.</li>
            <li>Confira os filtros e informações apresentadas.</li>
            <li>Execute a ação principal e valide o resultado.</li>
          </ol>
          <div class="checklist">
            <p><strong>Pré-requisitos</strong></p>
            <ul><li>Usuário autenticado com permissão de leitura da tela.</li></ul>
          </div>
          <div class="result-card"><strong>Resultado esperado</strong><p>A operação é concluída e o usuário confirma o resultado na tela.</p></div>
        </section>
        """.formatted(escape(resumo), escape(titulo), escape(codigo), escape(titulo));
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

  private static String escape(String value) {
    return value == null ? "" : value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
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
