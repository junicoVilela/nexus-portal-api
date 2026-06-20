package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.AtualizarEntregaRascunhoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.CriarEntregaRequest;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.PrioridadeEntrega;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.ProximaEntrega;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.StatusEntrega;
import br.com.softon.portal.releaseorchestrator.entity.StatusProximaEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorProximaEntregaRepository;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
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
  @Mock ClienteService clienteService;
  @Mock ProdutoRhRepository produtoRepository;
  @Mock ReleaseRepository releaseRepository;
  @Mock OrchestratorClienteProdutoRepository clienteProdutoRepository;
  @InjectMocks EntregaService service;

  Cliente cliente;
  ProdutoRh produto;
  Release release;
  final UUID clienteId = UUID.randomUUID();
  final UUID produtoId = UUID.randomUUID();
  final UUID releaseId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, clienteId);
    produto = new ProdutoRh("DTEC-LD", "DTECLD", null, "#fff", null, true);
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
        null, "Janela 22h", null));

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
        peId, null, null, null, null, null, null, null));

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
        peId, null, null, null, null, null, null, null)))
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
        peId, null, null, null, null, null, null, null)))
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
        peId, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("release");
  }

  @Test
  void criar_avulsoExigeCamposObrigatorios() {
    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        null, null, null, null, null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void criar_rejeitaClienteSemContrato() {
    when(clienteProdutoRepository.existsByCliente_IdAndProduto_Id(clienteId, produtoId))
        .thenReturn(false);

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, null, null)))
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
        null, null, origemId));

    assertThat(e.getEntregaOriginalId()).isEqualTo(origemId);
  }

  @Test
  void criar_reentregaRejeitaEntregaOriginalInexistente() {
    UUID origemId = UUID.randomUUID();
    when(repository.existsById(origemId)).thenReturn(false);

    assertThatThrownBy(() -> service.criar(new CriarEntregaRequest(
        null, clienteId, produtoId, releaseId, AmbientePadrao.PROD,
        null, null, origemId)))
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
  void cancelar_rejeitaQuandoConcluida() {
    UUID id = UUID.randomUUID();
    Entrega e = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    e.alterarStatus(StatusEntrega.CONCLUIDA);
    when(repository.findById(id)).thenReturn(Optional.of(e));

    assertThatThrownBy(() -> service.cancelar(id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("terminal");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
