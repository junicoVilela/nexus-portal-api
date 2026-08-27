package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.AtualizarEntregaRascunhoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.CriarEntregaRequest;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.PrioridadeEntrega;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.ProximaEntrega;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.StatusEntrega;
import com.nexus.portal.releaseorchestrator.entity.StatusProximaEntrega;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaModuloArtefatoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorProximaEntregaRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.time.LocalDate;
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
class EntregaServiceTest {

  @Mock OrchestratorEntregaRepository repository;
  @Mock OrchestratorProximaEntregaRepository proximaEntregaRepository;
  @Mock OrchestratorEntregaModuloRepository entregaModuloRepository;
  @Mock OrchestratorEntregaModuloArtefatoRepository deltaRepository;
  @Mock ClienteService clienteService;
  @Mock ProdutoRhRepository produtoRepository;
  @Mock ReleaseRepository releaseRepository;
  @Mock OrchestratorClienteProdutoRepository clienteProdutoRepository;
  @Mock OrchestratorInstalacaoClienteRepository instalacaoRepository;
  @Mock com.nexus.identityaccess.service.EscopoResolver escopoResolver;
  @InjectMocks EntregaService service;

  Cliente cliente;
  ProdutoRh produto;
  Release release;
  final UUID clienteId = UUID.randomUUID();
  final UUID produtoId = UUID.randomUUID();
  final UUID releaseId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    org.mockito.Mockito.when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(java.util.Optional.empty());
    org.mockito.Mockito.when(escopoResolver.podeAcessarCliente(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    org.mockito.Mockito.doNothing().when(escopoResolver).assertPodeEscreverEmCliente(org.mockito.ArgumentMatchers.any());
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, clienteId);
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    setId(produto, produtoId);
    release = new Release(produto, "1.5.0", "Release Junho",
        TipoRelease.MINOR, ReleaseStatus.PUBLICADA, null, null, null, null);
    setId(release, releaseId);

    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(clienteProdutoRepository.existsByCliente_IdAndProduto_Id(clienteId, produtoId))
        .thenReturn(true);
  }

  @Test
  void criar_avulsoSemProximaEntrega() {
    when(repository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));

    Entrega e = service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, "Janela 22h", null, null));

    assertThat(e.getStatus()).isEqualTo(StatusEntrega.RASCUNHO);
    assertThat(e.getCliente()).isEqualTo(cliente);
    assertThat(e.getProduto()).isEqualTo(produto);
    assertThat(e.getRelease()).isEqualTo(release);
    assertThat(e.getProximaEntregaId()).isNull();
  }

  @Test
  void criar_convertendoProximaEntrega() throws Exception {
    UUID peId = UUID.randomUUID();
    ProximaEntrega pe = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.HOM, PrioridadeEntrega.ALTA, null, "obs");
    setId(pe, peId);
    when(proximaEntregaRepository.findById(peId)).thenReturn(Optional.of(pe));
    when(repository.save(any(Entrega.class))).thenAnswer(inv -> {
      Entrega e = inv.getArgument(0);
      try {
        Field f = Entrega.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(e, UUID.randomUUID());
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      return e;
    });

    Entrega e = service.criar(new CriarEntregaRequest(
        peId, null, null, null, null, null, null, null, null));

    assertThat(e.getStatus()).isEqualTo(StatusEntrega.RASCUNHO);
    assertThat(e.getAmbiente()).isEqualTo(AmbientePadrao.HOM);  // veio do PE
    assertThat(e.getProximaEntregaId()).isEqualTo(peId);
    // ProximaEntrega ficou CONVERTIDA + entregaConvertidaId set
    assertThat(pe.getStatus()).isEqualTo(StatusProximaEntrega.CONVERTIDA);
    assertThat(pe.getEntregaConvertidaId()).isNotNull();
  }

  @Test
  void criar_rejeitaProximaEntregaJaConvertida() {
    UUID peId = UUID.randomUUID();
    ProximaEntrega pe = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    pe.marcarConvertida(UUID.randomUUID());
    when(proximaEntregaRepository.findById(peId)).thenReturn(Optional.of(pe));

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        peId, null, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("convertida");
  }

  @Test
  void criar_rejeitaProximaEntregaCancelada() {
    UUID peId = UUID.randomUUID();
    ProximaEntrega pe = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    pe.alterarStatus(StatusProximaEntrega.CANCELADA);
    when(proximaEntregaRepository.findById(peId)).thenReturn(Optional.of(pe));

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        peId, null, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("cancelada");
  }

  @Test
  void criar_rejeitaProximaEntregaSemRelease() {
    UUID peId = UUID.randomUUID();
    ProximaEntrega pe = new ProximaEntrega(cliente, produto, null,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    when(proximaEntregaRepository.findById(peId)).thenReturn(Optional.of(pe));

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        peId, null, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("release");
  }

  @Test
  void criar_avulsoExigeCamposObrigatorios() {
    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        null, null, null, null, null, null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void criar_rejeitaClienteSemContrato() {
    when(clienteProdutoRepository.existsByCliente_IdAndProduto_Id(clienteId, produtoId))
        .thenReturn(false);

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("contrata");
  }

  @Test
  void criar_reentregaValidaEntregaOriginalExiste() {
    UUID origemId = UUID.randomUUID();
    when(repository.existsById(origemId)).thenReturn(true);
    when(repository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));

    Entrega e = service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, null, origemId, null));

    assertThat(e.getEntregaOriginalId()).isEqualTo(origemId);
  }

  @Test
  void criar_reentregaRejeitaEntregaOriginalInexistente() {
    UUID origemId = UUID.randomUUID();
    when(repository.existsById(origemId)).thenReturn(false);

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, null, origemId, null)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void atualizarRascunho_rejeitaQuandoNaoEditavel() {
    UUID id = UUID.randomUUID();
    Entrega e = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    e.alterarStatus(StatusEntrega.CONCLUIDA);
    when(repository.findById(id)).thenReturn(Optional.of(e));

    assertThatThrownBy(() -> service.atualizarRascunho(id,
        new AtualizarEntregaRascunhoRequest(AmbientePadrao.HOM, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("editável");
  }

  @Test
  void cancelar_rascunho() {
    UUID id = UUID.randomUUID();
    Entrega e = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    when(repository.findById(id)).thenReturn(Optional.of(e));

    Entrega cancelada = service.cancelar(id);
    assertThat(cancelada.getStatus()).isEqualTo(StatusEntrega.CANCELADA);
    assertThat(cancelada.getDataConclusao()).isNotNull();
  }

  @Test
  void reentregar_clonaCamposBasicosEMarcaEntregaOriginalId() throws Exception {
    UUID origemId = UUID.randomUUID();
    Entrega origem = new Entrega(cliente, produto, release, AmbientePadrao.HOM,
        UUID.randomUUID(), "Entrega original");
    origem.alterarStatus(StatusEntrega.CONCLUIDA);
    setId(origem, origemId);
    when(repository.findById(origemId)).thenReturn(Optional.of(origem));
    when(repository.save(any(Entrega.class))).thenAnswer(inv -> {
      Entrega e = inv.getArgument(0);
      setId(e, UUID.randomUUID());
      return e;
    });
    when(entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(origemId))
        .thenReturn(java.util.List.of());
    when(deltaRepository.findByEntrega_Id(origemId)).thenReturn(java.util.List.of());

    Entrega nova = service.reentregar(origemId);

    assertThat(nova.getEntregaOriginalId()).isEqualTo(origemId);
    assertThat(nova.getCliente()).isEqualTo(cliente);
    assertThat(nova.getProduto()).isEqualTo(produto);
    assertThat(nova.getRelease()).isEqualTo(release);
    assertThat(nova.getAmbiente()).isEqualTo(AmbientePadrao.HOM);
    assertThat(nova.getStatus()).isEqualTo(StatusEntrega.RASCUNHO);
    assertThat(nova.getObservacoes()).isEqualTo("Entrega original");
  }

  @Test
  void reentregar_rejeitaQuandoOriginalNaoEstaEmEstadoTerminal() throws Exception {
    UUID origemId = UUID.randomUUID();
    Entrega origem = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    origem.marcarEmGeracao();
    setId(origem, origemId);
    when(repository.findById(origemId)).thenReturn(Optional.of(origem));

    assertThatThrownBy(() -> service.reentregar(origemId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("terminal");
  }

  @Test
  void reentregar_lancaNotFoundQuandoOriginalInexistente() {
    UUID origemId = UUID.randomUUID();
    when(repository.findById(origemId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reentregar(origemId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void cancelar_rejeitaQuandoConcluida() {
    UUID id = UUID.randomUUID();
    Entrega e = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    e.alterarStatus(StatusEntrega.CONCLUIDA);
    when(repository.findById(id)).thenReturn(Optional.of(e));

    assertThatThrownBy(() -> service.cancelar(id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("terminal");
  }

  @Test
  void criar_vinculaInstalacoesDoMesmoClienteProdutoAmbiente() throws Exception {
    UUID instId = UUID.randomUUID();
    Host host = new Host("SRV-LIN-01", "Linux", "lin-01",
        SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    setId(host, UUID.randomUUID());
    InstalacaoCliente inst = new InstalacaoCliente(
        "ACME-RPA-01", "RPA", cliente, host, produto,
        TipoImplantacao.DOCKER_PULL, AmbientePadrao.PROD);
    setId(inst, instId);
    when(instalacaoRepository.findById(instId)).thenReturn(Optional.of(inst));
    when(repository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));

    Entrega e = service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, null, null, java.util.List.of(instId)));

    assertThat(e.getAlvos()).hasSize(1);
    assertThat(e.getAlvos().get(0).getInstalacao()).isEqualTo(inst);
  }

  @Test
  void criar_rejeitaInstalacaoDeOutroCliente() throws Exception {
    UUID instId = UUID.randomUUID();
    Cliente outro = new Cliente("OUTRO", "Outro", AmbientePadrao.PROD);
    setId(outro, UUID.randomUUID());
    Host host = new Host("SRV-LIN-01", "Linux", "lin-01",
        SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    setId(host, UUID.randomUUID());
    InstalacaoCliente inst = new InstalacaoCliente(
        "OUTRO-RPA", "RPA", outro, host, produto,
        TipoImplantacao.DOCKER_PULL, AmbientePadrao.PROD);
    setId(inst, instId);
    when(instalacaoRepository.findById(instId)).thenReturn(Optional.of(inst));

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, null, null, java.util.List.of(instId))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não pertence ao cliente");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
