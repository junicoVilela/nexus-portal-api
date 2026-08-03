package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.AtualizarClienteProdutoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ContratarProdutoRequest;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.ClienteProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
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
  @Mock com.nexus.identityaccess.service.EscopoResolver escopoResolver;
  @InjectMocks ClienteProdutoService service;

  Cliente cliente;
  ProdutoRh produto;
  final UUID clienteId = UUID.randomUUID();
  final UUID produtoId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(java.util.Optional.empty());
    org.mockito.Mockito.when(escopoResolver.podeAcessarCliente(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    org.mockito.Mockito.doNothing().when(escopoResolver).assertPodeEscreverEmCliente(org.mockito.ArgumentMatchers.any());
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
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
