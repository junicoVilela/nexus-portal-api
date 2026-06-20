package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.ContatoRequest;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.Contato;
import br.com.softon.portal.releaseorchestrator.entity.PapelContato;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorContatoRepository;
import br.com.softon.portal.shared.exception.NotFoundException;
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
class ContatoServiceTest {

  @Mock OrchestratorContatoRepository repository;
  @Mock ClienteService clienteService;
  @InjectMocks ContatoService service;

  Cliente cliente;
  final UUID clienteId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    when(clienteService.buscar(clienteId)).thenReturn(cliente);
  }

  @Test
  void criar_normalizaEmailLowercase() {
    when(repository.save(any(Contato.class))).thenAnswer(inv -> inv.getArgument(0));

    Contato c = service.criar(clienteId, new ContatoRequest(
        "Ana Silva", PapelContato.TECNICO, "Ana.Silva@Acme.COM", "11999998888"));

    assertThat(c.getNome()).isEqualTo("Ana Silva");
    assertThat(c.getEmail()).isEqualTo("ana.silva@acme.com");
    assertThat(c.getPapel()).isEqualTo(PapelContato.TECNICO);
    assertThat(c.getTelefone()).isEqualTo("11999998888");
  }

  @Test
  void atualizar_modificaCampos() {
    UUID id = UUID.randomUUID();
    Contato existente = new Contato(cliente, "Old", PapelContato.OUTRO, "old@x.com", null);
    when(repository.findByCliente_IdAndId(clienteId, id)).thenReturn(Optional.of(existente));

    Contato c = service.atualizar(clienteId, id, new ContatoRequest(
        "New Name", PapelContato.COMERCIAL, "new@y.com", "11111111111"));

    assertThat(c.getNome()).isEqualTo("New Name");
    assertThat(c.getPapel()).isEqualTo(PapelContato.COMERCIAL);
    assertThat(c.getEmail()).isEqualTo("new@y.com");
  }

  @Test
  void listar_chamaRepositoryOrdenado() {
    Contato a = new Contato(cliente, "Ana", PapelContato.TECNICO, "ana@x.com", null);
    Contato b = new Contato(cliente, "Bob", PapelContato.COMERCIAL, "bob@x.com", null);
    when(repository.findByCliente_IdOrderByNomeAsc(clienteId)).thenReturn(List.of(a, b));

    assertThat(service.listar(clienteId))
        .extracting(Contato::getNome).containsExactly("Ana", "Bob");
  }

  @Test
  void buscar_lancaNotFoundQuandoOutroCliente() {
    UUID id = UUID.randomUUID();
    when(repository.findByCliente_IdAndId(clienteId, id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(clienteId, id))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void excluir_chamaDelete() {
    UUID id = UUID.randomUUID();
    Contato c = new Contato(cliente, "Ana", PapelContato.TECNICO, "ana@x.com", null);
    when(repository.findByCliente_IdAndId(clienteId, id)).thenReturn(Optional.of(c));

    service.excluir(clienteId, id);
    verify(repository).delete(c);
  }
}
