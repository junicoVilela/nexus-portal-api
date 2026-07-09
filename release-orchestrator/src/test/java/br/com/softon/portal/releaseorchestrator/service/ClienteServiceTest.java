package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.ClienteRequest;
import br.com.softon.portal.releaseorchestrator.entity.AmbientePadrao;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.TipoBanco;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorClienteRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteServiceTest {

  @Mock OrchestratorClienteRepository repository;
  @Mock br.com.softon.rbac.service.EscopoResolver escopoResolver;
  @InjectMocks ClienteService service;

  @org.junit.jupiter.api.BeforeEach
  void escopoAberto() {
    when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(java.util.Optional.empty());
    when(escopoResolver.podeAcessarCliente(any())).thenReturn(true);
    org.mockito.Mockito.doNothing().when(escopoResolver).assertPodeEscreverEmCliente(any());
  }

  @Test
  void criar_normalizaSiglaUppercase() {
    ClienteRequest req = req("ACME", "acme", AmbientePadrao.PROD, null);
    when(repository.existsBySiglaIgnoreCase("ACME")).thenReturn(false);
    when(repository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

    Cliente c = service.criar(req);
    assertThat(c.getSigla()).isEqualTo("ACME");
    assertThat(c.getAmbientePadrao()).isEqualTo(AmbientePadrao.PROD);
    assertThat(c.isAtivo()).isTrue();
  }

  @Test
  void criar_falhaSiglaDuplicada() {
    when(repository.existsBySiglaIgnoreCase("ACME")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(req("ACME", "acme", AmbientePadrao.PROD, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("sigla");
  }

  @Test
  void criar_falhaCnpjDuplicado() {
    when(repository.existsBySiglaIgnoreCase(any())).thenReturn(false);
    when(repository.existsByCnpj("12345678000100")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(req("ACME", "ACME", AmbientePadrao.PROD, "12345678000100")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("CNPJ");
  }

  @Test
  void criar_cnpjVazioNaoChamaExistsByCnpj() {
    when(repository.existsBySiglaIgnoreCase(any())).thenReturn(false);
    when(repository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

    Cliente c = service.criar(req("ACME", "ACME", AmbientePadrao.PROD, ""));
    assertThat(c).isNotNull();
    verify(repository, org.mockito.Mockito.never()).existsByCnpj(any());
  }

  @Test
  void atualizar_persisteCamposEditaveis() {
    UUID id = UUID.randomUUID();
    Cliente cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    when(repository.findById(id)).thenReturn(Optional.of(cliente));
    when(repository.existsBySiglaIgnoreCaseAndIdNot(any(), any())).thenReturn(false);

    Cliente atualizado = service.atualizar(id, new ClienteRequest(
        "ACME LTDA", "ACME Comércio LTDA", "12345678000100", "acme",
        null, AmbientePadrao.HOM, TipoBanco.ORACLE,
        "UTF-8", "America/Sao_Paulo", "Cliente piloto", true));

    assertThat(atualizado.getNome()).isEqualTo("ACME LTDA");
    assertThat(atualizado.getRazaoSocial()).isEqualTo("ACME Comércio LTDA");
    assertThat(atualizado.getCnpj()).isEqualTo("12345678000100");
    assertThat(atualizado.getSigla()).isEqualTo("ACME");
    assertThat(atualizado.getAmbientePadrao()).isEqualTo(AmbientePadrao.HOM);
    assertThat(atualizado.getTipoBanco()).isEqualTo(TipoBanco.ORACLE);
  }

  @Test
  void alterarStatus_atualiza() {
    UUID id = UUID.randomUUID();
    Cliente cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    when(repository.findById(id)).thenReturn(Optional.of(cliente));

    Cliente inativo = service.alterarStatus(id, false);
    assertThat(inativo.isAtivo()).isFalse();
  }

  @Test
  void buscar_lancaNotFoundQuandoAusente() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void excluir_chamaDelete() {
    UUID id = UUID.randomUUID();
    Cliente cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    when(repository.findById(id)).thenReturn(Optional.of(cliente));

    service.excluir(id);
    verify(repository).delete(cliente);
  }

  private ClienteRequest req(String nome, String sigla, AmbientePadrao amb, String cnpj) {
    return new ClienteRequest(nome, null, cnpj, sigla, null, amb, null, null, null, null, null);
  }
}
