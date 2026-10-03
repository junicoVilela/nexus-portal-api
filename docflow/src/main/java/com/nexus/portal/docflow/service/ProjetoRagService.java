package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

/**
 * Base de RAG de um projeto: as páginas <b>publicadas</b> dele, uma por arquivo Markdown, sem
 * depender de cliente nem de gerar publicação. É o que o pipeline de RAG de cada produto baixa
 * (ex.: num job de CI) para manter a ajuda do sistema em dia.
 */
@Service
public class ProjetoRagService {

  private final ProjetoService projetoService;
  private final PaginaRepository paginaRepository;
  private final PaginaSnippetService paginaSnippetService;
  private final ManualRagService manualRagService;

  public ProjetoRagService(
      ProjetoService projetoService,
      PaginaRepository paginaRepository,
      PaginaSnippetService paginaSnippetService,
      ManualRagService manualRagService) {
    this.projetoService = projetoService;
    this.paginaRepository = paginaRepository;
    this.paginaSnippetService = paginaSnippetService;
    this.manualRagService = manualRagService;
  }

  public record Exportacao(String nomeArquivo, byte[] zip) {}

  @Transactional(readOnly = true)
  public Exportacao exportar(UUID projetoId) {
    Projeto projeto = projetoService.buscar(projetoId);
    List<Pagina> paginas = paginaRepository.findAtivasByStatusWithModulo(StatusPagina.PUBLICADO).stream()
        .filter(p -> p.getModulo().getProjeto().getId().equals(projetoId))
        .sorted(ordemDoManual())
        .toList();
    if (paginas.isEmpty()) {
      throw new BusinessException("O projeto " + projeto.getNome() + " não tem páginas publicadas.");
    }
    Path dir = null;
    try {
      dir = Files.createTempDirectory("rag-" + projeto.getSlug());
      manualRagService.escrever(dir, paginas, new ManualRagService.Escopo(
          "Manual " + projeto.getNome(), null, null, null,
          p -> paginaSnippetService.resolver(p.getConteudoHtml() == null ? "" : p.getConteudoHtml()),
          false));
      return new Exportacao("rag-" + projeto.getSlug() + ".zip", zipar(dir));
    } catch (IOException ex) {
      throw new UncheckedIOException("Falha ao montar a base de RAG do projeto.", ex);
    } finally {
      if (dir != null) {
        FileSystemUtils.deleteRecursively(dir.toFile());
      }
    }
  }

  /** Módulos na ordem do projeto; dentro deles, a árvore de páginas (pai antes dos filhos). */
  static Comparator<Pagina> ordemDoManual() {
    return Comparator
        .comparingInt((Pagina p) -> p.getModulo().getOrdem())
        .thenComparing(p -> p.getModulo().getNome())
        .thenComparing(ProjetoRagService::chave);
  }

  private static String chave(Pagina pagina) {
    List<String> partes = new ArrayList<>();
    for (Pagina atual = pagina; atual != null; atual = atual.getParent()) {
      partes.addFirst("%06d|%s".formatted(atual.getOrdem(), atual.getTitulo()));
    }
    return String.join("/", partes);
  }

  private static byte[] zipar(Path dir) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes); Stream<Path> arquivos = Files.walk(dir)) {
      for (Path arquivo : arquivos.filter(Files::isRegularFile).sorted().toList()) {
        zip.putNextEntry(new ZipEntry(dir.relativize(arquivo).toString().replace('\\', '/')));
        Files.copy(arquivo, zip);
        zip.closeEntry();
      }
    }
    return bytes.toByteArray();
  }
}
