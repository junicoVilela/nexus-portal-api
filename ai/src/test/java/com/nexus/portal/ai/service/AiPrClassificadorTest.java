package com.nexus.portal.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.portal.ai.config.AiGithubProperties;
import com.nexus.portal.ai.entity.AiPrClassificacao;
import com.nexus.portal.ai.integration.github.AiGithubClient.ArquivoPr;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiPrClassificadorTest {

  private static final List<String> TELA = AiGithubProperties.CAMINHOS_TELA_PADRAO;

  private static AiPrClassificador.Resultado classificar(List<String> rotulos, ArquivoPr... arquivos) {
    return AiPrClassificador.classificar("Consulta de pedidos PED-001", "codigoTela: PED-010\nAjusta filtros.",
        rotulos, List.of(arquivos), TELA, "docs:skip");
  }

  private static ArquivoPr arquivo(String caminho, String status) {
    return new ArquivoPr(caminho, status, 10, 2, "@@ -1 +1 @@");
  }

  @Test
  void telaComArquivoNovoEhUiNova() {
    var resultado = classificar(List.of(),
        arquivo("src/app/pages/pedidos/pedidos.component.html", "added"),
        arquivo("src/app/pages/pedidos/pedidos.component.spec.ts", "added"),
        arquivo("backend/PedidoController.java", "modified"));

    assertThat(resultado.classificacao()).isEqualTo(AiPrClassificacao.UI_NOVA);
    assertThat(resultado.arquivosTela()).containsExactly("src/app/pages/pedidos/pedidos.component.html");
  }

  @Test
  void soTelaModificadaEhUiAlteracao() {
    assertThat(classificar(List.of(), arquivo("Pedidos.tsx", "modified")).classificacao())
        .isEqualTo(AiPrClassificacao.UI_ALTERACAO);
  }

  @Test
  void semArquivoDeTelaEhSoBackend() {
    assertThat(classificar(List.of(), arquivo("api/src/main/java/Pedido.java", "modified")).classificacao())
        .isEqualTo(AiPrClassificacao.SO_BACKEND);
  }

  @Test
  void docsTestesEBuildSaoIrrelevantes() {
    assertThat(classificar(List.of(),
        arquivo("README.md", "modified"),
        arquivo("src/app/x.component.spec.ts", "modified"),
        arquivo(".github/workflows/ci.yml", "modified")).classificacao())
        .isEqualTo(AiPrClassificacao.IRRELEVANTE);
  }

  @Test
  void rotuloDePularVenceTudo() {
    var resultado = classificar(List.of("docs:skip"), arquivo("src/app/pages/a.component.html", "added"));
    assertThat(resultado.classificacao()).isEqualTo(AiPrClassificacao.IRRELEVANTE);
    assertThat(resultado.motivo()).contains("docs:skip");
  }

  @Test
  void marcadorExplicitoVemAntesDosCodigosSoltos() {
    assertThat(AiPrClassificador.codigosTela("Consulta PED-001 em UTF-8 (DF-START)",
        "codigoTela: ped-010\nVer CLI-CAD-02 e SHA-256"))
        .containsExactly("PED-010", "PED-001", "CLI-CAD-02", "DF-START");
  }

  @Test
  void siglaSemDigitoSoServeParaAcharPaginaExistente() {
    assertThat(AiPrClassificador.codigoSugerido("Ajusta DF-START", null)).isNull();
    assertThat(AiPrClassificador.codigoSugerido("Ajusta DF-START e PED-001", null)).isEqualTo("PED-001");
    assertThat(AiPrClassificador.codigoSugerido("Ajusta DF-START", "tela: DF-NOVA")).isEqualTo("DF-NOVA");
  }
}
