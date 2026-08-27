package com.nexus.portal.releaseorchestrator.integration.ssh;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;

/** Copia o esqueleto do instalador (scripts, templates, artifacts) sem JDK/Tomcat/logs/.env. */
public final class LinuxManualTreeCopier {

  private static final Set<String> SKIP_NAMES = Set.of(".env", "application.pid", "catalina.out");
  private static final Set<String> SKIP_DIRS = Set.of("logs", "jdk", "runtime", ".git");

  private LinuxManualTreeCopier() {}

  public static void copiar(Path origem, Path destino) {
    try {
      Path src = origem.toAbsolutePath().normalize();
      Path dst = destino.toAbsolutePath().normalize();
      if (!Files.isDirectory(src)) {
        throw new SshHostException("Template da versão pronta não é pasta: " + src);
      }
      if (src.equals(dst)) {
        throw new SshHostException(
            "O destino não pode ser a pasta template (" + src + "). Use outro diretório, "
                + "por exemplo /tmp/nexus-lab/<codigo>.");
      }
      if (dst.startsWith(src)) {
        throw new SshHostException(
            "O destino não pode ficar dentro da pasta template (" + src + ").");
      }
      if (src.startsWith(dst)) {
        throw new SshHostException(
            "O destino " + dst + " contém a pasta template " + src
                + ". Não copie o instalador para cima dele. Use um caminho fora dessa árvore "
                + "(ex.: /tmp/nexus-lab/<codigo>) ou, para reutilizar a versão pronta sem copiar, "
                + "informe exatamente " + src + " no campo Diretório de instalação.");
      }
      Files.createDirectories(dst);
      Files.walkFileTree(src, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
          if (SKIP_DIRS.contains(name) || name.equalsIgnoreCase(".git")) {
            return FileVisitResult.SKIP_SUBTREE;
          }
          try {
            Files.createDirectories(dst.resolve(src.relativize(dir)));
          } catch (IOException e) {
            throw new SshHostException("Não criou pasta " + dir + ": " + e.getMessage(), e);
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          String name = file.getFileName().toString();
          String lower = name.toLowerCase(Locale.ROOT);
          if (SKIP_NAMES.contains(name) || lower.endsWith(".log")) {
            return FileVisitResult.CONTINUE;
          }
          Files.copy(
              file,
              dst.resolve(src.relativize(file)),
              StandardCopyOption.REPLACE_EXISTING,
              StandardCopyOption.COPY_ATTRIBUTES);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (SshHostException e) {
      throw e;
    } catch (IOException e) {
      throw new SshHostException("Falha ao copiar versão pronta: " + e.getMessage(), e);
    }
  }
}
