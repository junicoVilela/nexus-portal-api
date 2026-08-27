package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

/**
 * Lê o HTML de uma página de dentro do pacote já gerado.
 *
 * <p>O snapshot da publicação guarda só o hash do conteúdo — suficiente para
 * dizer que mudou, não para mostrar o quê. O ZIP tem o HTML renderizado de cada
 * versão, então serve como o arquivo histórico sem duplicar nada no banco.
 */
@Service
@RequiredArgsConstructor
public class PublicacaoConteudoService {

  private final PublicacaoService publicacaoService;

  public String htmlDaPagina(UUID publicacaoId, UUID paginaId) {
    Publicacao publicacao = publicacaoService.buscar(publicacaoId);
    if (publicacao.getStatus() != StatusPublicacao.SUCESSO) {
      throw new BusinessException("Só publicações concluídas têm conteúdo arquivado.");
    }
    String slug = slugNoSnapshot(publicacaoId, paginaId);
    Path zip = caminhoDoPacote(publicacao);

    try (ZipFile arquivo = new ZipFile(zip.toFile())) {
      ZipEntry entrada = arquivo.getEntry("paginas/" + slug + ".html");
      if (entrada == null) {
        throw new NotFoundException("Página não encontrada no pacote desta publicação.");
      }
      try (InputStream conteudo = arquivo.getInputStream(entrada)) {
        return extrairArtigo(new String(conteudo.readAllBytes(), StandardCharsets.UTF_8));
      }
    } catch (IOException ex) {
      throw new BusinessException("Não foi possível ler o pacote da publicação: " + ex.getMessage());
    }
  }

  /**
   * O arquivo do pacote é a página inteira (menu, cabeçalho, rodapé). Para
   * comparar duas versões só interessa o corpo do artigo.
   */
  private String extrairArtigo(String htmlCompleto) {
    var documento = Jsoup.parse(htmlCompleto);
    var artigo = documento.selectFirst(".article-content");
    return artigo == null ? documento.body().html() : artigo.html();
  }

  private String slugNoSnapshot(UUID publicacaoId, UUID paginaId) {
    return publicacaoService.arvorePaginas(publicacaoId).stream()
        .filter(item -> paginaId.equals(item.id()))
        .map(PublicacaoPaginaSnapshotItem::slug)
        .filter(slug -> slug != null && !slug.isBlank())
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Página não faz parte desta publicação."));
  }

  private Path caminhoDoPacote(Publicacao publicacao) {
    String caminho = publicacao.getArquivoZipCaminho();
    if (caminho == null || caminho.isBlank()) {
      throw new BusinessException("Publicação sem pacote disponível.");
    }
    Path zip = Path.of(caminho);
    if (!Files.exists(zip)) {
      throw new NotFoundException("Pacote da publicação não encontrado em disco.");
    }
    return zip;
  }
}
