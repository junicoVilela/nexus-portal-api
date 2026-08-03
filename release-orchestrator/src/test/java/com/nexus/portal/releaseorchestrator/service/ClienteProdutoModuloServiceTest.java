package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.SalvarClienteProdutoModuloRequest;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.ClienteProduto;
import com.nexus.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteProdutoModuloRepository;
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
class ClienteProdutoModuloServiceTest {

  @Mock OrchestratorClienteProdutoModuloRepository repository;
  @Mock ClienteProdutoService clienteProdutoService;
  @Mock ModuloProdutoRepository moduloRepository;
  @Mock com.nexus.identityaccess.service.EscopoResolver escopoResolver;
  @InjectMocks ClienteProdutoModuloService service;

  Cliente cliente;
  ProdutoRh produto;
  ClienteProduto clienteProduto;
  ModuloProduto modulo;
  final UUID clienteId = UUID.randomUUID();
  final UUID clienteProdutoId = UUID.randomUUID();
  final UUID moduloId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    org.mockito.Mockito.when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(java.util.Optional.empty());
    org.mockito.Mockito.when(escopoResolver.podeAcessarCliente(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    org.mockito.Mockito.doNothing().when(escopoResolver).assertPodeEscreverEmCliente(org.mockito.ArgumentMatchers.any());
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());
    clienteProduto = new ClienteProduto(cliente, produto, AmbientePadrao.PROD);
    modulo = new ModuloProduto(produto, "nexus-portal", "Portal", TipoModulo.WEB,
        false, true, 1, null);
    setId(modulo, moduloId);

    when(clienteProdutoService.buscar(clienteId, clienteProdutoId)).thenReturn(clienteProduto);
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));
  }

  @Test
  void salvar_criaVinculoNovoComVersaoAtual() {
    when(repository.findByClienteProduto_IdAndModuloProduto_Id(clienteProdutoId, moduloId))
        .thenReturn(Optional.empty());
    when(repository.save(any(ClienteProdutoModulo.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ClienteProdutoModulo cpm = service.salvar(clienteId, clienteProdutoId, moduloId,
        new SalvarClienteProdutoModuloRequest("1.4.0", null));

    assertThat(cpm.getVersaoAtual()).isEqualTo("1.4.0");
    assertThat(cpm.isAtivo()).isTrue();
    verify(repository).save(any(ClienteProdutoModulo.class));
  }

  @Test
  void salvar_atualizaVinculoExistente() {
    ClienteProdutoModulo existente = new ClienteProdutoModulo(clienteProduto, modulo, "1.3.0");
    when(repository.findByClienteProduto_IdAndModuloProduto_Id(clienteProdutoId, moduloId))
        .thenReturn(Optional.of(existente));

    ClienteProdutoModulo cpm = service.salvar(clienteId, clienteProdutoId, moduloId,
        new SalvarClienteProdutoModuloRequest("1.5.0", false));

    assertThat(cpm.getVersaoAtual()).isEqualTo("1.5.0");
    assertThat(cpm.isAtivo()).isFalse();
    verify(repository, never()).save(any(ClienteProdutoModulo.class));
  }

  @Test
  void salvar_rejeitaModuloDeOutroProduto() throws Exception {
    ProdutoRh outro = new ProdutoRh("OUTRO", "OUTRO", null, "#fff", null, true);
    setId(outro, UUID.randomUUID());
    ModuloProduto moduloDeOutro = new ModuloProduto(outro, "x", "X",
        TipoModulo.WEB, false, true, 1, null);
    setId(moduloDeOutro, moduloId);
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(moduloDeOutro));

    assertThatThrownBy(() -> service.salvar(clienteId, clienteProdutoId, moduloId,
        new SalvarClienteProdutoModuloRequest("1.0", null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não pertence");
  }

  @Test
  void remover_excluiVinculoExistente() {
    ClienteProdutoModulo cpm = new ClienteProdutoModulo(clienteProduto, modulo, "1.4.0");
    when(repository.findByClienteProduto_IdAndModuloProduto_Id(clienteProdutoId, moduloId))
        .thenReturn(Optional.of(cpm));

    service.remover(clienteId, clienteProdutoId, moduloId);
    verify(repository).delete(cpm);
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
