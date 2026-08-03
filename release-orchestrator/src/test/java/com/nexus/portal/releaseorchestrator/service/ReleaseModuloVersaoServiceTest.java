package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseModuloVersao;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseModuloVersaoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
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
class ReleaseModuloVersaoServiceTest {

  @Mock ReleaseModuloVersaoRepository repository;
  @Mock ReleaseRepository releaseRepository;
  @Mock ModuloProdutoRepository moduloRepository;
  @InjectMocks ReleaseModuloVersaoService service;

  ProdutoRh produto;
  Release release;
  ModuloProduto modulo;
  final UUID releaseId = UUID.randomUUID();
  final UUID moduloId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());

    release = new Release(produto, "1.5.0", "Release", TipoRelease.MINOR,
        ReleaseStatus.EM_DESENVOLVIMENTO, null, null, null, null);
    setId(release, releaseId);

    modulo = new ModuloProduto(produto, "nexus-portal", "Portal", TipoModulo.WEB,
        false, true, 1, null);
    setId(modulo, moduloId);
  }

  @Test
  void salvar_criaVinculoNovoQuandoNaoExiste() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));
    when(repository.findByRelease_IdAndModuloProduto_Id(releaseId, moduloId))
        .thenReturn(Optional.empty());
    when(repository.save(any(ReleaseModuloVersao.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ReleaseModuloVersao v = service.salvar(releaseId, moduloId, "1.5.0");

    assertThat(v.getVersao()).isEqualTo("1.5.0");
    assertThat(v.getModuloProduto().getCodigo()).isEqualTo("nexus-portal");
    verify(repository).save(any(ReleaseModuloVersao.class));
  }

  @Test
  void salvar_atualizaVersaoQuandoVinculoJaExiste() {
    ReleaseModuloVersao existente = new ReleaseModuloVersao(release, modulo, "1.4.0");
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));
    when(repository.findByRelease_IdAndModuloProduto_Id(releaseId, moduloId))
        .thenReturn(Optional.of(existente));

    ReleaseModuloVersao v = service.salvar(releaseId, moduloId, "1.5.0");

    assertThat(v.getVersao()).isEqualTo("1.5.0");
    verify(repository, never()).save(any(ReleaseModuloVersao.class));
  }

  @Test
  void salvar_rejeitaQuandoReleasePublicada() {
    release.setStatus(ReleaseStatus.PUBLICADA);
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));

    assertThatThrownBy(() -> service.salvar(releaseId, moduloId, "1.5.0"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PUBLICADA");
  }

  @Test
  void salvar_rejeitaQuandoModuloDeOutroProduto() throws Exception {
    ProdutoRh outro = new ProdutoRh("OUTRO", "OUTRO", null, "#fff", null, true);
    setId(outro, UUID.randomUUID());
    ModuloProduto moduloDeOutro = new ModuloProduto(outro, "x", "X", TipoModulo.WEB,
        false, true, 1, null);
    setId(moduloDeOutro, moduloId);
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(moduloDeOutro));

    assertThatThrownBy(() -> service.salvar(releaseId, moduloId, "1.5.0"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não pertence");
  }

  @Test
  void remover_excluiVinculoExistente() {
    ReleaseModuloVersao vinculo = new ReleaseModuloVersao(release, modulo, "1.5.0");
    when(repository.findByRelease_IdAndModuloProduto_Id(releaseId, moduloId))
        .thenReturn(Optional.of(vinculo));

    service.remover(releaseId, moduloId);
    verify(repository).delete(vinculo);
  }

  @Test
  void remover_rejeitaQuandoReleasePublicada() {
    release.setStatus(ReleaseStatus.PUBLICADA);
    ReleaseModuloVersao vinculo = new ReleaseModuloVersao(release, modulo, "1.5.0");
    when(repository.findByRelease_IdAndModuloProduto_Id(releaseId, moduloId))
        .thenReturn(Optional.of(vinculo));

    assertThatThrownBy(() -> service.remover(releaseId, moduloId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PUBLICADA");
    verify(repository, never()).delete(any(ReleaseModuloVersao.class));
  }

  @Test
  void listar_lancaNotFoundQuandoReleaseInexistente() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.listar(releaseId))
        .isInstanceOf(NotFoundException.class);
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
