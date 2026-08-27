package com.nexus.portal.releaseorchestrator.integration.ssh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class HostPortProbeTest {

  @Test
  void parse_ssENetstat() {
    String out = """
        0.0.0.0:8010
        [::]:4209
        127.0.0.1:5432
        *:22
        """;
    assertThat(HostPortProbe.parse(out)).containsExactly(8010, 4209, 5432, 22);
  }

  @Test
  void parse_vazio() {
    assertThat(HostPortProbe.parse(null)).isEmpty();
    assertThat(HostPortProbe.parse("")).isEmpty();
    assertThat(HostPortProbe.parse("LISTEN")).isEqualTo(Set.of());
  }
}
