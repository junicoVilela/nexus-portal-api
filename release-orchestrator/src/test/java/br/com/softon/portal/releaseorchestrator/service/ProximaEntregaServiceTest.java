package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.ProximaEntregaRequest;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.PrioridadeEntrega;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.ProximaEntrega;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.StatusProximaEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
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
class ProximaEntregaServiceTest {

  @Mock OrchestratorProximaEntregaRepository repository;
  @Mock ClienteService clienteService;
  @Mock ProdutoRhRepository produtoRepository;
  @Mock ReleaseRepository releaseRepository;
  @Mock OrchestratorClienteProdutoRepository clienteProdutoRepository;
  @Mock br.com.softon.rbac.service.EscopoResolver escopoResolver;
  @InjectMocks ProximaEntregaService service;

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
    produto = new ProdutoRh("DTEC-LD", "DTECLD", null, "#fff", null, true);
    setId(produto, produtoId);
    release = new Release(produto, "1.5.0", "Release Junho",
        TipoRelease.MINOR, ReleaseStatus.PUBLICADA,
        null, null, null, null);
    setId(release, releaseId);

    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    when(clienteProdutoRepository.existsByCliente_IdAndProduto_Id(clienteId, produtoId))
        .thenReturn(true);
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
  }

  @Test
  void criar_iniciaStatusPlanejada() {
    when(repository.save(any(ProximaEntrega.class))).thenAnswer(inv -> inv.getArgument(0));

    ProximaEntrega p = service.criar(new ProximaEntregaRequest(
        clienteId, produtoId, releaseId, LocalDate.of(2026, 7, 15),
        AmbientePadrao.PROD, PrioridadeEntrega.ALTA, null, "Janela noturna"));

    assertThat(p.getStatus()).isEqualTo(StatusProximaEntrega.PLANEJADA);
    assertThat(p.getCliente()).isEqualTo(cliente);
    assertThat(p.getRelease()).isEqualTo(release);
    assertThat(p.getPrioridade()).isEqualTo(PrioridadeEntrega.ALTA);
  }

  @Test
  void criar_aceitaSemRelease() {
    when(repository.save(any(ProximaEntrega.class))).thenAnswer(inv -> inv.getArgument(0));

    ProximaEntrega p = service.criar(new ProximaEntregaRequest(
        clienteId, produtoId, null, LocalDate.of(2026, 7, 15),
        AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null));

    assertThat(p.getRelease()).isNull();
  }

  @Test
  void criar_rejeitaClienteSemContrato() {
    when(clienteProdutoRepository.existsByCliente_IdAndProduto_Id(clienteId, produtoId))
        .thenReturn(false);

    assertThatThrownBy(() -> service.criar(new ProximaEntregaRequest(
        clienteId, produtoId, null, LocalDate.now(),
        AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("contrata");
  }

  @Test
  void criar_rejeitaReleaseDeOutroProduto() throws Exception {
    ProdutoRh outro = new ProdutoRh("OUTRO", "OUTRO", null, "#fff", null, true);
    setId(outro, UUID.randomUUID());
    Release releaseOutra = new Release(outro, "1.0", "X",
        TipoRelease.MINOR, ReleaseStatus.PUBLICADA, null, null, null, null);
    setId(releaseOutra, releaseId);
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(releaseOutra));

    assertThatThrownBy(() -> service.criar(new ProximaEntregaRequest(
        clienteId, produtoId, releaseId, LocalDate.now(),
        AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Release");
  }

  @Test
  void alterarStatus_transicaoValidaPLANEJADAparaAGENDADA() {
    UUID id = UUID.randomUUID();
    ProximaEntrega p = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    when(repository.findById(id)).thenReturn(Optional.of(p));

    ProximaEntrega upd = service.alterarStatus(id, StatusProximaEntrega.AGENDADA);
    assertThat(upd.getStatus()).isEqualTo(StatusProximaEntrega.AGENDADA);
  }

  @Test
  void alterarStatus_rejeitaCONVERTIDADiretamente() {
    UUID id = UUID.randomUUID();
    ProximaEntrega p = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    when(repository.findById(id)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.alterarStatus(id, StatusProximaEntrega.CONVERTIDA))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("CONVERTIDA");
  }

  @Test
  void alterarStatus_rejeitaTransicaoTerminal() {
    UUID id = UUID.randomUUID();
    ProximaEntrega p = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    p.alterarStatus(StatusProximaEntrega.CANCELADA);
    when(repository.findById(id)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.alterarStatus(id, StatusProximaEntrega.AGENDADA))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Transição inválida");
  }

  @Test
  void atualizar_rejeitaQuandoStatusTerminal() {
    UUID id = UUID.randomUUID();
    ProximaEntrega p = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    p.alterarStatus(StatusProximaEntrega.CANCELADA);
    when(repository.findById(id)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.atualizar(id, new ProximaEntregaRequest(
        clienteId, produtoId, releaseId, LocalDate.now(),
        AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("terminal");
  }

  @Test
  void excluir_rejeitaQuandoConvertida() {
    UUID id = UUID.randomUUID();
    ProximaEntrega p = new ProximaEntrega(cliente, produto, release,
        LocalDate.now(), AmbientePadrao.PROD, PrioridadeEntrega.MEDIA, null, null);
    p.marcarConvertida(UUID.randomUUID());
    when(repository.findById(id)).thenReturn(Optional.of(p));

    assertThatThrownBy(() -> service.excluir(id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("convertida");
    verify(repository, never()).delete(any(ProximaEntrega.class));
  }

  @Test
  void buscar_lancaNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.buscar(id))
        .isInstanceOf(NotFoundException.class);
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
