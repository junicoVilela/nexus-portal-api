package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.ClienteProduto;
import com.nexus.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaModulo;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseModuloVersao;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.StatusEntrega;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseModuloVersaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.util.List;
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
class EntregaModuloServiceTest {

  @Mock OrchestratorEntregaModuloRepository repository;
  @Mock EntregaService entregaService;
  @Mock OrchestratorClienteProdutoRepository clienteProdutoRepository;
  @Mock OrchestratorClienteProdutoModuloRepository clienteProdutoModuloRepository;
  @Mock ReleaseModuloVersaoRepository releaseModuloVersaoRepository;
  @InjectMocks EntregaModuloService service;

  Cliente cliente;
  ProdutoRh produto;
  Release release;
  Entrega entrega;
  ClienteProduto contrato;
  ModuloProduto modPortal;
  ModuloProduto modApi;
  ModuloProduto modDb;
  final UUID entregaId = UUID.randomUUID();
  final UUID clienteId = UUID.randomUUID();
  final UUID produtoId = UUID.randomUUID();
  final UUID releaseId = UUID.randomUUID();
  final UUID contratoId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, clienteId);
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    setId(produto, produtoId);
    release = new Release(produto, "1.5.0", "Release", TipoRelease.MINOR,
        ReleaseStatus.PUBLICADA, null, null, null, null);
    setId(release, releaseId);
    entrega = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    setId(entrega, entregaId);

    contrato = new ClienteProduto(cliente, produto, AmbientePadrao.PROD);
    setId(contrato, contratoId);

    modPortal = newModulo("nexus-portal", "Portal", 1);
    modApi = newModulo("nexus-api", "API", 2);
    modDb = newModulo("nexus-db", "DB", 3);

    when(entregaService.buscar(entregaId)).thenReturn(entrega);
    when(clienteProdutoRepository.findByCliente_IdAndProduto_Id(clienteId, produtoId))
        .thenReturn(Optional.of(contrato));
  }

  @Test
  void inicializar_automarcaModulosQueMudaramDeVersao() {
    // Contrato: portal 1.4.0 + api 1.4.0
    ClienteProdutoModulo cpmPortal = new ClienteProdutoModulo(contrato, modPortal, "1.4.0");
    ClienteProdutoModulo cpmApi = new ClienteProdutoModulo(contrato, modApi, "1.4.0");
    when(clienteProdutoModuloRepository
        .findByClienteProduto_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(contratoId))
        .thenReturn(List.of(cpmPortal, cpmApi));

    // Release: portal 1.5.0 (mudou) + api 1.4.0 (mesma)
    ReleaseModuloVersao rmvPortal = new ReleaseModuloVersao(release, modPortal, "1.5.0");
    ReleaseModuloVersao rmvApi = new ReleaseModuloVersao(release, modApi, "1.4.0");
    when(releaseModuloVersaoRepository
        .findByRelease_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(releaseId))
        .thenReturn(List.of(rmvPortal, rmvApi));

    when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    List<EntregaModulo> result = service.inicializar(entregaId);

    assertThat(result).hasSize(2);
    EntregaModulo portal = result.get(0);
    assertThat(portal.getModuloProduto().getCodigo()).isEqualTo("nexus-portal");
    assertThat(portal.getVersaoFrom()).isEqualTo("1.4.0");
    assertThat(portal.getVersaoTo()).isEqualTo("1.5.0");
    assertThat(portal.isSelecionado()).isTrue();   // mudou
    assertThat(portal.isForaContrato()).isFalse();

    EntregaModulo api = result.get(1);
    assertThat(api.isSelecionado()).isFalse();    // mesma versão
    assertThat(api.isForaContrato()).isFalse();

    verify(repository).deleteByEntrega_Id(entregaId);
  }

  @Test
  void inicializar_moduloNovoNaReleaseEntraComoForaContrato() {
    // Contrato vazio
    when(clienteProdutoModuloRepository
        .findByClienteProduto_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(contratoId))
        .thenReturn(List.of());

    // Release tem db
    ReleaseModuloVersao rmvDb = new ReleaseModuloVersao(release, modDb, "1.5.0");
    when(releaseModuloVersaoRepository
        .findByRelease_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(releaseId))
        .thenReturn(List.of(rmvDb));

    when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    List<EntregaModulo> result = service.inicializar(entregaId);

    assertThat(result).hasSize(1);
    EntregaModulo db = result.get(0);
    assertThat(db.isForaContrato()).isTrue();
    assertThat(db.isSelecionado()).isFalse();  // operador decide
    assertThat(db.getVersaoFrom()).isNull();
    assertThat(db.getVersaoTo()).isEqualTo("1.5.0");
  }

  @Test
  void inicializar_moduloContratadoSemVersaoNaRelease() {
    ClienteProdutoModulo cpm = new ClienteProdutoModulo(contrato, modPortal, "1.4.0");
    when(clienteProdutoModuloRepository
        .findByClienteProduto_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(contratoId))
        .thenReturn(List.of(cpm));
    when(releaseModuloVersaoRepository
        .findByRelease_IdOrderByModuloProduto_OrdemAscModuloProduto_NomeAsc(releaseId))
        .thenReturn(List.of());
    when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    List<EntregaModulo> result = service.inicializar(entregaId);

    EntregaModulo portal = result.get(0);
    assertThat(portal.getVersaoFrom()).isEqualTo("1.4.0");
    assertThat(portal.getVersaoTo()).isNull();
    assertThat(portal.isSelecionado()).isFalse();  // nada a entregar (sem to)
  }

  @Test
  void inicializar_rejeitaEntregaNaoRascunho() {
    entrega.alterarStatus(StatusEntrega.EM_GERACAO);

    assertThatThrownBy(() -> service.inicializar(entregaId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("RASCUNHO");
  }

  @Test
  void inicializar_rejeitaQuandoSemContrato() {
    when(clienteProdutoRepository.findByCliente_IdAndProduto_Id(clienteId, produtoId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.inicializar(entregaId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Contrato");
  }

  @Test
  void alterarSelecao_atualizaFlag() {
    UUID moduloId = modPortal.getId();
    EntregaModulo em = new EntregaModulo(entrega, modPortal, "1.4.0", "1.5.0",
        true, false, 0);
    when(repository.findByEntrega_IdAndModuloProduto_Id(entregaId, moduloId))
        .thenReturn(Optional.of(em));

    EntregaModulo result = service.alterarSelecao(entregaId, moduloId, false);
    assertThat(result.isSelecionado()).isFalse();
  }

  @Test
  void alterarSelecao_rejeitaQuandoEntregaForaDoRascunho() {
    entrega.alterarStatus(StatusEntrega.EM_GERACAO);

    assertThatThrownBy(() -> service.alterarSelecao(entregaId, UUID.randomUUID(), true))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("imutável");
  }

  @Test
  void alterarSelecao_lancaQuandoLinhaNaoExiste() {
    UUID moduloId = UUID.randomUUID();
    when(repository.findByEntrega_IdAndModuloProduto_Id(entregaId, moduloId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.alterarSelecao(entregaId, moduloId, true))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Inicialize");
  }

  private ModuloProduto newModulo(String codigo, String nome, int ordem) throws Exception {
    ModuloProduto m = new ModuloProduto(produto, codigo, nome, TipoModulo.WEB,
        false, true, ordem, null);
    setId(m, UUID.randomUUID());
    return m;
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
