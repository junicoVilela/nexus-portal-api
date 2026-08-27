package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorManifestoImplantacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.Optional;
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
class ManifestoImplantacaoServiceTest {

  @Mock OrchestratorManifestoImplantacaoRepository repository;
  @Mock ReleaseRepository releaseRepository;
  @InjectMocks ManifestoImplantacaoService service;

  Release release;
  InstalacaoCliente instalacao;

  @BeforeEach
  void setUp() throws Exception {
    Cliente cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    Host host = new Host("SRV", "Srv", "srv", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    host.setDockerDisponivel(true);
    ProdutoRh produto = new ProdutoRh("Nexus LD", "NEXUSLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());
    release = new Release(produto, "1.5.0", "Junho", TipoRelease.MINOR, ReleaseStatus.PUBLICADA,
        null, null, null, null);
    instalacao = new InstalacaoCliente("ACME-RPA", "RPA", cliente, host, produto,
        TipoImplantacao.DOCKER_PULL, AmbientePadrao.PROD);
    org.mockito.Mockito.when(repository.findByRelease_IdAndTipoImplantacao(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.empty());
  }

  @Test
  void resolver_substituiTagDaImagemPelaVersaoDaRelease() {
    instalacao.atualizar("ACME-RPA", "RPA", instalacao.getCliente(), instalacao.getHost(),
        instalacao.getProduto(), TipoImplantacao.DOCKER_PULL,
        instalacao.getStatus(), AmbientePadrao.PROD,
        "registry.local/rpa:old", null, null, null);

    ManifestoImplantacaoResponse m = service.resolver(release, instalacao);

    assertThat(m.imagemRef()).isEqualTo("registry.local/rpa:1.5.0");
    assertThat(m.derivado()).isTrue();
    assertThat(m.resumo()).contains("Docker pull");
  }

  @Test
  void resolver_dockerPullSemImagemUsaPadraoDoProduto() {
    ManifestoImplantacaoResponse m = service.resolver(release, instalacao);

    assertThat(m.imagemRef()).isEqualTo("nexusld:1.5.0");
  }

  @Test
  void validar_linuxManualExigeDiretorio() {
    ManifestoImplantacaoResponse m = new ManifestoImplantacaoResponse(
        null, "1.5.0", TipoImplantacao.LINUX_MANUAL, null, null, null,
        null, "x", true, "fp");

    assertThatThrownBy(() -> service.validarParaTipo(TipoImplantacao.LINUX_MANUAL, m))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("diretório");
  }

  @Test
  void comTag_anexaQuandoNaoHaTag() {
    assertThat(ManifestoImplantacaoService.comTag("nexus/rpa", "2.0.0")).isEqualTo("nexus/rpa:2.0.0");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
