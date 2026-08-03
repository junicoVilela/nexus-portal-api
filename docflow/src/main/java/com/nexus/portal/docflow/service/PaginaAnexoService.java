package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaAnexo;
import com.nexus.portal.docflow.repository.PaginaAnexoRepository;
import com.nexus.portal.shared.config.StorageProperties;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PaginaAnexoService {
  private final PaginaService paginaService;
  private final PaginaAnexoRepository paginaAnexoRepository;
  private final StorageProperties storageProperties;

  public List<PaginaAnexo> listar(UUID paginaId) {
    paginaService.buscar(paginaId);
    return paginaAnexoRepository.findByPagina_IdOrderByCreatedAtDesc(paginaId);
  }

  public Page<PaginaAnexo> listarBiblioteca(String busca, Pageable pageable) {
    return busca == null || busca.isBlank()
        ? paginaAnexoRepository.findAll(pageable)
        : paginaAnexoRepository.findByNomeOriginalContainingIgnoreCase(busca.trim(), pageable);
  }

  @Transactional
  public PaginaAnexo anexar(UUID paginaId, MultipartFile file) {
    Pagina pagina = paginaService.buscar(paginaId);
    validar(file);
    String nomeOriginal = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
        ? "imagem"
        : file.getOriginalFilename();
    String filename = UUID.randomUUID() + extensao(nomeOriginal, file.getContentType());
    Path dir = anexosDir().resolve(paginaId.toString());
    Path destino = dir.resolve(filename).normalize();
    try {
      Files.createDirectories(dir);
      file.transferTo(destino);
    } catch (IOException ex) {
      throw new BusinessException("Falha ao salvar anexo: " + ex.getMessage());
    }
    return paginaAnexoRepository.save(new PaginaAnexo(pagina, nomeOriginal, file.getContentType(), file.getSize(),
        destino.toString()));
  }

  public Resource arquivo(UUID paginaId, UUID anexoId) {
    PaginaAnexo anexo = buscar(anexoId);
    if (!anexo.getPagina().getId().equals(paginaId)) {
      throw new NotFoundException("Anexo não encontrado para a página.");
    }
    Path path = Path.of(anexo.getCaminho());
    if (!Files.exists(path)) {
      throw new NotFoundException("Arquivo do anexo não encontrado em disco.");
    }
    return new PathResource(path);
  }

  public PaginaAnexo buscar(UUID id) {
    return paginaAnexoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Anexo não encontrado."));
  }

  @Transactional
  public void excluir(UUID paginaId, UUID anexoId) {
    PaginaAnexo anexo = buscar(anexoId);
    if (!anexo.getPagina().getId().equals(paginaId)) {
      throw new NotFoundException("Anexo não encontrado para a página.");
    }
    paginaAnexoRepository.delete(anexo);
    try {
      Files.deleteIfExists(Path.of(anexo.getCaminho()));
    } catch (IOException ignored) {
      // A referência no banco já foi removida; arquivo órfão não deve bloquear a operação.
    }
  }

  private void validar(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("Selecione um arquivo para anexar.");
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (!contentType.startsWith("image/")) {
      throw new BusinessException("Apenas imagens podem ser anexadas nesta etapa.");
    }
    if (file.getSize() > 8 * 1024 * 1024) {
      throw new BusinessException("Imagem maior que 8 MB.");
    }
  }

  private Path anexosDir() {
    Path publicacoesDir = Path.of(storageProperties.publicacoesDir()).toAbsolutePath().normalize();
    Path base = publicacoesDir.getParent() == null ? publicacoesDir : publicacoesDir.getParent();
    return base.resolve("anexos");
  }

  private String extensao(String nomeOriginal, String contentType) {
    int dot = nomeOriginal.lastIndexOf('.');
    if (dot >= 0 && dot < nomeOriginal.length() - 1) {
      return nomeOriginal.substring(dot).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase(Locale.ROOT);
    }
    return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      default -> ".jpg";
    };
  }

}
