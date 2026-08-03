package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.dto.request.AtualizarModuloProdutoRequest;
import com.nexus.portal.releaseorchestrator.dto.request.CriarModuloProdutoRequest;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.List;
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
class ModuloProdutoServiceTest {

  @Mock ModuloProdutoRepository repository;
  @Mock ProdutoRhRepository produtoRepository;
  @InjectMocks ModuloProdutoService service;

  private final UUID produtoId = UUID.randomUUID();
  private final ProdutoRh produto = new ProdutoRh(
      "NEXUS-LD", "NEXUSLD", null, "#fff", null, true);

  @Test
  void criar_aplicaDefaultsDoTipoQuandoCamposVazios() {
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    when(repository.existsByProduto_IdAndCodigoIgnoreCase(produtoId, "nexus-portal")).thenReturn(false);
    when(repository.save(any(ModuloProduto.class))).thenAnswer(inv -> inv.getArgument(0));

    ModuloProduto m = service.criar(produtoId, new CriarModuloProdutoRequest(
        "Portal", "nexus-portal", TipoModulo.WEB, null, null, null, null));

    assertThat(m.getCodigo()).isEqualTo("nexus-portal");
    assertThat(m.getTipo()).isEqualTo(TipoModulo.WEB);
    assertThat(m.isGeraDelta()).isFalse();   // WEB default
    assertThat(m.isObrigatorio()).isTrue();  // WEB default
    assertThat(m.getOrdem()).isZero();
  }

  @Test
  void criar_respeitaCamposExplicitos() {
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    when(repository.existsByProduto_IdAndCodigoIgnoreCase(any(), any())).thenReturn(false);
    when(repository.save(any(ModuloProduto.class))).thenAnswer(inv -> inv.getArgument(0));

    ModuloProduto m = service.criar(produtoId, new CriarModuloProdutoRequest(
        "Banco", "nexus-db", TipoModulo.BANCO, false, false, 5, "{\"dialeto\":\"oracle\"}"));

    assertThat(m.isGeraDelta()).isFalse();
    assertThat(m.isObrigatorio()).isFalse();
    assertThat(m.getOrdem()).isEqualTo(5);
    assertThat(m.getConfigEspecifica()).contains("oracle");
  }

  @Test
  void criar_falhaQuandoCodigoDuplicadoNoProduto() {
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    when(repository.existsByProduto_IdAndCodigoIgnoreCase(produtoId, "nexus-portal")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(produtoId, new CriarModuloProdutoRequest(
        "Portal", "nexus-portal", TipoModulo.WEB, null, null, null, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("código");
  }

  @Test
  void criar_falhaQuandoProdutoNaoExiste() {
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.criar(produtoId, new CriarModuloProdutoRequest(
        "Portal", "nexus-portal", TipoModulo.WEB, null, null, null, null)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void atualizar_naoMudaCodigoNemTipo() {
    UUID id = UUID.randomUUID();
    ModuloProduto modulo = new ModuloProduto(
        produto, "nexus-portal", "Portal", TipoModulo.WEB, false, true, 1, null);
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.of(modulo));

    ModuloProduto atualizado = service.atualizar(produtoId, id, new AtualizarModuloProdutoRequest(
        "Portal Web", true, false, "{\"destinoPacote\":\"web/portal/\"}"));

    assertThat(atualizado.getCodigo()).isEqualTo("nexus-portal");   // imutável
    assertThat(atualizado.getTipo()).isEqualTo(TipoModulo.WEB);    // imutável
    assertThat(atualizado.getNome()).isEqualTo("Portal Web");
    assertThat(atualizado.isGeraDelta()).isTrue();
    assertThat(atualizado.isObrigatorio()).isFalse();
    assertThat(atualizado.getConfigEspecifica()).contains("destinoPacote");
  }

  @Test
  void alterarStatus_atualizaFlag() {
    UUID id = UUID.randomUUID();
    ModuloProduto modulo = new ModuloProduto(
        produto, "nexus-portal", "Portal", TipoModulo.WEB, false, true, 1, null);
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.of(modulo));

    ModuloProduto inativo = service.alterarStatus(produtoId, id, false);
    assertThat(inativo.isAtivo()).isFalse();
  }

  @Test
  void buscar_lancaQuandoModuloNaoExisteNoProduto() {
    UUID id = UUID.randomUUID();
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(produtoId, id))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void listar_filtraPorProdutoEMantemOrdem() {
    when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produto));
    ModuloProduto a = new ModuloProduto(produto, "nexus-a", "A", TipoModulo.WEB, false, true, 1, null);
    ModuloProduto b = new ModuloProduto(produto, "nexus-b", "B", TipoModulo.WEB, false, true, 2, null);
    when(repository.findByProduto_IdOrderByOrdemAscNomeAsc(produtoId)).thenReturn(List.of(a, b));

    List<ModuloProduto> resultado = service.listar(produtoId);
    assertThat(resultado).extracting(ModuloProduto::getCodigo).containsExactly("nexus-a", "nexus-b");
  }

  @Test
  void excluir_chamaDelete() {
    UUID id = UUID.randomUUID();
    ModuloProduto modulo = new ModuloProduto(
        produto, "nexus-portal", "Portal", TipoModulo.WEB, false, true, 1, null);
    when(repository.findByProduto_IdAndId(produtoId, id)).thenReturn(Optional.of(modulo));

    service.excluir(produtoId, id);
    verify(repository).delete(modulo);
  }
}
