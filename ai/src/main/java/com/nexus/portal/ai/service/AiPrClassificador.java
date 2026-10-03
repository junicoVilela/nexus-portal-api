package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiPrClassificacao;
import com.nexus.portal.ai.integration.github.AiGithubClient.ArquivoPr;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Decide, sem IA, se um PR mexe em tela e com qual código de tela (AI-605). Regras simples e
 * previsíveis: quem lê a fila precisa entender por que um PR entrou ou não.
 *
 * <ol>
 *   <li>rótulo de pular ({@code docs:skip}) → {@code IRRELEVANTE};</li>
 *   <li>só docs/testes/build → {@code IRRELEVANTE};</li>
 *   <li>nenhum arquivo de tela → {@code SO_BACKEND};</li>
 *   <li>algum arquivo de tela adicionado → {@code UI_NOVA}; senão → {@code UI_ALTERACAO}.</li>
 * </ol>
 */
public final class AiPrClassificador {

  /** {@code codigoTela: PED-001} (ou {@code tela:}) no corpo do PR tem prioridade. */
  private static final Pattern MARCADOR = Pattern.compile("(?im)^\\s*(?:codigo\\s*tela|código\\s*tela|codigoTela|tela)\\s*[:=]\\s*([A-Z][A-Z0-9-]{2,60})");
  /** Códigos no estilo {@code PED-001}, {@code DF-START}: maiúsculas e dígitos separados por hífen. */
  private static final Pattern CODIGO = Pattern.compile("\\b([A-Z][A-Z0-9]{1,15}(?:-[A-Z0-9]{1,15})+)\\b");
  private static final List<String> SO_DOCUMENTACAO = List.of(
      "**/*.md", "**/*.spec.ts", "**/*.test.*", "**/test/**", "**/tests/**", "**/__tests__/**",
      "**/*.lock", "**/package-lock.json", "**/.github/**", "**/Dockerfile", "**/*.yml", "**/*.yaml");
  private static final Set<String> FALSOS_CODIGOS = Set.of("UTF-8", "ISO-8859-1", "HTTP-200", "SHA-256");

  private AiPrClassificador() {}

  public record Resultado(
      AiPrClassificacao classificacao,
      /** Candidatos para achar a página existente, na ordem de confiança (marcador primeiro). */
      List<String> codigosTela,
      /** Código para página nova: marcador explícito ou código com dígito; nulo se não houver. */
      String codigoSugerido,
      List<String> arquivosTela,
      String motivo) {}

  public static Resultado classificar(
      String titulo,
      String corpo,
      List<String> rotulos,
      List<ArquivoPr> arquivos,
      List<String> caminhosTela,
      String rotuloIgnorar) {
    List<String> codigos = codigosTela(titulo, corpo);
    String sugerido = codigoSugerido(titulo, corpo);
    if (rotulos.stream().anyMatch(r -> r.equalsIgnoreCase(rotuloIgnorar))) {
      return new Resultado(AiPrClassificacao.IRRELEVANTE, codigos, sugerido, List.of(),
          "PR marcado com o rótulo " + rotuloIgnorar + ".");
    }
    List<PathMatcher> tela = matchers(caminhosTela);
    List<PathMatcher> documentacao = matchers(SO_DOCUMENTACAO);
    List<ArquivoPr> relevantes = arquivos.stream().filter(a -> !casa(documentacao, a.caminho())).toList();
    if (relevantes.isEmpty()) {
      return new Resultado(AiPrClassificacao.IRRELEVANTE, codigos, sugerido, List.of(),
          "Só documentação, testes ou configuração de build.");
    }
    List<ArquivoPr> deTela = relevantes.stream().filter(a -> casa(tela, a.caminho())).toList();
    List<String> caminhos = deTela.stream().map(ArquivoPr::caminho).toList();
    if (deTela.isEmpty()) {
      return new Resultado(AiPrClassificacao.SO_BACKEND, codigos, sugerido, List.of(),
          relevantes.size() + " arquivo(s) alterado(s), nenhum de tela.");
    }
    boolean telaNova = deTela.stream().anyMatch(a -> "added".equals(a.status()));
    return new Resultado(
        telaNova ? AiPrClassificacao.UI_NOVA : AiPrClassificacao.UI_ALTERACAO,
        codigos,
        sugerido,
        caminhos,
        deTela.size() + " arquivo(s) de tela " + (telaNova ? "(com arquivos novos)." : "alterado(s)."));
  }

  /** Marcador explícito, depois códigos com dígito, depois os demais ({@code DF-START}). */
  static List<String> codigosTela(String titulo, String corpo) {
    Set<String> codigos = new LinkedHashSet<>(marcados(texto(titulo, corpo)));
    List<String> soltos = soltos(texto(titulo, corpo));
    soltos.stream().filter(AiPrClassificador::temDigito).forEach(codigos::add);
    codigos.addAll(soltos);
    return List.copyOf(codigos);
  }

  /** Sem dígito, "ABC-DEF" pode ser qualquer sigla: só serve para achar página que já existe. */
  static String codigoSugerido(String titulo, String corpo) {
    String texto = texto(titulo, corpo);
    return marcados(texto).stream().findFirst()
        .or(() -> soltos(texto).stream().filter(AiPrClassificador::temDigito).findFirst())
        .orElse(null);
  }

  private static List<String> marcados(String texto) {
    List<String> codigos = new ArrayList<>();
    Matcher marcador = MARCADOR.matcher(texto);
    while (marcador.find()) {
      codigos.add(marcador.group(1).toUpperCase(Locale.ROOT));
    }
    return codigos;
  }

  private static List<String> soltos(String texto) {
    List<String> codigos = new ArrayList<>();
    Matcher codigo = CODIGO.matcher(texto);
    while (codigo.find()) {
      if (!FALSOS_CODIGOS.contains(codigo.group(1))) {
        codigos.add(codigo.group(1));
      }
    }
    return codigos;
  }

  private static boolean temDigito(String codigo) {
    return codigo.chars().anyMatch(Character::isDigit);
  }

  private static String texto(String titulo, String corpo) {
    return (titulo == null ? "" : titulo) + "\n" + (corpo == null ? "" : corpo);
  }

  private static List<PathMatcher> matchers(List<String> globs) {
    return globs.stream()
        .flatMap(glob -> glob.startsWith("**/")
            // "**/x" não casa "x" na raiz; o par cobre os dois casos.
            ? Stream.of(glob, glob.substring(3))
            : Stream.of(glob))
        .map(glob -> FileSystems.getDefault().getPathMatcher("glob:" + glob))
        .toList();
  }

  private static boolean casa(List<PathMatcher> matchers, String caminho) {
    Path path = Path.of(caminho);
    return matchers.stream().anyMatch(m -> m.matches(path));
  }
}
