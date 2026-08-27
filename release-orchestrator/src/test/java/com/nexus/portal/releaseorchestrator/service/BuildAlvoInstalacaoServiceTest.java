package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse.AlvoBuild;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.repository.BuildInstalacaoArtefatoRepository;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuildAlvoInstalacaoServiceTest {

  @Mock ModuloProdutoRepository moduloRepository;
  @Mock BuildInstalacaoArtefatoRepository buildRepository;
  BuildAlvoInstalacaoService service;

  ProdutoRh ld;
  InstalacaoCliente inst;

  @BeforeEach
  void setUp() {
    service = new BuildAlvoInstalacaoService(
        moduloRepository, buildRepository, new ObjectMapper());
    ld = new ProdutoRh("Softon V5", "LD", null, "#111", null, true);
    ld.setId(UUID.randomUUID());
    ld.atualizarIntegracaoJenkins("http://j", "ld-v5-build", "admin", "token", "BUILD_ON_TAG");
    Cliente cliente = new Cliente("BMW", "BMW", AmbientePadrao.HOM);
    cliente.setId(UUID.randomUUID());
    Host host = new Host("LOCAL", "Local", "localhost", SistemaOperacionalHost.LINUX,
        TipoConexaoHost.SSH);
    inst = new InstalacaoCliente("BMW", "BMW - V5", cliente, host, ld,
        TipoImplantacao.LINUX_MANUAL, AmbientePadrao.HOM);
    inst.setId(UUID.randomUUID());
    when(moduloRepository.findByProduto_IdAndAtivoTrueOrderByOrdemAscNomeAsc(ld.getId()))
        .thenReturn(List.of());
    when(buildRepository.findTop8ByInstalacao_IdOrderByCreatedAtDesc(inst.getId()))
        .thenReturn(List.of());
  }

  @Test
  void listar_semModulosTrazSoJobDoProdutoDaInstalacao() {
    List<AlvoBuild> alvos = service.listar(inst);
    assertThat(alvos).extracting(AlvoBuild::produtoSigla).containsExactly("LD");
    assertThat(alvos.get(0).selecionadoPadrao()).isTrue();
    assertThat(alvos.get(0).jenkinsJob()).isEqualTo("ld-v5-build");
  }

  @Test
  void selecionar_semIdsUsaSoProdutoDaInstalacao() {
    List<AlvoBuild> alvos = service.listar(inst);
    List<AlvoBuild> escolhidos = service.selecionar(alvos, null, ld.getId());
    assertThat(escolhidos).extracting(AlvoBuild::produtoSigla).containsExactly("LD");
  }

  @Test
  void selecionar_respeitaAlvoIds() {
    List<AlvoBuild> alvos = service.listar(inst);
    List<AlvoBuild> escolhidos = service.selecionar(alvos, List.of("inexistente"), ld.getId());
    assertThat(escolhidos).isEmpty();
  }

  @Test
  void listar_comModulosUsaJobDoModulo() {
    ModuloProduto backend = new ModuloProduto(ld, "backend", "Backend WAR", TipoModulo.WEB,
        false, true, 1, "{\"jenkinsJob\":\"ld-v5-build\",\"nomeArtefato\":\"ldv4.war\",\"padraoAsset\":\"ldv4.war\"}");
    backend.setId(UUID.randomUUID());
    when(moduloRepository.findByProduto_IdAndAtivoTrueOrderByOrdemAscNomeAsc(ld.getId()))
        .thenReturn(List.of(backend));

    List<AlvoBuild> alvos = service.listar(inst);

    assertThat(alvos).extracting(AlvoBuild::tipo).containsExactly("MODULO");
    assertThat(alvos.get(0).jenkinsJob()).isEqualTo("ld-v5-build");
    assertThat(alvos.get(0).nomeArquivo()).isEqualTo("ldv4.war");
    assertThat(alvos.get(0).selecionadoPadrao()).isTrue();
  }

  @Test
  void listar_appJarSemJobFicaDesmarcado() {
    ModuloProduto app = new ModuloProduto(ld, "ld-app", "Aplicação (app.jar)", TipoModulo.BATCH,
        false, false, 30,
        "{\"jenkinsJob\":\"\",\"nomeArtefato\":\"app.jar\",\"padraoAsset\":\"app.jar\",\"selecionadoPadrao\":false}");
    app.setId(UUID.randomUUID());
    when(moduloRepository.findByProduto_IdAndAtivoTrueOrderByOrdemAscNomeAsc(ld.getId()))
        .thenReturn(List.of(app));

    List<AlvoBuild> alvos = service.listar(inst);

    assertThat(alvos.get(0).jenkinsJob()).isNull();
    assertThat(alvos.get(0).nomeArquivo()).isEqualTo("app.jar");
    assertThat(alvos.get(0).selecionadoPadrao()).isFalse();
  }
}
