package br.com.softon.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import br.com.softon.portal.releaseorchestrator.service.ArtefatoStorageService.ArtefatoStored;
import br.com.softon.portal.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArtefatoStorageServiceTest {

  @TempDir Path tempDir;

  @Test
  void armazenar_gravaArquivoComSha256Correto() {
    ArtefatoStorageService service = service(tempDir.toString());
    UUID releaseId = UUID.randomUUID();
    UUID moduloId = UUID.randomUUID();

    ArtefatoStored stored = service.armazenar(releaseId, moduloId, "dtec.war",
        new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)));

    assertThat(stored.tamanhoBytes()).isEqualTo(11);
    // SHA-256("hello world") = b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
    assertThat(stored.sha256())
        .isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");

    Path destino = Path.of(stored.caminho());
    assertThat(destino).exists();
    assertThat(destino.startsWith(tempDir.resolve(releaseId.toString()).resolve(moduloId.toString())))
        .isTrue();
    assertThat(destino.getFileName().toString()).endsWith("-dtec.war");
  }

  @Test
  void armazenar_sanitizaNomeDoArquivo() {
    ArtefatoStorageService service = service(tempDir.toString());

    ArtefatoStored stored = service.armazenar(UUID.randomUUID(), UUID.randomUUID(),
        "../../etc/passwd", new ByteArrayInputStream(new byte[]{0x01}));

    assertThat(Path.of(stored.caminho()).getFileName().toString()).endsWith("-passwd");
  }

  @Test
  void armazenar_falhaSeArtefatosDirAusente() {
    ArtefatoStorageService service = service(null);

    assertThatThrownBy(() -> service.armazenar(UUID.randomUUID(), UUID.randomUUID(),
        "x.war", new ByteArrayInputStream(new byte[0])))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("artefatos-dir");
  }

  @Test
  void remover_deletaArquivoEIgnoraInexistente() throws Exception {
    ArtefatoStorageService service = service(tempDir.toString());
    ArtefatoStored stored = service.armazenar(UUID.randomUUID(), UUID.randomUUID(),
        "x.war", new ByteArrayInputStream(new byte[]{0x01}));
    Path destino = Path.of(stored.caminho());
    assertThat(destino).exists();

    service.remover(stored.caminho());
    assertThat(destino).doesNotExist();

    // chamada subsequente não lança
    service.remover(stored.caminho());
  }

  @Test
  void arquivo_lancaQuandoNaoExiste() {
    ArtefatoStorageService service = service(tempDir.toString());
    assertThatThrownBy(() -> service.arquivo(tempDir.resolve("inexistente.war").toString()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não encontrado");
  }

  private ArtefatoStorageService service(String dir) {
    return new ArtefatoStorageService(new ReleaseOrchestratorStorageProperties(dir));
  }
}
