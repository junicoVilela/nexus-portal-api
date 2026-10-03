package com.nexus.portal.ai.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompts versionados em {@code classpath:prompts/<nome>.md} — editar texto não exige mexer em Java.
 *
 * <p>Formato: cabeçalho {@code ---\nversao: N\n---} seguido do texto com placeholders
 * {@code {{variavel}}}. Ver {@code prompts/README.md}.
 */
public final class AiPromptCatalogo {

  private static final Pattern VERSAO = Pattern.compile("(?m)^versao:\\s*(\\d+)\\s*$");
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*}}");
  private static final Map<String, Prompt> CACHE = new ConcurrentHashMap<>();

  private AiPromptCatalogo() {}

  public static Prompt carregar(String nome) {
    return CACHE.computeIfAbsent(nome, AiPromptCatalogo::ler);
  }

  private static Prompt ler(String nome) {
    String caminho = "prompts/" + nome + ".md";
    try (InputStream in = AiPromptCatalogo.class.getClassLoader().getResourceAsStream(caminho)) {
      if (in == null) {
        throw new IllegalStateException("Prompt não encontrado: " + caminho);
      }
      String bruto = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
      int fimCabecalho = bruto.startsWith("---\n") ? bruto.indexOf("\n---\n", 3) : -1;
      Matcher versao = fimCabecalho < 0 ? null : VERSAO.matcher(bruto.substring(4, fimCabecalho + 1));
      if (versao == null || !versao.find()) {
        throw new IllegalStateException("Prompt " + caminho + " sem cabeçalho '---/versao: N/---'.");
      }
      String texto = bruto.substring(fimCabecalho + "\n---\n".length()).strip();
      return new Prompt(nome, Integer.parseInt(versao.group(1)), texto);
    } catch (IOException ex) {
      throw new UncheckedIOException("Falha ao ler " + caminho, ex);
    }
  }

  /** Prompt carregado; {@link #id()} ({@code nome@versao}) é o que fica gravado na proposta. */
  public record Prompt(String nome, int versao, String texto) {

    public String id() {
      return nome + "@" + versao;
    }

    public Set<String> variaveis() {
      Set<String> nomes = new LinkedHashSet<>();
      Matcher matcher = PLACEHOLDER.matcher(texto);
      while (matcher.find()) {
        nomes.add(matcher.group(1));
      }
      return nomes;
    }

    /**
     * Substitui os placeholders. Falha se faltar valor para algum deles ou se sobrar variável
     * que o texto não usa — os dois casos indicam prompt e código fora de sincronia.
     */
    public String renderizar(Map<String, String> valores) {
      Set<String> esperadas = variaveis();
      if (!esperadas.equals(valores.keySet())) {
        throw new IllegalStateException(
            "Prompt " + id() + " espera " + esperadas + " mas recebeu " + valores.keySet());
      }
      Matcher matcher = PLACEHOLDER.matcher(texto);
      StringBuilder out = new StringBuilder();
      while (matcher.find()) {
        String valor = valores.get(matcher.group(1));
        matcher.appendReplacement(out, Matcher.quoteReplacement(valor == null ? "" : valor));
      }
      matcher.appendTail(out);
      return out.toString();
    }
  }
}
