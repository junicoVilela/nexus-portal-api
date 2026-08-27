package com.nexus.portal.releaseorchestrator.integration.ssh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinuxManualTreeCopierTest {

  @TempDir Path temp;

  @Test
  void copiar_criaPastasEIgnoraEnvELogs() throws Exception {
    Path src = temp.resolve("template");
    Path dst = temp.resolve("cliente");
    Files.createDirectories(src.resolve("scripts"));
    Files.createDirectories(src.resolve("logs"));
    Files.createDirectories(src.resolve("jdk/temurin-8"));
    Files.createDirectories(src.resolve("runtime/apache-tomcat-9.0.98/bin"));
    Files.writeString(src.resolve("scripts/start.sh"), "#!/bin/bash\n");
    Files.writeString(src.resolve("scripts/install.sh"), "#!/bin/bash\n");
    Files.writeString(src.resolve(".env"), "DATABASE_PASSWORD=secreto\n");
    Files.writeString(src.resolve("logs/application.log"), "lixo\n");
    Files.writeString(src.resolve("jdk/temurin-8/release"), "JAVA\n");
    Files.writeString(src.resolve("runtime/apache-tomcat-9.0.98/bin/catalina.sh"), "ok\n");
    Files.writeString(src.resolve("readme.txt"), "ok\n");

    LinuxManualTreeCopier.copiar(src, dst);

    assertThat(dst.resolve("scripts/start.sh")).exists();
    assertThat(dst.resolve("scripts/install.sh")).exists();
    assertThat(dst.resolve("readme.txt")).exists();
    assertThat(dst.resolve(".env")).doesNotExist();
    assertThat(dst.resolve("logs/application.log")).doesNotExist();
    assertThat(dst.resolve("jdk/temurin-8/release")).doesNotExist();
    assertThat(dst.resolve("runtime/apache-tomcat-9.0.98/bin/catalina.sh")).doesNotExist();
  }

  @Test
  void copiar_recusaDestinoIgualAoTemplate() throws Exception {
    Path src = temp.resolve("mesmo");
    Files.createDirectories(src);
    assertThatThrownBy(() -> LinuxManualTreeCopier.copiar(src, src))
        .isInstanceOf(SshHostException.class)
        .hasMessageContaining("template");
  }

  @Test
  void copiar_recusaDestinoQueContemOTemplate() throws Exception {
    Path dst = temp.resolve("V5");
    Path src = dst.resolve("instalador/instalador");
    Files.createDirectories(src);
    assertThatThrownBy(() -> LinuxManualTreeCopier.copiar(src, dst))
        .isInstanceOf(SshHostException.class)
        .hasMessageContaining("contém a pasta template");
  }
}
