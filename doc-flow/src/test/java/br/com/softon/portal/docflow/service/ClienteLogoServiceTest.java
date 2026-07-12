package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.repository.ClienteRepository;
import br.com.softon.portal.shared.config.StorageProperties;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import br.com.softon.rbac.service.EscopoResolver;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteLogoServiceTest {

  @Mock ClienteRepository clienteRepository;
  @Mock EscopoResolver escopoResolver;

  ClienteLogoService service;

  UUID clienteId;
  Cliente cliente;

  @TempDir Path storage;

  @BeforeEach
  void setUp() throws Exception {
    service = new ClienteLogoService(clienteRepository,
        new StorageProperties(storage.resolve("publicacoes").toString()), escopoResolver);
    clienteId = UUID.randomUUID();
    cliente = new Cliente("ACME", "acme", true);
    setId(cliente, clienteId);
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
    when(escopoResolver.podeAcessarCliente(clienteId)).thenReturn(true);
  }

  @Test
  void salvarLogo_persisteArquivoEAtualizaMetadados() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

    service.salvarLogo(clienteId, file);

    assertThat(cliente.getLogoPath()).isNotNull();
    assertThat(cliente.getLogoContentType()).isEqualTo("image/png");
    assertThat(Files.exists(Path.of(cliente.getLogoPath()))).isTrue();
    assertThat(Path.of(cliente.getLogoPath()).getFileName().toString()).isEqualTo("logo.png");
  }

  @Test
  void salvarLogo_falhaSeClienteInexistente() {
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());
    MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

    assertThatThrownBy(() -> service.salvarLogo(clienteId, file))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void salvarLogo_falhaSeArquivoVazio() {
    MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);

    assertThatThrownBy(() -> service.salvarLogo(clienteId, file))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Selecione");
  }

  @Test
  void salvarLogo_falhaSeNaoEhImagem() {
    MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

    assertThatThrownBy(() -> service.salvarLogo(clienteId, file))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("imagens");
  }

  @Test
  void salvarLogo_falhaSeMaiorQue4MB() {
    byte[] big = new byte[5 * 1024 * 1024];
    MultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", big);

    assertThatThrownBy(() -> service.salvarLogo(clienteId, file))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("4 MB");
  }

  @Test
  void servir_retorna404SeSemLogo() {
    ResponseEntity<Resource> resp = service.servir(clienteId);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void servir_retorna404SeArquivoRemovidoDoDisco() throws Exception {
    setField(cliente, "logoPath", storage.resolve("nao-existe.png").toString());
    setField(cliente, "logoContentType", "image/png");

    ResponseEntity<Resource> resp = service.servir(clienteId);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void removerLogo_apagaArquivoESujaMetadadosDoCliente() throws Exception {
    Path logo = storage.resolve("logo-a.png");
    Files.writeString(logo, "png");
    setField(cliente, "logoPath", logo.toString());
    setField(cliente, "logoContentType", "image/png");

    service.removerLogo(clienteId);

    assertThat(Files.exists(logo)).isFalse();
    assertThat(cliente.getLogoPath()).isNull();
  }

  private static void setId(Object entity, UUID id) throws Exception {
    setField(entity, "id", id);
  }

  private static void setField(Object entity, String name, Object value) throws Exception {
    Field f = entity.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(entity, value);
  }
}
