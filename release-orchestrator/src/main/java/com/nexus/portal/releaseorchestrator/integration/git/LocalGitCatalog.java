package com.nexus.portal.releaseorchestrator.integration.git;

import com.nexus.portal.releaseorchestrator.config.LocalGitProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Lista tags e branches {@code release/v*} de um clone local mapeado
 * em {@code release-orchestrator.git.local-clones}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalGitCatalog {

  private final LocalGitProperties properties;

  public boolean temClone(String repositorio) {
    return caminho(repositorio) != null;
  }

  public List<LocalGitRef> listarRefs(String repositorio) {
    Path dir = caminho(repositorio);
    if (dir == null) return List.of();
    LinkedHashSet<LocalGitRef> out = new LinkedHashSet<>();
    for (String tag : git(dir, "for-each-ref", "--sort=-creatordate",
        "--format=%(refname:short)", "refs/tags")) {
      if (!tag.isBlank()) out.add(new LocalGitRef(tag, false));
    }
    Set<String> branches = new LinkedHashSet<>();
    for (String raw : git(dir, "for-each-ref", "--sort=-creatordate",
        "--format=%(refname:short)", "refs/heads", "refs/remotes/origin")) {
      String nome = normalizarBranch(raw);
      if (nome != null) branches.add(nome);
    }
    for (String b : branches) {
      out.add(new LocalGitRef(b, true));
    }
    return new ArrayList<>(out);
  }

  private Path caminho(String repositorio) {
    if (repositorio == null || repositorio.isBlank()) return null;
    String key = repositorio.trim();
    String mapped = properties.localClones().stream()
        .filter(c -> c.repositorio() != null && c.repositorio().equalsIgnoreCase(key))
        .map(LocalGitProperties.Clone::caminho)
        .filter(p -> p != null && !p.isBlank())
        .findFirst()
        .orElse(null);
    if (mapped == null || mapped.isBlank()) return null;
    Path dir = Path.of(mapped).toAbsolutePath().normalize();
    if (!Files.isDirectory(dir.resolve(".git")) && !Files.isRegularFile(dir.resolve(".git"))) {
      log.warn("Clone local de {} não é um repositório Git: {}", key, dir);
      return null;
    }
    return dir;
  }

  /** origin/release/v5.4.1 → release/v5.4.1; ignora branch padrão (v5/main, main-coso) e HEAD. */
  static String normalizarBranch(String raw) {
    if (raw == null) return null;
    String n = raw.trim();
    if (n.startsWith("origin/")) n = n.substring("origin/".length());
    if (n.equals("HEAD") || n.equals("origin") || n.contains("HEAD")) return null;
    if (!n.toLowerCase(Locale.ROOT).matches("release/v\\d+.*")) return null;
    return n;
  }

  private List<String> git(Path dir, String... args) {
    List<String> cmd = new ArrayList<>();
    cmd.add("git");
    cmd.add("-C");
    cmd.add(dir.toString());
    cmd.addAll(List.of(args));
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(true);
    try {
      Process p = pb.start();
      List<String> lines = new ArrayList<>();
      try (BufferedReader r = new BufferedReader(
          new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = r.readLine()) != null) {
          if (!line.isBlank()) lines.add(line.trim());
        }
      }
      int code = p.waitFor();
      if (code != 0) {
        log.warn("git {} em {} saiu {}", String.join(" ", args), dir, code);
        return List.of();
      }
      return lines;
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      log.warn("Falha ao listar git em {}: {}", dir, e.getMessage());
      return List.of();
    }
  }

  public record LocalGitRef(String name, boolean branch) {}
}
