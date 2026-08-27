package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ArtefatoBuildMatcherTest {

  @Test
  void casa_warEJarERejeitaPlain() {
    assertThat(ArtefatoBuildMatcher.casa("ldv4.war", "*.war,*.jar")).isTrue();
    assertThat(ArtefatoBuildMatcher.casa("app.jar", "*.jar")).isTrue();
    assertThat(ArtefatoBuildMatcher.casa("dtec-application-1.0-plain.jar", "*.jar")).isFalse();
    assertThat(ArtefatoBuildMatcher.casa("readme.txt", "*.war,*.jar")).isFalse();
  }

  @Test
  void destino_renomeiaQuandoInformado() {
    assertThat(ArtefatoBuildMatcher.destino("target/foo.war", "ldv4.war")).isEqualTo("ldv4.war");
    assertThat(ArtefatoBuildMatcher.destino("backend/ldv4.war", null)).isEqualTo("ldv4.war");
  }

  @Test
  void config_jobENomeDestino() {
    ObjectMapper mapper = new ObjectMapper();
    ConfigEmpacotamentoModulo cfg = ConfigEmpacotamentoModulo.de(
        "{\"jenkinsJob\":\"cr-build\",\"nomeArtefato\":\"cr.war\",\"padraoAsset\":\"*.war\"}",
        mapper);
    assertThat(cfg.jobOu("ld-v5-build")).isEqualTo("cr-build");
    assertThat(cfg.nomeDestino()).isEqualTo("cr.war");
    assertThat(cfg.padraoAssetOuDefault(null)).isEqualTo("*.war");
  }

  @Test
  void config_jobVazioNaoHerdaEDesmarca() {
    ObjectMapper mapper = new ObjectMapper();
    ConfigEmpacotamentoModulo cfg = ConfigEmpacotamentoModulo.de(
        "{\"jenkinsJob\":\"\",\"nomeArtefato\":\"app.jar\",\"selecionadoPadrao\":false}",
        mapper);
    assertThat(cfg.jobOu("ld-v5-build")).isNull();
    assertThat(cfg.marcadoPadrao(false)).isFalse();
  }
}
