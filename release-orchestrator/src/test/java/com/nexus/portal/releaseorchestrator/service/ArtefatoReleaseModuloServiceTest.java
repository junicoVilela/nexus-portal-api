package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.repository.ArtefatoReleaseModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.releaseorchestrator.service.ArtefatoStorageService.ArtefatoStored;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.io.ByteArrayInputStream;
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
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArtefatoReleaseModuloServiceTest {

  @Mock ArtefatoReleaseModuloRepository repository;
  @Mock ReleaseRepository releaseRepository;
  @Mock ModuloProdutoRepository moduloRepository;
  @Mock ArtefatoStorageService storage;
  @InjectMocks ArtefatoReleaseModuloService service;

  private ProdutoRh produto;
  private Release release;
  private ModuloProduto modulo;
  private final UUID releaseId = UUID.randomUUID();
  private final UUID moduloId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());

    release = new Release(produto, "1.5.0", "Release Junho",
        TipoRelease.MINOR, ReleaseStatus.EM_DESENVOLVIMENTO,
        null, null, null, null);
    setId(release, releaseId);

    modulo = new ModuloProduto(produto, "nexus-portal", "Portal",
        TipoModulo.WEB, false, true, 1, null);
    setId(modulo, moduloId);
  }

  @Test
  void upload_felizPath_persisteComShaETamanho() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));
    when(storage.armazenar(eq(releaseId), eq(moduloId), eq("nexus.war"), any()))
        .thenReturn(new ArtefatoStored("/tmp/abc-nexus.war", "abc123", 42L));
    when(repository.save(any(ArtefatoReleaseModulo.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    MockMultipartFile file = new MockMultipartFile("file", "nexus.war",
        "application/octet-stream", new byte[]{1, 2, 3});

    ArtefatoReleaseModulo a = service.upload(releaseId, moduloId, file, "primeira tentativa");

    assertThat(a.getNomeArquivo()).isEqualTo("nexus.war");
    assertThat(a.getSha256()).isEqualTo("abc123");
    assertThat(a.getTamanhoBytes()).isEqualTo(42);
    assertThat(a.getObservacao()).isEqualTo("primeira tentativa");
    verify(repository).save(any(ArtefatoReleaseModulo.class));
  }

  @Test
  void upload_rejeitaExtensaoNaoAceitaPeloTipo() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));

    MockMultipartFile file = new MockMultipartFile("file", "nexus.exe",
        "application/octet-stream", new byte[]{1});

    assertThatThrownBy(() -> service.upload(releaseId, moduloId, file, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Extensão");
    verify(storage, never()).armazenar(any(), any(), anyString(), any());
  }

  @Test
  void upload_aceitaExtensaoEmCaseInsensitivePeloTipo() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));
    when(storage.armazenar(any(), any(), anyString(), any()))
        .thenReturn(new ArtefatoStored("/tmp/x", "h", 1));
    when(repository.save(any(ArtefatoReleaseModulo.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    MockMultipartFile file = new MockMultipartFile("file", "NEXUS.WAR",
        "application/octet-stream", new byte[]{1});

    ArtefatoReleaseModulo a = service.upload(releaseId, moduloId, file, null);
    assertThat(a.getNomeArquivo()).isEqualTo("NEXUS.WAR");
  }

  @Test
  void upload_rejeitaTipoQueNaoAceitaUpload() throws Exception {
    ModuloProduto funcs = new ModuloProduto(produto, "nexus-funcs", "Funcs",
        TipoModulo.FUNCIONALIDADES, false, true, 1, null);
    setId(funcs, moduloId);
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(funcs));

    MockMultipartFile file = new MockMultipartFile("file", "x.sql",
        "application/sql", new byte[]{1});

    assertThatThrownBy(() -> service.upload(releaseId, moduloId, file, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("FUNCIONALIDADES");
  }

  @Test
  void upload_rejeitaQuandoReleasePublicada() {
    release.setStatus(ReleaseStatus.PUBLICADA);
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));

    MockMultipartFile file = new MockMultipartFile("file", "nexus.war",
        "application/octet-stream", new byte[]{1});

    assertThatThrownBy(() -> service.upload(releaseId, moduloId, file, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PUBLICADA");
  }

  @Test
  void upload_rejeitaQuandoReleaseCancelada() {
    release.setStatus(ReleaseStatus.CANCELADA);
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));

    MockMultipartFile file = new MockMultipartFile("file", "nexus.war",
        "application/octet-stream", new byte[]{1});

    assertThatThrownBy(() -> service.upload(releaseId, moduloId, file, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("CANCELADA");
  }

  @Test
  void upload_rejeitaQuandoModuloNaoPertenceAoProdutoDaRelease() throws Exception {
    ProdutoRh outroProduto = new ProdutoRh("OUTRO", "OUTRO", null, "#fff", null, true);
    setId(outroProduto, UUID.randomUUID());
    ModuloProduto moduloDeOutro = new ModuloProduto(outroProduto, "x", "X",
        TipoModulo.WEB, false, true, 1, null);
    setId(moduloDeOutro, moduloId);

    when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
    when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(moduloDeOutro));

    MockMultipartFile file = new MockMultipartFile("file", "x.war",
        "application/octet-stream", new byte[]{1});

    assertThatThrownBy(() -> service.upload(releaseId, moduloId, file, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não pertence");
  }

  @Test
  void upload_falhaQuandoReleaseInexistente() {
    when(releaseRepository.findById(releaseId)).thenReturn(Optional.empty());

    MockMultipartFile file = new MockMultipartFile("file", "x.war",
        "application/octet-stream", new byte[]{1});

    assertThatThrownBy(() -> service.upload(releaseId, moduloId, file, null))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Release");
  }

  @Test
  void excluir_rejeitaQuandoReleasePublicada() {
    UUID artefatoId = UUID.randomUUID();
    ArtefatoReleaseModulo artefato = new ArtefatoReleaseModulo(
        release, modulo, "nexus.war", "/tmp/x", "abc", 10L, null);
    release.setStatus(ReleaseStatus.PUBLICADA);
    when(repository.findByRelease_IdAndModuloProduto_IdAndId(releaseId, moduloId, artefatoId))
        .thenReturn(Optional.of(artefato));

    assertThatThrownBy(() -> service.excluir(releaseId, moduloId, artefatoId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PUBLICADA");
    verify(repository, never()).delete(any(ArtefatoReleaseModulo.class));
    verify(storage, never()).remover(anyString());
  }

  @Test
  void excluir_felizPath_removeArquivoEEntidade() {
    UUID artefatoId = UUID.randomUUID();
    ArtefatoReleaseModulo artefato = new ArtefatoReleaseModulo(
        release, modulo, "nexus.war", "/tmp/abc-nexus.war", "abc", 10L, null);
    when(repository.findByRelease_IdAndModuloProduto_IdAndId(releaseId, moduloId, artefatoId))
        .thenReturn(Optional.of(artefato));

    service.excluir(releaseId, moduloId, artefatoId);

    verify(storage).remover("/tmp/abc-nexus.war");
    verify(repository).delete(artefato);
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
