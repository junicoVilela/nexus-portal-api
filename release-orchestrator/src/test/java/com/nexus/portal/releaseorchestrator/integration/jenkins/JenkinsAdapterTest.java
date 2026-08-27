package com.nexus.portal.releaseorchestrator.integration.jenkins;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JenkinsAdapterTest {

  @Test
  void jobPath_pastasViraramJobSegmentos() {
    assertThat(JenkinsAdapter.jobPath("ld-build")).isEqualTo("/job/ld-build");
    assertThat(JenkinsAdapter.jobPath("folder/ld-build")).isEqualTo("/job/folder/job/ld-build");
    assertThat(JenkinsAdapter.jobPath("com espaço")).isEqualTo("/job/com%20espa%C3%A7o");
  }

  @Test
  void parametrosPadrao_preencheContratoDoJenkinsfile() {
    var p = JenkinsAdapter.parametrosPadrao("LD", "5.0.0", "v5.0.0", "refs/tags/v5.0.0",
        "RELEASE_ATUAL");
    assertThat(p).containsEntry("PORTAL_PRODUCT_SIGLA", "LD")
        .containsEntry("RELEASE_VERSION", "5.0.0")
        .containsEntry("TAG_NAME", "v5.0.0")
        .containsEntry("GIT_REF", "refs/tags/v5.0.0")
        .containsEntry("ORIGEM", "RELEASE_ATUAL");
  }

  @Test
  void filaPath_converteUrlAbsolutaEmPathDaApi() {
    assertThat(JenkinsAdapter.filaPath("http://localhost:8090",
        "http://localhost:8090/queue/item/12/"))
        .isEqualTo("/queue/item/12/api/json?tree=cancelled,executable[number,url]");
    assertThat(JenkinsAdapter.filaPath("http://localhost:8090", "/queue/item/3"))
        .isEqualTo("/queue/item/3/api/json?tree=cancelled,executable[number,url]");
  }

  @Test
  void encodeArtifactPath_preservaPastas() {
    assertThat(JenkinsAdapter.encodeArtifactPath("backend/target/ldv4.war"))
        .isEqualTo("backend/target/ldv4.war");
    assertThat(JenkinsAdapter.encodeArtifactPath("a b.war")).isEqualTo("a%20b.war");
  }
}
