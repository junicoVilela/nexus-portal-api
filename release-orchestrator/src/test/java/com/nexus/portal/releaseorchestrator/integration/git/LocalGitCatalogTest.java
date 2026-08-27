package com.nexus.portal.releaseorchestrator.integration.git;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.releaseorchestrator.config.LocalGitProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalGitCatalogTest {

  @TempDir Path tmp;

  @Test
  void listaTagsV5EBranchesReleaseV5() throws Exception {
    Path repo = tmp.resolve("dtec-ld");
    Files.createDirectories(repo);
    git(repo, "init", "-b", "v5/main");
    git(repo, "config", "user.email", "test@example.com");
    git(repo, "config", "user.name", "test");
    Files.writeString(repo.resolve("README"), "x");
    git(repo, "add", "README");
    git(repo, "commit", "-m", "init");
    git(repo, "tag", "v5.4.1");
    git(repo, "tag", "v4.4.22");
    git(repo, "checkout", "-b", "release/v5.4.1");
    git(repo, "checkout", "-b", "release/v4.4.1");

    LocalGitCatalog catalog = new LocalGitCatalog(
        new LocalGitProperties(List.of(
            new LocalGitProperties.Clone("softonsi/dtec-ld", repo.toString()))));

    assertThat(catalog.temClone("softonsi/dtec-ld")).isTrue();
    List<LocalGitCatalog.LocalGitRef> refs = catalog.listarRefs("softonsi/dtec-ld");
    assertThat(refs).extracting(LocalGitCatalog.LocalGitRef::name)
        .contains("v5.4.1", "v4.4.22", "release/v5.4.1", "release/v4.4.1")
        .doesNotContain("v5/main");
    assertThat(refs.stream().filter(r -> r.name().equals("release/v5.4.1")))
        .allMatch(LocalGitCatalog.LocalGitRef::branch);
    assertThat(refs.stream().filter(r -> r.name().equals("v5.4.1")))
        .noneMatch(LocalGitCatalog.LocalGitRef::branch);
  }

  @Test
  void normalizarBranch_soReleaseComMajor() {
    assertThat(LocalGitCatalog.normalizarBranch("origin/release/v5.4.1"))
        .isEqualTo("release/v5.4.1");
    assertThat(LocalGitCatalog.normalizarBranch("release/v5.3.58")).isEqualTo("release/v5.3.58");
    assertThat(LocalGitCatalog.normalizarBranch("release/v2.3.18")).isEqualTo("release/v2.3.18");
    assertThat(LocalGitCatalog.normalizarBranch("v5/main")).isNull();
    assertThat(LocalGitCatalog.normalizarBranch("main-coso")).isNull();
    assertThat(LocalGitCatalog.normalizarBranch("origin/HEAD")).isNull();
  }

  private static void git(Path dir, String... args) throws Exception {
    List<String> cmd = new java.util.ArrayList<>();
    cmd.add("git");
    cmd.add("-C");
    cmd.add(dir.toString());
    cmd.addAll(List.of(args));
    Process p = new ProcessBuilder(cmd).inheritIO().start();
    assertThat(p.waitFor()).isZero();
  }
}
