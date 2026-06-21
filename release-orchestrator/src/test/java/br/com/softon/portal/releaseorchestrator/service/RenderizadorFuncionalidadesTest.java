package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ClienteFuncionalidadeProduto;
import br.com.softon.portal.releaseorchestrator.entity.DominioProduto;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.entity.OrigemFuncionalidade;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteFuncionalidadeRepository;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RenderizadorFuncionalidadesTest {

  @Mock OrchestratorClienteFuncionalidadeRepository matrizRepository;
  @InjectMocks RenderizadorFuncionalidades renderizador;

  Cliente cliente;
  ProdutoRh produto;
  Release release;
  Entrega entrega;
  DominioProduto dominio;
  FuncionalidadeProduto func1;
  FuncionalidadeProduto func2;
  ModuloProduto modFunc;
  ModuloProduto modRegras;
  ModuloProduto modWeb;

  @BeforeEach
  void setUp() throws Exception {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, UUID.randomUUID());
    produto = new ProdutoRh("DTEC-LD", "DTECLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());
    release = new Release(produto, "1.5.0", "Release Junho", TipoRelease.MINOR,
        ReleaseStatus.PUBLICADA, null, null, null, null);
    setId(release, UUID.randomUUID());
    entrega = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    setId(entrega, UUID.randomUUID());

    dominio = new DominioProduto(produto, "Usuários", "usuarios", "USR", null, 0);
    func1 = new FuncionalidadeProduto(dominio, "Inserir", "inserir",
        "FUNC_INS", "OP_INS", null, false, 0);
    func2 = new FuncionalidadeProduto(dominio, "Excluir", "excluir",
        "FUNC_DEL", "OP_DEL", null, true, 1);

    modFunc = new ModuloProduto(produto, "dtec-funcs", "Funcionalidades",
        TipoModulo.FUNCIONALIDADES, false, true, 0, null);
    modRegras = new ModuloProduto(produto, "dtec-regras", "Regras",
        TipoModulo.REGRAS, false, true, 0, null);
    modWeb = new ModuloProduto(produto, "dtec-portal", "Portal", TipoModulo.WEB,
        false, true, 0, null);
  }

  @Test
  void renderizar_geraSqlParaModuloFUNCIONALIDADES() {
    when(matrizRepository.findByClienteEProduto(cliente.getId(), produto.getId()))
        .thenReturn(List.of(
            cf(func1, true, OrigemFuncionalidade.MANUAL),
            cf(func2, false, OrigemFuncionalidade.MANUAL)));  // desabilitada
    EntregaModulo em = new EntregaModulo(entrega, modFunc, null, "1.5.0", true, false, 0);

    var virtuais = renderizador.renderizar(entrega, List.of(em));

    assertThat(virtuais).hasSize(1);
    var v = virtuais.get(0);
    assertThat(v.nomeArquivo()).isEqualTo("funcionalidades-acme.sql");
    String sql = new String(v.conteudo(), StandardCharsets.UTF_8);
    assertThat(sql)
        .contains("DELETE FROM TB_CLIENTE_FUNCIONALIDADE")
        .contains("CD_CLIENTE = 'ACME'")
        .contains("'FUNC_INS'")
        .contains("'OP_INS'")
        .doesNotContain("'FUNC_DEL'")  // desabilitada
        .endsWith("COMMIT;\n");
    assertThat(v.sha256()).hasSize(64);
  }

  @Test
  void renderizar_geraJsonParaModuloREGRAS() {
    when(matrizRepository.findByClienteEProduto(cliente.getId(), produto.getId()))
        .thenReturn(List.of(cf(func2, true, OrigemFuncionalidade.HERDADA)));
    EntregaModulo em = new EntregaModulo(entrega, modRegras, null, "1.5.0", true, false, 0);

    var virtuais = renderizador.renderizar(entrega, List.of(em));

    assertThat(virtuais).hasSize(1);
    var v = virtuais.get(0);
    assertThat(v.nomeArquivo()).isEqualTo("regras-acme.json");
    String jsonText = new String(v.conteudo(), StandardCharsets.UTF_8);
    assertThat(jsonText)
        .contains("\"cliente\"")
        .contains("\"ACME\"")
        .contains("\"excluir\"")
        .contains("\"FUNC_DEL\"")
        .contains("\"critica\" : true")
        .contains("\"origem\" : \"HERDADA\"");
  }

  @Test
  void renderizar_ignoraModulosWebEModulosNaoSelecionados() {
    EntregaModulo emWeb = new EntregaModulo(entrega, modWeb, "1.4.0", "1.5.0", true, false, 0);
    EntregaModulo emFuncNaoSel = new EntregaModulo(entrega, modFunc, null, "1.5.0",
        false, false, 1);

    var virtuais = renderizador.renderizar(entrega, List.of(emWeb, emFuncNaoSel));
    assertThat(virtuais).isEmpty();
  }

  @Test
  void renderizar_naoConsultaMatrizQuandoNenhumModuloFuncRegrasSelecionado() {
    EntregaModulo emWeb = new EntregaModulo(entrega, modWeb, "1.4.0", "1.5.0", true, false, 0);

    var virtuais = renderizador.renderizar(entrega, List.of(emWeb));
    assertThat(virtuais).isEmpty();
    org.mockito.Mockito.verifyNoInteractions(matrizRepository);
  }

  @Test
  void renderizar_sqlUsaCodigoComoFallbackQuandoLegadoNulo() {
    FuncionalidadeProduto sem = new FuncionalidadeProduto(dominio, "Sem Legado",
        "sem-legado", null, null, null, false, 0);
    when(matrizRepository.findByClienteEProduto(cliente.getId(), produto.getId()))
        .thenReturn(List.of(cf(sem, true, OrigemFuncionalidade.MANUAL)));
    EntregaModulo em = new EntregaModulo(entrega, modFunc, null, "1.5.0", true, false, 0);

    var virtuais = renderizador.renderizar(entrega, List.of(em));
    String sql = new String(virtuais.get(0).conteudo(), StandardCharsets.UTF_8);
    assertThat(sql).contains("'sem-legado'").contains("'DEFAULT'");
  }

  private ClienteFuncionalidadeProduto cf(FuncionalidadeProduto fp, boolean habilitada,
      OrigemFuncionalidade origem) {
    return new ClienteFuncionalidadeProduto(cliente, fp, habilitada, origem);
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
