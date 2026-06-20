package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.AtualizarClienteProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ContratarProdutoRequest;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ClienteProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
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
class ClienteProdutoServiceTest {

  @Mock OrchestratorClienteProdutoRepository repository;
  @Mock ProdutoRhRepository produtoRepository;
  @Mock ClienteService clienteService;
  @InjectMocks ClienteProdutoService service;

  Cliente cliente;
  ProdutoRh produto;
  final UUID clienteId = UUID.randomUUID();
  final UUID produtoId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    produto = new ProdutoRh("DTEC-LD", "DTECLD", null, "#fff", null, true);
    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
  }

  @Test
  void contratar_criaVinculoNovo() {
    when(repository.existsByCliente_IdAndProduto_Id(clienteId, produtoId)).thenReturn(false);
    when(repository.save(any(ClienteProduto.class))).thenAnswer(inv -> inv.getArgument(0));

    ClienteProduto cp = service.contratar(clienteId,
        new ContratarProdutoRequest(produtoId, AmbientePadrao.HOM));

    assertThat(cp.getAmbiente()).isEqualTo(AmbientePadrao.HOM);
    assertThat(cp.isAtivo()).isTrue();
  }

  @Test
  void contratar_falhaDuplicado() {
    when(repository.existsByCliente_IdAndProduto_Id(clienteId, produtoId)).thenReturn(true);

    assertThatThrownBy(() -> service.contratar(clienteId,
        new ContratarProdutoRequest(produtoId, AmbientePadrao.PROD)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("contrata");
  }

  @Test
  void contratar_falhaProdutoInexistente() {
    when(repository.existsByCliente_IdAndProduto_Id(any(), any())).thenReturn(false);
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.contratar(clienteId,
        new ContratarProdutoRequest(produtoId, AmbientePadrao.PROD)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void atualizar_mudaAmbienteEAtivo() {
    UUID id = UUID.randomUUID();
    ClienteProduto cp = new ClienteProduto(cliente, produto, AmbientePadrao.PROD);
    when(repository.findByCliente_IdAndId(clienteId, id)).thenReturn(Optional.of(cp));

    ClienteProduto upd = service.atualizar(clienteId, id,
        new AtualizarClienteProdutoRequest(AmbientePadrao.HOM, false));

    assertThat(upd.getAmbiente()).isEqualTo(AmbientePadrao.HOM);
    assertThat(upd.isAtivo()).isFalse();
  }

  @Test
  void atualizar_mantemAtivoQuandoNulo() {
    UUID id = UUID.randomUUID();
    ClienteProduto cp = new ClienteProduto(cliente, produto, AmbientePadrao.PROD);
    when(repository.findByCliente_IdAndId(clienteId, id)).thenReturn(Optional.of(cp));

    ClienteProduto upd = service.atualizar(clienteId, id,
        new AtualizarClienteProdutoRequest(AmbientePadrao.HOM, null));

    assertThat(upd.isAtivo()).isTrue();
  }

  @Test
  void rescindir_excluiContrato() {
    UUID id = UUID.randomUUID();
    ClienteProduto cp = new ClienteProduto(cliente, produto, AmbientePadrao.PROD);
    when(repository.findByCliente_IdAndId(clienteId, id)).thenReturn(Optional.of(cp));

    service.rescindir(clienteId, id);
    verify(repository).delete(cp);
  }
}
