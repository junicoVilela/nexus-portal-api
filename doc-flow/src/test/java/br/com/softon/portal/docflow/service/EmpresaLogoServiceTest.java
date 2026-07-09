package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.softon.portal.shared.config.StorageProperties;
import br.com.softon.portal.shared.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class EmpresaLogoServiceTest {

  EmpresaLogoService service;

  @TempDir Path storage;

  @BeforeEach
  void setUp() {
    service = new EmpresaLogoService(new StorageProperties(storage.resolve("publicacoes").toString()));
  }

  @Test
  void salvarLogo_persisteNaPastaDaEmpresaEEncontraDepois() {
    MultipartFile file = new MockMultipartFile("file", "brand.png", "image/png", new byte[]{9, 9});

    service.salvarLogo(file);

    Optional<Path> logo = service.encontrar();
    assertThat(logo).isPresent();
    assertThat(logo.get().getFileName().toString()).isEqualTo("logo.png");
  }

  @Test
  void salvarLogo_substituiLogoAnteriorDeExtensaoDiferente() {
    service.salvarLogo(new MockMultipartFile("f", "old.jpg", "image/jpeg", new byte[]{1}));
    Path dir = service.empresaDir();
    Path velhoJpg = dir.resolve("logo.jpg");
    assertThat(Files.exists(velhoJpg)).isTrue();

    service.salvarLogo(new MockMultipartFile("f", "new.png", "image/png", new byte[]{2}));

    assertThat(Files.exists(velhoJpg)).isFalse();
    assertThat(Files.exists(dir.resolve("logo.png"))).isTrue();
  }

  @Test
  void salvarLogo_falhaSeArquivoVazio() {
    MultipartFile empty = new MockMultipartFile("f", "logo.png", "image/png", new byte[0]);

    assertThatThrownBy(() -> service.salvarLogo(empty))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void salvarLogo_falhaSeNaoEhImagem() {
    MultipartFile pdf = new MockMultipartFile("f", "doc.pdf", "application/pdf", new byte[]{1});

    assertThatThrownBy(() -> service.salvarLogo(pdf))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void servir_404SeNaoExiste() {
    ResponseEntity<Resource> resp = service.servir();

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void servir_200QuandoLogoPresente() {
    service.salvarLogo(new MockMultipartFile("f", "l.png", "image/png", new byte[]{1, 2}));

    ResponseEntity<Resource> resp = service.servir();

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getHeaders().getContentType()).hasToString("image/png");
  }

  @Test
  void removerLogo_apagaTodasAsExtensoesConhecidas() {
    service.salvarLogo(new MockMultipartFile("f", "l.png", "image/png", new byte[]{1}));

    service.removerLogo();

    assertThat(service.encontrar()).isEmpty();
  }
}
