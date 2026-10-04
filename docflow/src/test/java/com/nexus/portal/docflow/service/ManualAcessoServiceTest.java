package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.ManualAcesso;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ManualAcessoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManualAcessoServiceTest {

  private final ManualAcessoRepository repository = mock(ManualAcessoRepository.class);
  private final ClienteRepository clientes = mock(ClienteRepository.class);
  private final UUID clienteId = UUID.randomUUID();
  private ManualAcessoService service;

  @BeforeEach
  void setUp() {
    service = new ManualAcessoService(repository, clientes, mock(EscopoResolver.class), 2);
    when(clientes.findById(clienteId)).thenReturn(Optional.of(new Cliente("ACME", "acme", true)));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private ManualAcessoService.Criada criar(List<String> origens) {
    return service.criar(clienteId, "NEXUS-LD", origens, null, () -> "ana");
  }

  @Test
  void criaTokenNxmGuardandoSoOHashEOrigensNormalizadas() {
    var criada = criar(List.of("https://App.Acme.com/", "https://app.acme.com", "http://localhost:4200"));

    assertThat(criada.token()).startsWith("nxm_").hasSizeGreaterThan(30);
    assertThat(criada.acesso().getTokenHash()).isEqualTo(ManualAcessoService.hash(criada.token()))
        .doesNotContain(criada.token());
    assertThat(criada.acesso().getPrefixo()).isEqualTo(criada.token().substring(0, 12));
    assertThat(criada.acesso().getOrigens()).containsExactly("https://app.acme.com", "http://localhost:4200");
    assertThat(criada.acesso().getExpiraEm()).isNull();
  }

  @Test
  void recusaOrigemQueNaoEhOrigem() {
    assertThatThrownBy(() -> criar(List.of("https://app.acme.com/ajuda"))).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> criar(List.of("ftp://app.acme.com"))).isInstanceOf(BusinessException.class);
  }

  @Test
  void validaChaveOrigemRevogacaoELimite() {
    var criada = criar(List.of("https://app.acme.com"));
    ManualAcesso acesso = criada.acesso();
    when(repository.findByTokenHash(acesso.getTokenHash())).thenReturn(Optional.of(acesso));

    assertThat(service.clienteDaChave(criada.token(), "https://app.acme.com")).isEqualTo(clienteId);
    assertThat(service.clienteDaChave(criada.token(), null)).as("servidor a servidor").isEqualTo(clienteId);
    assertThatThrownBy(() -> service.clienteDaChave(criada.token(), "https://outro.com"))
        .isInstanceOf(NotFoundException.class).hasMessageContaining("Origem");
    assertThatThrownBy(() -> service.clienteDaChave(criada.token(), null))
        .as("limite de 2 por minuto").isInstanceOf(BusinessException.class);

    acesso.revogar("ana");
    assertThatThrownBy(() -> service.clienteDaChave(criada.token(), null)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void chaveDesconhecidaNaoEntra() {
    when(repository.findByTokenHash(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.clienteDaChave("nxm_x", null)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void origensDoCorsVemDasChavesAtivas() {
    var criada = criar(List.of("https://app.acme.com"));
    when(repository.findByAtivoTrue()).thenReturn(List.of(criada.acesso()));
    assertThat(service.origens()).containsExactly("https://app.acme.com");
  }
}
