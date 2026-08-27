package com.nexus.portal.releaseorchestrator.service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Casa o nome do artefato arquivado no Jenkins com o {@code padraoAsset} do
 * módulo (glob simples) e decide o nome de destino em {@code artifacts/}.
 */
public final class ArtefatoBuildMatcher {

  private ArtefatoBuildMatcher() {}

  public static boolean casa(String fileName, String padraoAsset) {
    if (fileName == null || fileName.isBlank()) {
      return false;
    }
    if (ehPlainJar(fileName)) {
      return false;
    }
    String glob = padraoAsset == null || padraoAsset.isBlank() ? "*.war,*.jar,*.ear,*.zip" : padraoAsset;
    String nome = nomeArquivo(fileName);
    for (String parte : glob.split(",")) {
      if (casaUm(nome, parte.trim())) {
        return true;
      }
    }
    return false;
  }

  public static boolean ehPlainJar(String fileName) {
    String nome = nomeArquivo(fileName).toLowerCase(Locale.ROOT);
    return nome.endsWith("-plain.jar");
  }

  public static String destino(String fileName, String nomeArtefato) {
    if (nomeArtefato != null && !nomeArtefato.isBlank()) {
      return nomeArquivo(nomeArtefato.trim());
    }
    return nomeArquivo(fileName);
  }

  public static String nomeArquivo(String caminho) {
    if (caminho == null) {
      return "";
    }
    String n = caminho.replace('\\', '/');
    int slash = n.lastIndexOf('/');
    return slash < 0 ? n : n.substring(slash + 1);
  }

  static boolean casaUm(String fileName, String glob) {
    if (glob.isBlank()) {
      return false;
    }
    String g = glob.replace('\\', '/');
    int slash = g.lastIndexOf('/');
    if (slash >= 0) {
      g = g.substring(slash + 1);
    }
    g = g.replace("**/", "");
    String regex = globParaRegex(g);
    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(fileName).matches();
  }

  private static String globParaRegex(String glob) {
    StringBuilder sb = new StringBuilder("^");
    for (int i = 0; i < glob.length(); i++) {
      char c = glob.charAt(i);
      switch (c) {
        case '*' -> sb.append(".*");
        case '?' -> sb.append('.');
        case '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|' -> sb.append('\\').append(c);
        default -> sb.append(c);
      }
    }
    return sb.append('$').toString();
  }
}
