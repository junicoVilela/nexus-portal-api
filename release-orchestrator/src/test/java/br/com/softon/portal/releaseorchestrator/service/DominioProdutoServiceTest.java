package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.releaseorchestrator.dto.request.DominioProdutoRequest;
import br.com.softon.portal.releaseorchestrator.entity.DominioProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorDominioProdutoRepository;
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
class DominioProdutoServiceTest {

  @Mock OrchestratorDominioProdutoRepository repository;
  @Mock ProdutoRhRepository produtoRepository;
  @InjectMocks DominioProdutoService service;

  ProdutoRh produto;
  final UUID produtoId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    produto = new ProdutoRh("DTEC-LD", "DTECLD", null, "#fff", null, true);
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
  }

  @Test
  void criar_persisteComOrdemPadraoZero() {
    when(repository.existsByProduto_IdAndCodigoIgnoreCase(produtoId, "usuarios"))
        .thenReturn(false);
    when(repository.save(any(DominioProduto.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DominioProduto d = service.criar(produtoId, new DominioProdutoRequest(
        "Usuários", "usuarios", "USR", "Tudo de usuários", null));

    assertThat(d.getNome()).isEqualTo("Usuários");
    assertThat(d.getCodigo()).isEqualTo("usuarios");
    assertThat(d.getCodigoLegado()).isEqualTo("USR");
    assertThat(d.getOrdem()).isZero();
  }

  @Test
  void criar_falhaCodigoDuplicadoNoMesmoProduto() {
    when(repository.existsByProduto_IdAndCodigoIgnoreCase(produtoId, "usuarios"))
        .thenReturn(true);

    assertThatThrownBy(() -> service.criar(produtoId, new DominioProdutoRequest(
        "Usuários", "usuarios", null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("código");
  }

  @Test
  void atualizar_naoMudaCodigo() {
    UUID id = UUID.randomUUID();
    DominioProduto existente = new DominioProduto(produto, "Original", "orig",
        null, null, 1);
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.of(existente));

    DominioProduto upd = service.atualizar(produtoId, id, new DominioProdutoRequest(
        "Novo Nome", "novo-codigo-ignorado", "LEG", "nova desc", 5));

    assertThat(upd.getNome()).isEqualTo("Novo Nome");
    assertThat(upd.getCodigo()).isEqualTo("orig");  // imutável
    assertThat(upd.getCodigoLegado()).isEqualTo("LEG");
    assertThat(upd.getOrdem()).isEqualTo(5);
  }

  @Test
  void buscar_lancaNotFoundQuandoOutroProduto() {
    UUID id = UUID.randomUUID();
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(produtoId, id))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void alterarStatus_atualiza() {
    UUID id = UUID.randomUUID();
    DominioProduto d = new DominioProduto(produto, "X", "x", null, null, 0);
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.of(d));

    DominioProduto inativo = service.alterarStatus(produtoId, id, false);
    assertThat(inativo.isAtivo()).isFalse();
  }

  @Test
  void excluir_chamaDelete() {
    UUID id = UUID.randomUUID();
    DominioProduto d = new DominioProduto(produto, "X", "x", null, null, 0);
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.of(d));

    service.excluir(produtoId, id);
    verify(repository).delete(d);
  }
}
