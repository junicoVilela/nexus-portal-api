package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.SalvarClienteFuncionalidadeRequest;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ClienteFuncionalidadeProduto;
import br.com.softon.portal.releaseorchestrator.entity.DominioProduto;
import br.com.softon.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import br.com.softon.portal.releaseorchestrator.entity.OrigemFuncionalidade;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteFuncionalidadeRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorFuncionalidadeProdutoRepository;
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
class ClienteFuncionalidadeServiceTest {

  @Mock OrchestratorClienteFuncionalidadeRepository repository;
  @Mock OrchestratorFuncionalidadeProdutoRepository funcionalidadeRepository;
  @Mock ClienteService clienteService;
  @Mock br.com.softon.rbac.service.EscopoResolver escopoResolver;
  @InjectMocks ClienteFuncionalidadeService service;

  Cliente cliente;
  FuncionalidadeProduto funcionalidade;
  final UUID clienteId = UUID.randomUUID();
  final UUID funcionalidadeId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(java.util.Optional.empty());
    org.mockito.Mockito.when(escopoResolver.podeAcessarCliente(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    org.mockito.Mockito.doNothing().when(escopoResolver).assertPodeEscreverEmCliente(org.mockito.ArgumentMatchers.any());
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    ProdutoRh produto = new ProdutoRh("DTEC", "DTEC", null, "#fff", null, true);
    DominioProduto dominio = new DominioProduto(produto, "Usuários", "usuarios", null, null, 0);
    funcionalidade = new FuncionalidadeProduto(
        dominio, "Inserir", "inserir", null, null, null, false, 0);
    when(clienteService.buscar(clienteId)).thenReturn(cliente);
    when(funcionalidadeRepository.findById(funcionalidadeId))
        .thenReturn(Optional.of(funcionalidade));
  }

  @Test
  void salvar_criaVinculoNovoComOrigemMANUALPadrao() {
    when(repository.findByCliente_IdAndFuncionalidade_Id(clienteId, funcionalidadeId))
        .thenReturn(Optional.empty());
    when(repository.save(any(ClienteFuncionalidadeProduto.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ClienteFuncionalidadeProduto cf = service.salvar(clienteId, funcionalidadeId,
        new SalvarClienteFuncionalidadeRequest(true, null));

    assertThat(cf.isHabilitada()).isTrue();
    assertThat(cf.getOrigem()).isEqualTo(OrigemFuncionalidade.MANUAL);
    verify(repository).save(any(ClienteFuncionalidadeProduto.class));
  }

  @Test
  void salvar_atualizaVinculoExistenteSemSave() {
    ClienteFuncionalidadeProduto existente = new ClienteFuncionalidadeProduto(
        cliente, funcionalidade, false, OrigemFuncionalidade.TEMPLATE);
    when(repository.findByCliente_IdAndFuncionalidade_Id(clienteId, funcionalidadeId))
        .thenReturn(Optional.of(existente));

    ClienteFuncionalidadeProduto cf = service.salvar(clienteId, funcionalidadeId,
        new SalvarClienteFuncionalidadeRequest(true, OrigemFuncionalidade.MANUAL));

    assertThat(cf.isHabilitada()).isTrue();
    assertThat(cf.getOrigem()).isEqualTo(OrigemFuncionalidade.MANUAL);
    verify(repository, never()).save(any(ClienteFuncionalidadeProduto.class));
  }

  @Test
  void salvar_respeitaOrigemExplicita() {
    when(repository.findByCliente_IdAndFuncionalidade_Id(clienteId, funcionalidadeId))
        .thenReturn(Optional.empty());
    when(repository.save(any(ClienteFuncionalidadeProduto.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ClienteFuncionalidadeProduto cf = service.salvar(clienteId, funcionalidadeId,
        new SalvarClienteFuncionalidadeRequest(true, OrigemFuncionalidade.HERDADA));

    assertThat(cf.getOrigem()).isEqualTo(OrigemFuncionalidade.HERDADA);
  }

  @Test
  void salvar_falhaQuandoFuncionalidadeInexistente() {
    when(funcionalidadeRepository.findById(funcionalidadeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.salvar(clienteId, funcionalidadeId,
        new SalvarClienteFuncionalidadeRequest(true, null)))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Funcionalidade");
  }

  @Test
  void remover_excluiVinculoExistente() {
    ClienteFuncionalidadeProduto cf = new ClienteFuncionalidadeProduto(
        cliente, funcionalidade, true, OrigemFuncionalidade.MANUAL);
    when(repository.findByCliente_IdAndFuncionalidade_Id(clienteId, funcionalidadeId))
        .thenReturn(Optional.of(cf));

    service.remover(clienteId, funcionalidadeId);
    verify(repository).delete(cf);
  }

  @Test
  void remover_lancaNotFoundQuandoSemVinculo() {
    when(repository.findByCliente_IdAndFuncionalidade_Id(clienteId, funcionalidadeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.remover(clienteId, funcionalidadeId))
        .isInstanceOf(NotFoundException.class);
  }
}
