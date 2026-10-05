package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.ManualSinonimo;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ManualSinonimoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManualSinonimoServiceTest {

  private final ManualSinonimoRepository repository = mock(ManualSinonimoRepository.class);
  private final ClienteRepository clientes = mock(ClienteRepository.class);
  private final UUID clienteId = UUID.randomUUID();
  private ManualSinonimoService service;

  @BeforeEach
  void setUp() {
    service = new ManualSinonimoService(repository, clientes, mock(EscopoResolver.class));
    when(clientes.findById(clienteId)).thenReturn(Optional.of(new Cliente("ACME", "acme", true)));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void criaGrupoSemRepetirTermoEquivalente() {
    ManualSinonimo grupo = service.criar(clienteId, List.of(" nota  fiscal ", "NF", "Nota Fiscal", ""), () -> "ana");

    assertThat(grupo.getTermos()).containsExactly("nota fiscal", "NF");
    assertThat(grupo.getCreatedBy()).isEqualTo("ana");
  }

  @Test
  void exigeDoisTermosDiferentes() {
    assertThatThrownBy(() -> service.criar(clienteId, List.of("NF", "nf"), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("pelo menos dois termos");
  }

  @Test
  void termoSoPodeEstarEmUmGrupoDoCliente() {
    var existente = new ManualSinonimo(clienteId, List.of("nota fiscal", "NF"), null);
    when(repository.findByClienteIdOrderByCreatedAtAsc(clienteId)).thenReturn(List.of(existente));

    assertThatThrownBy(() -> service.criar(clienteId, List.of("nf", "cupom"), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("\"nf\" já está no grupo nota fiscal, NF");
  }

  @Test
  void editarOProprioGrupoNaoConflitaComEleMesmo() {
    var existente = new ManualSinonimo(clienteId, List.of("nota fiscal", "NF"), null);
    when(repository.findById(existente.getId())).thenReturn(Optional.of(existente));
    when(repository.findByClienteIdOrderByCreatedAtAsc(clienteId)).thenReturn(List.of(existente));

    service.atualizar(existente.getId(), List.of("nota fiscal", "NF", "NF-e"));

    assertThat(existente.getTermos()).containsExactly("nota fiscal", "NF", "NF-e");
  }

  @Test
  void gruposSaemNormalizadosParaABusca() {
    when(repository.findByClienteIdOrderByCreatedAtAsc(clienteId))
        .thenReturn(List.of(new ManualSinonimo(clienteId, List.of("Nota Fiscal", "NF-e"), null)));

    assertThat(service.grupos(clienteId)).containsExactly(List.of("nota fiscal", "nf e"));
    assertThat(service.grupos(null)).isEmpty();
  }
}
