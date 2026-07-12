package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.repository.ClienteRepository;
import br.com.softon.portal.shared.config.StorageProperties;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import br.com.softon.rbac.service.EscopoResolver;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ClienteLogoService {

  private final ClienteRepository clienteRepository;
  private final StorageProperties storageProperties;
  private final EscopoResolver escopoResolver;

  @Transactional
  public void salvarLogo(UUID clienteId, MultipartFile file) {
    Cliente cliente = buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    validar(file);

    String ext = extensao(file.getOriginalFilename(), file.getContentType());
    Path dir = logosDir(clienteId);
    Path destino = dir.resolve("logo" + ext);

    try {
      Files.createDirectories(dir);
      // remove logo anterior se existir
      if (cliente.getLogoPath() != null) {
        Files.deleteIfExists(Path.of(cliente.getLogoPath()));
      }
      file.transferTo(destino);
    } catch (IOException ex) {
      throw new BusinessException("Falha ao salvar logo: " + ex.getMessage());
    }

    cliente.definirLogo(destino.toString(), file.getContentType());
  }

  public ResponseEntity<Resource> servir(UUID clienteId) {
    Cliente cliente = buscar(clienteId);
    if (!escopoResolver.podeAcessarCliente(clienteId) || cliente.getLogoPath() == null) {
      return ResponseEntity.notFound().build();
    }
    Path path = Path.of(cliente.getLogoPath());
    if (!Files.exists(path)) {
      return ResponseEntity.notFound().build();
    }
    String contentType = cliente.getLogoContentType() != null
        ? cliente.getLogoContentType()
        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .body(new PathResource(path));
  }

  @Transactional
  public void removerLogo(UUID clienteId) {
    Cliente cliente = buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    if (cliente.getLogoPath() != null) {
      try {
        Files.deleteIfExists(Path.of(cliente.getLogoPath()));
      } catch (IOException ignored) {
        // Arquivo não encontrado; apenas limpar referência no banco
      }
      cliente.removerLogo();
    }
  }

  public Path logosDir(UUID clienteId) {
    Path base = Path.of(storageProperties.publicacoesDir()).toAbsolutePath().normalize();
    Path parent = base.getParent() != null ? base.getParent() : base;
    return parent.resolve("clientes").resolve(clienteId.toString());
  }

  private Cliente buscar(UUID id) {
    return clienteRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
  }

  private void validar(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("Selecione uma imagem para o logo.");
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!contentType.startsWith("image/")) {
      throw new BusinessException("Apenas imagens são aceitas como logo.");
    }
    if (file.getSize() > 4 * 1024 * 1024) {
      throw new BusinessException("Logo maior que 4 MB.");
    }
  }

  private String extensao(String nomeOriginal, String contentType) {
    if (nomeOriginal != null) {
      int dot = nomeOriginal.lastIndexOf('.');
      if (dot >= 0 && dot < nomeOriginal.length() - 1) {
        return nomeOriginal.substring(dot).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase(Locale.ROOT);
      }
    }
    return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      default -> ".jpg";
    };
  }
}
