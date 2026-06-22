package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.response.DeltaResumoResponse;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.StatusEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import br.com.softon.portal.releaseorchestrator.repository.ArtefatoReleaseModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloArtefatoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
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
class DeltaEntregaServiceTest {

  @Mock OrchestratorEntregaModuloArtefatoRepository repository;
  @Mock OrchestratorEntregaModuloRepository entregaModuloRepository;
  @Mock ArtefatoReleaseModuloRepository artefatoRepository;
  @Mock EntregaService entregaService;
  @Mock GithubAssetSyncService githubAssetSyncService;
  @InjectMocks DeltaEntregaService service;

  Cliente cliente;
  ProdutoRh produto;
  Release release;
  Entrega entrega;
  ModuloProduto modPortal;
  ModuloProduto modDb;
  final UUID entregaId = UUID.randomUUID();
  final UUID releaseId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    produto = new ProdutoRh("DTEC-LD", "DTECLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());
    release = new Release(produto, "1.5.0", "Release", TipoRelease.MINOR,
        ReleaseStatus.PUBLICADA, null, null, null, null);
    setId(release, releaseId);
    entrega = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    setId(entrega, entregaId);

    modPortal = newModulo("portal", "Portal", TipoModulo.WEB);
    modDb = newModulo("db", "DB", TipoModulo.BANCO);

    when(entregaService.buscar(entregaId)).thenReturn(entrega);
  }

  @Test
  void calcular_geraDeltaComArtefatosDosModulosSelecionados() {
    EntregaModulo emPortal = newEm(modPortal, "1.4.0", "1.5.0", true);
    EntregaModulo emDb = newEm(modDb, "1.4.0", "1.5.0", true);
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of(emPortal, emDb));

    ArtefatoReleaseModulo war = new ArtefatoReleaseModulo(
        release, modPortal, "dtec.war", "/tmp/war", "sha1", 1024, null);
    ArtefatoReleaseModulo sqlDdl = new ArtefatoReleaseModulo(
        release, modDb, "ddl.sql", "/tmp/ddl", "sha2", 512, null);
    ArtefatoReleaseModulo sqlDml = new ArtefatoReleaseModulo(
        release, modDb, "dml.sql", "/tmp/dml", "sha3", 768, null);

    when(artefatoRepository.findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(
        releaseId, modPortal.getId())).thenReturn(List.of(war));
    when(artefatoRepository.findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(
        releaseId, modDb.getId())).thenReturn(List.of(sqlDdl, sqlDml));
    when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    DeltaResumoResponse resumo = service.calcular(entregaId);

    assertThat(resumo.totalArtefatos()).isEqualTo(3);
    assertThat(resumo.totalTamanhoBytes()).isEqualTo(1024 + 512 + 768);
    assertThat(resumo.modulos()).hasSize(2);
    assertThat(resumo.modulos().get(0).quantidadeArtefatos()).isEqualTo(1);
    assertThat(resumo.modulos().get(1).quantidadeArtefatos()).isEqualTo(2);
    verify(repository).deleteByEntregaModulo_Entrega_Id(entregaId);
  }

  @Test
  void calcular_ignoraModulosNaoSelecionados() {
    EntregaModulo emPortal = newEm(modPortal, "1.4.0", "1.5.0", true);
    EntregaModulo emDb = newEm(modDb, "1.4.0", "1.5.0", false);  // não selecionado
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of(emPortal, emDb));

    ArtefatoReleaseModulo war = new ArtefatoReleaseModulo(
        release, modPortal, "dtec.war", "/tmp/war", "sha1", 1024, null);
    when(artefatoRepository.findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(
        releaseId, modPortal.getId())).thenReturn(List.of(war));
    when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    DeltaResumoResponse resumo = service.calcular(entregaId);

    assertThat(resumo.modulos()).hasSize(1);
    assertThat(resumo.modulos().get(0).codigo()).isEqualTo("portal");
  }

  @Test
  void calcular_aceitaModuloSemArtefatos() {
    EntregaModulo emPortal = newEm(modPortal, "1.4.0", "1.5.0", true);
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of(emPortal));
    when(artefatoRepository.findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(
        releaseId, modPortal.getId())).thenReturn(List.of());

    DeltaResumoResponse resumo = service.calcular(entregaId);

    assertThat(resumo.totalArtefatos()).isZero();
    assertThat(resumo.modulos()).hasSize(1);
    assertThat(resumo.modulos().get(0).quantidadeArtefatos()).isZero();
  }

  @Test
  void calcular_rejeitaEntregaNaoRascunho() {
    entrega.alterarStatus(StatusEntrega.EM_GERACAO);

    assertThatThrownBy(() -> service.calcular(entregaId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("RASCUNHO");
  }

  @Test
  void calcular_rejeitaSelecaoVazia() {
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.calcular(entregaId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Inicialize");
  }

  private EntregaModulo newEm(ModuloProduto m, String from, String to, boolean selecionado) {
    return new EntregaModulo(entrega, m, from, to, selecionado, false, 0);
  }

  private ModuloProduto newModulo(String codigo, String nome, TipoModulo tipo) throws Exception {
    ModuloProduto m = new ModuloProduto(produto, codigo, nome, tipo, false, true, 0, null);
    setId(m, UUID.randomUUID());
    return m;
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
