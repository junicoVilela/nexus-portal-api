package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.StatusEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloArtefatoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
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
class GeracaoEntregaServiceTest {

  @Mock OrchestratorEntregaRepository entregaRepository;
  @Mock OrchestratorEntregaModuloRepository entregaModuloRepository;
  @Mock OrchestratorEntregaModuloArtefatoRepository deltaRepository;
  @Mock OrchestratorClienteProdutoRepository clienteProdutoRepository;
  @Mock OrchestratorClienteProdutoModuloRepository cpmRepository;
  @Mock EmpacotadorEntrega empacotador;
  @Mock RenderizadorFuncionalidades renderizador;
  @Mock PublicacaoRemotaService publicacaoRemotaService;
  MeterRegistry meterRegistry = new SimpleMeterRegistry();

  GeracaoEntregaService service;
  ReleaseOrchestratorStorageProperties storage;

  Cliente cliente;
  ProdutoRh produto;
  Release release;
  Entrega entrega;
  ModuloProduto modPortal;
  ClienteProduto contrato;
  final UUID entregaId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, UUID.randomUUID());
    produto = new ProdutoRh("DTEC-LD", "DTECLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());
    release = new Release(produto, "1.5.0", "Release", TipoRelease.MINOR,
        ReleaseStatus.PUBLICADA, null, null, null, null);
    setId(release, UUID.randomUUID());
    entrega = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    setId(entrega, entregaId);

    modPortal = new ModuloProduto(produto, "portal", "Portal", TipoModulo.WEB,
        false, true, 0, null);
    setId(modPortal, UUID.randomUUID());

    contrato = new ClienteProduto(cliente, produto, AmbientePadrao.PROD);
    setId(contrato, UUID.randomUUID());

    storage = new ReleaseOrchestratorStorageProperties(null, "/tmp/entregas", 90);
    service = new GeracaoEntregaService(entregaRepository, entregaModuloRepository,
        deltaRepository, clienteProdutoRepository, cpmRepository, empacotador,
        renderizador, storage, publicacaoRemotaService, meterRegistry);
    service.registrarMetricas();

    when(renderizador.renderizar(any(), any())).thenReturn(List.of());
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(any()))
        .thenReturn(List.of());
  }

  @Test
  void iniciar_transicionaEM_GERACAOQuandoRascunho() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(deltaRepository.findByEntrega_Id(entregaId)).thenReturn(List.of(mockDelta()));

    GeracaoEntregaService spied = spy(service);
    org.mockito.Mockito.doNothing().when(spied).executar(any());

    Entrega result = spied.iniciar(entregaId);
    assertThat(result.getStatus()).isEqualTo(StatusEntrega.EM_GERACAO);
    assertThat(result.getDataInicioGeracao()).isNotNull();
    verify(spied).executar(entregaId);
  }

  @Test
  void iniciar_aceitaRetentativaDeFalha() {
    entrega.alterarStatus(StatusEntrega.FALHA);
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(deltaRepository.findByEntrega_Id(entregaId)).thenReturn(List.of(mockDelta()));

    GeracaoEntregaService spied = spy(service);
    org.mockito.Mockito.doNothing().when(spied).executar(any());

    Entrega result = spied.iniciar(entregaId);
    assertThat(result.getStatus()).isEqualTo(StatusEntrega.EM_GERACAO);
  }

  @Test
  void iniciar_rejeitaEntregaJaConcluida() {
    entrega.alterarStatus(StatusEntrega.CONCLUIDA);
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));

    assertThatThrownBy(() -> service.iniciar(entregaId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("CONCLUIDA");
  }

  @Test
  void iniciar_rejeitaQuandoDeltaERenderizaveisVazios() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(deltaRepository.findByEntrega_Id(entregaId)).thenReturn(List.of());
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.iniciar(entregaId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Nada a entregar");
  }

  @Test
  void iniciar_rejeitaQuandoStorageEntregasNaoConfigurado() {
    storage = new ReleaseOrchestratorStorageProperties(null, null, 90);
    service = new GeracaoEntregaService(entregaRepository, entregaModuloRepository,
        deltaRepository, clienteProdutoRepository, cpmRepository, empacotador,
        renderizador, storage, publicacaoRemotaService, meterRegistry);
    service.registrarMetricas();
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));

    assertThatThrownBy(() -> service.iniciar(entregaId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("entregas-dir");
  }

  @Test
  void iniciar_lancaNotFoundQuandoEntregaInexistente() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.iniciar(entregaId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void executar_concluiQuandoEmpacotamentoOk() throws Exception {
    entrega.alterarStatus(StatusEntrega.EM_GERACAO);
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(deltaRepository.findByEntrega_Id(entregaId)).thenReturn(List.of(mockDelta()));
    when(empacotador.empacotar(any(), any(), any(), any())).thenReturn(
        new EmpacotadorEntrega.PacoteGerado("/tmp/pacote.zip", "shaXXX", 1024, 1));
    EntregaModulo em = new EntregaModulo(entrega, modPortal, "1.4.0", "1.5.0", true, false, 0);
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId))
        .thenReturn(List.of(em));
    when(clienteProdutoRepository.findByCliente_IdAndProduto_Id(
        cliente.getId(), produto.getId()))
        .thenReturn(Optional.of(contrato));
    ClienteProdutoModulo cpm = new ClienteProdutoModulo(contrato, modPortal, "1.4.0");
    when(cpmRepository.findByClienteProduto_IdAndModuloProduto_Id(
        contrato.getId(), modPortal.getId()))
        .thenReturn(Optional.of(cpm));

    service.executar(entregaId);

    assertThat(entrega.getStatus()).isEqualTo(StatusEntrega.CONCLUIDA);
    assertThat(entrega.getArquivoPacoteCaminho()).isEqualTo("/tmp/pacote.zip");
    assertThat(entrega.getArquivoPacoteSha256()).isEqualTo("shaXXX");
    assertThat(entrega.getTamanhoBytes()).isEqualTo(1024);
    // versão atual do módulo no contrato foi atualizada
    assertThat(cpm.getVersaoAtual()).isEqualTo("1.5.0");
  }

  @Test
  void executar_marcaFalhaQuandoEmpacotadorLancaExcecao() throws Exception {
    entrega.alterarStatus(StatusEntrega.EM_GERACAO);
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));
    when(deltaRepository.findByEntrega_Id(entregaId)).thenReturn(List.of(mockDelta()));
    when(empacotador.empacotar(any(), any(), any(), any()))
        .thenThrow(new BusinessException("falha simulada"));

    service.executar(entregaId);

    assertThat(entrega.getStatus()).isEqualTo(StatusEntrega.FALHA);
    assertThat(entrega.getFalhaMotivo()).contains("falha simulada");
  }

  @Test
  void executar_abortaQuandoStatusInesperado() throws Exception {
    entrega.alterarStatus(StatusEntrega.CANCELADA);
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));

    service.executar(entregaId);

    assertThat(entrega.getStatus()).isEqualTo(StatusEntrega.CANCELADA);
    verify(empacotador, never()).empacotar(any(), any(), any(), any());
  }

  @Test
  void executar_naoQuebraQuandoEntregaDesapareceu() {
    when(entregaRepository.findById(entregaId)).thenReturn(Optional.empty());

    service.executar(entregaId);  // não lança
  }

  private EntregaModuloArtefato mockDelta() {
    EntregaModulo em = new EntregaModulo(entrega, modPortal, "1.4.0", "1.5.0", true, false, 0);
    ArtefatoReleaseModulo art = new ArtefatoReleaseModulo(release, modPortal,
        "x.war", "/tmp/x.war", "sha", 100, null);
    return new EntregaModuloArtefato(em, art, 0);
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
