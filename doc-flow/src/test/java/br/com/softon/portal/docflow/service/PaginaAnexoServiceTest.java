package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.PaginaAnexo;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.repository.PaginaAnexoRepository;
import br.com.softon.portal.shared.config.StorageProperties;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaginaAnexoServiceTest {

  @Mock PaginaService paginaService;
  @Mock PaginaAnexoRepository paginaAnexoRepository;

  PaginaAnexoService service;

  UUID paginaId;
  Pagina pagina;

  @TempDir Path storage;

  @BeforeEach
  void setUp() throws Exception {
    service = new PaginaAnexoService(paginaService, paginaAnexoRepository,
        new StorageProperties(storage.resolve("publicacoes").toString()));
    paginaId = UUID.randomUUID();
    Projeto proj = new Projeto("P", "p", null, true);
    Modulo mod = new Modulo("M", "m", null, 1, true, proj);
    pagina = new Pagina("T", "t", "T", null, null, 1, true, mod, null);
    setId(pagina, paginaId);
    when(paginaService.buscar(paginaId)).thenReturn(pagina);
    when(paginaAnexoRepository.save(any(PaginaAnexo.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void anexar_persisteArquivoEMetadata() {
    MultipartFile img = new MockMultipartFile("f", "print.png", "image/png", new byte[]{1, 2, 3});

    PaginaAnexo anexo = service.anexar(paginaId, img);

    assertThat(anexo.getNomeOriginal()).isEqualTo("print.png");
    assertThat(anexo.getContentType()).isEqualTo("image/png");
    assertThat(anexo.getTamanhoBytes()).isEqualTo(3);
    assertThat(Files.exists(Path.of(anexo.getCaminho()))).isTrue();
    assertThat(Path.of(anexo.getCaminho()).getFileName().toString()).endsWith(".png");
  }

  @Test
  void anexar_falhaSeNaoEhImagem() {
    MultipartFile pdf = new MockMultipartFile("f", "doc.pdf", "application/pdf", new byte[]{1});

    assertThatThrownBy(() -> service.anexar(paginaId, pdf))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("imagens");
  }

  @Test
  void anexar_falhaSeMaiorQue8MB() {
    byte[] big = new byte[9 * 1024 * 1024];
    MultipartFile img = new MockMultipartFile("f", "big.png", "image/png", big);

    assertThatThrownBy(() -> service.anexar(paginaId, img))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("8 MB");
  }

  @Test
  void arquivo_falhaSeAnexoNaoEhDaPagina() throws Exception {
    UUID anexoId = UUID.randomUUID();
    Projeto proj = new Projeto("P", "p", null, true);
    Modulo mod = new Modulo("M", "m", null, 1, true, proj);
    Pagina outra = new Pagina("O", "o", "O", null, null, 1, true, mod, null);
    setId(outra, UUID.randomUUID());
    PaginaAnexo a = new PaginaAnexo(outra, "x.png", "image/png", 1, "/tmp/x.png");
    when(paginaAnexoRepository.findById(anexoId)).thenReturn(Optional.of(a));

    assertThatThrownBy(() -> service.arquivo(paginaId, anexoId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void arquivo_falhaSeArquivoAusenteNoDisco() {
    UUID anexoId = UUID.randomUUID();
    PaginaAnexo a = new PaginaAnexo(pagina, "x.png", "image/png", 1,
        storage.resolve("nao-existe.png").toString());
    when(paginaAnexoRepository.findById(anexoId)).thenReturn(Optional.of(a));

    assertThatThrownBy(() -> service.arquivo(paginaId, anexoId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("disco");
  }

  @Test
  void excluir_removeArquivoDoDiscoEDoRepositorio() throws Exception {
    Path file = storage.resolve("a.png");
    Files.writeString(file, "png");
    UUID anexoId = UUID.randomUUID();
    PaginaAnexo a = new PaginaAnexo(pagina, "a.png", "image/png", 3, file.toString());
    when(paginaAnexoRepository.findById(anexoId)).thenReturn(Optional.of(a));

    service.excluir(paginaId, anexoId);

    verify(paginaAnexoRepository).delete(a);
    assertThat(Files.exists(file)).isFalse();
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
