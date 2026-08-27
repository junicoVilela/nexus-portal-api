package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaAnexo;
import com.nexus.portal.docflow.repository.PaginaAnexoRepository;
import com.nexus.portal.shared.config.StorageProperties;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.identityaccess.service.AuditoriaService;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
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

  /** SVG decodifica como XML e pode carregar script; fica de fora do que é servido publicamente. */
  private static final List<String> SVG_CONTENT_TYPES =
      List.of("image/svg+xml", "image/svg");

  private final PaginaService paginaService;
  private final PaginaAnexoRepository paginaAnexoRepository;
  private final AnexoStorage anexoStorage;
  private final ArquivoRemocaoService arquivoRemocaoService;
  private final AuditoriaService auditoriaService;

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
    Path destino = anexoStorage.novoArquivo(paginaId, nomeOriginal, file.getContentType());
    try {
      Files.createDirectories(destino.getParent());
      file.transferTo(destino);
    } catch (IOException ex) {
      throw new BusinessException("Falha ao salvar anexo: " + ex.getMessage());
    }
    return paginaAnexoRepository.save(new PaginaAnexo(pagina, nomeOriginal, file.getContentType(), file.getSize(),
        destino.toString()));
  }

  /**
   * O download é público ({@code permitAll}) porque o HTML do manual aponta para
   * cá. Registra o acesso na auditoria: sem isso não há como saber que um anexo
   * foi baixado, nem por quem quando há sessão.
   */
  public Resource arquivo(UUID paginaId, UUID anexoId, Principal principal) {
    PaginaAnexo anexo = buscar(anexoId);
    if (!anexo.getPagina().getId().equals(paginaId)) {
      throw new NotFoundException("Anexo não encontrado para a página.");
    }
    Path path = Path.of(anexo.getCaminho());
    if (!Files.exists(path)) {
      throw new NotFoundException("Arquivo do anexo não encontrado em disco.");
    }
    auditoriaService.registrar("PAGINA_ANEXO", anexoId, "BAIXAR", anexo.getNomeOriginal(), principal);
    return new PathResource(path);
  }

  public PaginaAnexo buscar(UUID id) {
    return paginaAnexoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Anexo não encontrado."));
  }

  @Transactional
  public void excluir(UUID paginaId, UUID anexoId, Principal principal) {
    PaginaAnexo anexo = buscar(anexoId);
    if (!anexo.getPagina().getId().equals(paginaId)) {
      throw new NotFoundException("Anexo não encontrado para a página.");
    }
    Path caminho = Path.of(anexo.getCaminho());
    paginaAnexoRepository.delete(anexo);
    auditoriaService.registrar("PAGINA_ANEXO", anexoId, "EXCLUIR",
        "Anexo removido: " + anexo.getNomeOriginal(), principal);
    // Só apaga o arquivo depois do commit — um rollback deixaria a linha no
    // banco apontando para um caminho inexistente.
    arquivoRemocaoService.removerAposCommit(caminho);
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
    validarConteudoDeImagem(file);
  }

  /**
   * O {@code Content-Type} vem do cliente e não prova nada. O anexo é servido
   * publicamente ({@code /anexos/{id}/download}), então o conteúdo precisa ser
   * mesmo uma imagem decodificável.
   */
  private void validarConteudoDeImagem(MultipartFile file) {
    if (SVG_CONTENT_TYPES.contains(file.getContentType().toLowerCase(Locale.ROOT))) {
      throw new BusinessException("SVG não é aceito como anexo: use PNG, JPG, WEBP ou GIF.");
    }
    try (InputStream entrada = file.getInputStream()) {
      if (ImageIO.read(entrada) == null) {
        throw new BusinessException("O arquivo enviado não é uma imagem válida.");
      }
    } catch (IOException ex) {
      throw new BusinessException("Não foi possível ler a imagem enviada: " + ex.getMessage());
    }
  }

}
