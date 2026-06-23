package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.ConfigEntregaRequest;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import br.com.softon.portal.releaseorchestrator.integration.publish.PublishService;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorConfigEntregaRepository;
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
class ConfigEntregaServiceTest {

  @Mock OrchestratorConfigEntregaRepository repository;
  @Mock ClienteService clienteService;
  @Mock EncryptionService encryptionService;
  @Mock PublishService publishService;
  @InjectMocks ConfigEntregaService service;

  Cliente cliente;
  final UUID clienteId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    when(clienteService.buscar(clienteId)).thenReturn(cliente);
  }

  @Test
  void salvar_criaConfigNovaQuandoNaoExiste() {
    when(repository.findByCliente_Id(clienteId)).thenReturn(Optional.empty());
    when(repository.save(any(ConfigEntrega.class))).thenAnswer(inv -> inv.getArgument(0));

    ConfigEntrega c = service.salvar(clienteId, new ConfigEntregaRequest(
        TipoDestinoEntrega.PASTA, "/var/lib/softon/entregas/acme", true,
        "ops@acme.com",
        null, null, null, null, null, null, null, null, null, null));

    assertThat(c.getTipoDestino()).isEqualTo(TipoDestinoEntrega.PASTA);
    assertThat(c.getCaminhoBase()).isEqualTo("/var/lib/softon/entregas/acme");
    assertThat(c.isExigirAprovacao()).isTrue();
    assertThat(c.getEmailsNotificacao()).isEqualTo("ops@acme.com");
    verify(repository).save(any(ConfigEntrega.class));
  }

  @Test
  void salvar_atualizaInPlaceQuandoJaExiste() {
    ConfigEntrega existente = new ConfigEntrega(cliente, TipoDestinoEntrega.PASTA, "/x");
    when(repository.findByCliente_Id(clienteId)).thenReturn(Optional.of(existente));

    ConfigEntrega c = service.salvar(clienteId, new ConfigEntregaRequest(
        TipoDestinoEntrega.PASTA, "/var/lib/softon/entregas/acme", false, null,
        null, null, null, null, null, null, null, null, null, null));

    assertThat(c.getCaminhoBase()).isEqualTo("/var/lib/softon/entregas/acme");
    verify(repository, never()).save(any(ConfigEntrega.class));
  }

  @Test
  void salvar_rejeitaFtpSemHost() {
    assertThatThrownBy(() -> service.salvar(clienteId, new ConfigEntregaRequest(
        TipoDestinoEntrega.FTP, "/x", false, null,
        null, null, null, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Host");
  }

  @Test
  void salvar_rejeitaCaminhoVazio() {
    assertThatThrownBy(() -> service.salvar(clienteId, new ConfigEntregaRequest(
        TipoDestinoEntrega.PASTA, "", false, null,
        null, null, null, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("obrigatório");
  }

  @Test
  void salvar_rejeitaPathTraversal() {
    assertThatThrownBy(() -> service.salvar(clienteId, new ConfigEntregaRequest(
        TipoDestinoEntrega.PASTA, "/var/../etc", false, null,
        null, null, null, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("..");
  }

  @Test
  void salvar_rejeitaPathRelativo() {
    assertThatThrownBy(() -> service.salvar(clienteId, new ConfigEntregaRequest(
        TipoDestinoEntrega.PASTA, "relativo/path", false, null,
        null, null, null, null, null, null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("absoluto");
  }

  @Test
  void buscar_lancaNotFoundQuandoSemConfig() {
    when(repository.findByCliente_Id(clienteId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.buscar(clienteId))
        .isInstanceOf(NotFoundException.class);
  }
}
