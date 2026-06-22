package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KettleDependencyParserTest {

  private final KettleDependencyParser parser = new KettleDependencyParser();

  private Set<String> parse(String xml) throws IOException {
    try (var in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
      return parser.extrair(in);
    }
  }

  @Test
  @DisplayName("Extrai .ktr/.kjb literais de <filename>")
  void extraiLiterais() throws IOException {
    String xml = """
        <job>
          <entries>
            <entry>
              <name>Carga A</name>
              <filename>subjobs/carga-a.ktr</filename>
            </entry>
            <entry>
              <name>Encerra</name>
              <filename>subjobs/encerra.kjb</filename>
            </entry>
          </entries>
        </job>
        """;
    assertThat(parse(xml)).containsExactlyInAnyOrder(
        "subjobs/carga-a.ktr", "subjobs/encerra.kjb");
  }

  @Test
  @DisplayName("Ignora variáveis Kettle (${PDI_HOME}/x.ktr)")
  void ignoraVariaveis() throws IOException {
    String xml = """
        <job>
          <entry><filename>${PDI_HOME}/subjobs/x.ktr</filename></entry>
          <entry><filename>literal/y.ktr</filename></entry>
          <entry><filename>%%VAR%%/z.ktr</filename></entry>
        </job>
        """;
    assertThat(parse(xml)).containsExactly("literal/y.ktr");
  }

  @Test
  @DisplayName("Ignora paths absolutos")
  void ignoraAbsolutos() throws IOException {
    String xml = """
        <job>
          <entry><filename>/opt/pdi/x.ktr</filename></entry>
          <entry><filename>./y.ktr</filename></entry>
        </job>
        """;
    assertThat(parse(xml)).containsExactly("./y.ktr");
  }

  @Test
  @DisplayName("Ignora <filename> sem extensão de interesse")
  void ignoraOutrasExtensoes() throws IOException {
    String xml = """
        <job>
          <entry><filename>config.properties</filename></entry>
          <entry><filename>job.kjb</filename></entry>
        </job>
        """;
    assertThat(parse(xml)).containsExactly("job.kjb");
  }

  @Test
  @DisplayName("Resolve relativo ao diretório do arquivo pai")
  void resolveRelativo() {
    String r = parser.resolver("subjobs/x.ktr", "kettle/etl/main.kjb");
    assertThat(r).isEqualTo("kettle/etl/subjobs/x.ktr");
  }

  @Test
  @DisplayName("Resolve com .. corretamente")
  void resolveComDoisPontos() {
    String r = parser.resolver("../shared/x.ktr", "kettle/etl/main.kjb");
    assertThat(r).isEqualTo("kettle/shared/x.ktr");
  }

  @Test
  @DisplayName("Resolve com ./ corretamente")
  void resolveComPontoBarra() {
    String r = parser.resolver("./sub.ktr", "kettle/etl/main.kjb");
    assertThat(r).isEqualTo("kettle/etl/sub.ktr");
  }
}
