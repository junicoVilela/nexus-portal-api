package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.FuncionalidadeProdutoRequest;
import com.nexus.portal.releaseorchestrator.entity.DominioProduto;
import com.nexus.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorFuncionalidadeProdutoRepository;
import com.nexus.portal.shared.exception.BusinessException;
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
class FuncionalidadeProdutoServiceTest {

  @Mock OrchestratorFuncionalidadeProdutoRepository repository;
  @Mock DominioProdutoService dominioService;
  @InjectMocks FuncionalidadeProdutoService service;

  ProdutoRh produto;
  DominioProduto dominio;
  final UUID produtoId = UUID.randomUUID();
  final UUID dominioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    dominio = new DominioProduto(produto, "Usuários", "usuarios", null, null, 0);
    when(dominioService.buscar(produtoId, dominioId)).thenReturn(dominio);
  }

  @Test
  void criar_aplicaCriticaPadraoFalse() {
    when(repository.existsByDominio_IdAndCodigoIgnoreCase(dominioId, "inserir"))
        .thenReturn(false);
    when(repository.save(any(FuncionalidadeProduto.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    FuncionalidadeProduto f = service.criar(produtoId, dominioId,
        new FuncionalidadeProdutoRequest("Inserir", "inserir", "CD1", "OP1",
            "Cria usuário", null, null));

    assertThat(f.getCodigo()).isEqualTo("inserir");
    assertThat(f.isCritica()).isFalse();
    assertThat(f.getOrdem()).isZero();
  }

  @Test
  void criar_falhaCodigoDuplicadoNoMesmoDominio() {
    when(repository.existsByDominio_IdAndCodigoIgnoreCase(dominioId, "inserir"))
        .thenReturn(true);

    assertThatThrownBy(() -> service.criar(produtoId, dominioId,
        new FuncionalidadeProdutoRequest("Inserir", "inserir", null, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("código");
  }

  @Test
  void atualizar_naoMudaCodigoMasMudaCriticaEDescricao() {
    UUID id = UUID.randomUUID();
    FuncionalidadeProduto existente = new FuncionalidadeProduto(
        dominio, "Original", "orig", null, null, "antiga", false, 1);
    when(repository.findByDominio_IdAndId(dominioId, id)).thenReturn(Optional.of(existente));

    FuncionalidadeProduto upd = service.atualizar(produtoId, dominioId, id,
        new FuncionalidadeProdutoRequest("Novo", "novo-ignorado", "L2", "OP2",
            "nova", true, 3));

    assertThat(upd.getCodigo()).isEqualTo("orig");  // imutável
    assertThat(upd.isCritica()).isTrue();
    assertThat(upd.getOrdem()).isEqualTo(3);
  }

  @Test
  void alterarStatus_atualiza() {
    UUID id = UUID.randomUUID();
    FuncionalidadeProduto f = new FuncionalidadeProduto(
        dominio, "X", "x", null, null, null, false, 0);
    when(repository.findByDominio_IdAndId(dominioId, id)).thenReturn(Optional.of(f));

    FuncionalidadeProduto inativa = service.alterarStatus(produtoId, dominioId, id, false);
    assertThat(inativa.isAtivo()).isFalse();
  }
}
