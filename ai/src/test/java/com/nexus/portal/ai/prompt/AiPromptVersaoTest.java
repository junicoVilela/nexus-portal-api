package com.nexus.portal.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.ai.prompt.AiPromptCatalogo.Prompt;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Prompt ops (AI-703): o texto de um prompt só muda junto com a {@code versao}. Sem isso, duas
 * propostas com o mesmo {@code prompt_versao} poderiam vir de textos diferentes e a comparação
 * por versão no painel Qualidade da IA perderia o sentido.
 *
 * <p>Compara cada arquivo de {@code resources/prompts} com {@code prompts-versoes.lock}.
 */
class AiPromptVersaoTest {

  private static final Path PROMPTS = Path.of("src/main/resources/prompts");

  @Test
  void textoSoMudaJuntoComAVersao() throws IOException {
    Map<String, String[]> trava = lerTrava();
    List<String> problemas = new ArrayList<>();
    for (String nome : nomes()) {
      Prompt prompt = AiPromptCatalogo.carregar(nome);
      String linha = nome + " " + prompt.versao() + " " + impressao(prompt.texto());
      String[] registrado = trava.remove(nome);
      if (registrado == null) {
        problemas.add("prompt novo sem registro — adicione: " + linha);
      } else if (registrado[0].equals(String.valueOf(prompt.versao()))
          && !registrado[1].equals(impressao(prompt.texto()))) {
        problemas.add(nome + ": texto mudou sem subir a versao (" + prompt.versao()
            + "). Suba `versao` no .md e atualize a trava.");
      } else if (!registrado[0].equals(String.valueOf(prompt.versao()))) {
        problemas.add(nome + ": versao mudou — atualize a trava para: " + linha);
      }
    }
    trava.keySet().forEach(nome -> problemas.add("trava cita prompt que não existe mais: " + nome));
    assertThat(problemas).as("prompts-versoes.lock").isEmpty();
  }

  private static List<String> nomes() throws IOException {
    try (Stream<Path> arquivos = Files.list(PROMPTS)) {
      return arquivos
          .map(p -> p.getFileName().toString())
          .filter(n -> n.endsWith(".md") && !n.equals("README.md"))
          .map(n -> n.substring(0, n.length() - ".md".length()))
          .sorted()
          .toList();
    }
  }

  private static Map<String, String[]> lerTrava() throws IOException {
    try (InputStream in = AiPromptVersaoTest.class.getResourceAsStream("/prompts-versoes.lock")) {
      assertThat(in).as("src/test/resources/prompts-versoes.lock").isNotNull();
      Map<String, String[]> trava = new TreeMap<>();
      new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
          .map(String::strip)
          .filter(l -> !l.isEmpty() && !l.startsWith("#"))
          .map(l -> l.split("\\s+"))
          .forEach(p -> trava.put(p[0], new String[] {p[1], p[2]}));
      return trava;
    }
  }

  static String impressao(String texto) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, 16);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
