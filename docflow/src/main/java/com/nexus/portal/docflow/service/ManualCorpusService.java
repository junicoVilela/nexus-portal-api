package com.nexus.portal.docflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corpus do answer engine e do MCP (Onda E): o <b>snapshot publicado</b> — o mesmo ZIP que o
 * cliente recebe. Rascunhos e páginas editadas depois da publicação nunca entram (INT-505).
 *
 * <p>Lê {@code rag/} (um Markdown por tela, Onda A) e corta cada tela nas seções {@code ##}.
 * Publicações anteriores à Onda A caem no {@code search-index.json} (uma seção por tela).
 */
@Service
public class ManualCorpusService {

  private static final int MAX_EM_CACHE = 32;

  private final PublicacaoRepository publicacaoRepository;
  private final ObjectMapper objectMapper;
  /** O ZIP de uma publicação não muda: cache por id até o limite. */
  private final Map<UUID, Corpus> cache = new ConcurrentHashMap<>();

  public ManualCorpusService(PublicacaoRepository publicacaoRepository, ObjectMapper objectMapper) {
    this.publicacaoRepository = publicacaoRepository;
    this.objectMapper = objectMapper;
  }

  /** Uma tela inteira do manual. */
  public record Documento(String codigoTela, String titulo, String caminho, String url, String markdown) {}

  /** Trecho recuperável: uma seção {@code ##} de uma tela (a introdução é a seção sem título). */
  public record Secao(Documento documento, String titulo, String texto) {}

  public record Corpus(
      UUID publicacaoId,
      String cliente,
      String versao,
      Map<String, Documento> documentos,
      List<Secao> secoes) {

    public String rotulo() {
      return "Manual " + cliente + " v" + versao;
    }
  }

  @Transactional(readOnly = true)
  public Corpus daPublicacao(UUID publicacaoId) {
    Corpus emCache = cache.get(publicacaoId);
    if (emCache != null) {
      return emCache;
    }
    Publicacao publicacao = publicacaoRepository.findById(publicacaoId)
        .orElseThrow(() -> new NotFoundException("Publicação não encontrada."));
    return carregar(publicacao);
  }

  /** Última publicação concluída do cliente (o manual "vigente"). */
  @Transactional(readOnly = true)
  public Corpus vigenteDoCliente(UUID clienteId) {
    Publicacao publicacao = publicacaoVigente(clienteId);
    Corpus emCache = cache.get(publicacao.getId());
    return emCache != null ? emCache : carregar(publicacao);
  }

  /** Manual vigente já resolvido (sem associações lazy para fora da transação). */
  public record Vigente(UUID publicacaoId, String cliente, String versao, OffsetDateTime publicadaEm,
      int quantidadePaginas, Path zip) {}

  @Transactional(readOnly = true)
  public Vigente vigente(UUID clienteId) {
    Publicacao p = publicacaoVigente(clienteId);
    return new Vigente(p.getId(), p.getCliente().getNome(), p.getVersao(), p.getUpdatedAt(),
        p.getQuantidadePaginas(), Path.of(p.getArquivoZipCaminho()));
  }

  /** Publicação vigente: a última concluída do cliente (INT-401). */
  @Transactional(readOnly = true)
  public Publicacao publicacaoVigente(UUID clienteId) {
    return publicacaoRepository.findByCliente_IdOrderByCreatedAtDesc(clienteId).stream()
        .filter(p -> p.getStatus() == StatusPublicacao.SUCESSO && p.getArquivoZipCaminho() != null)
        .findFirst()
        .orElseThrow(() -> new NotFoundException("O cliente ainda não tem manual publicado."));
  }

  private Corpus carregar(Publicacao publicacao) {
    if (publicacao.getStatus() != StatusPublicacao.SUCESSO || publicacao.getArquivoZipCaminho() == null) {
      throw new BusinessException("A publicação " + publicacao.getVersao() + " não tem pacote concluído.");
    }
    Path zip = Path.of(publicacao.getArquivoZipCaminho());
    if (!Files.exists(zip)) {
      throw new NotFoundException("O pacote da publicação " + publicacao.getVersao() + " não está mais disponível.");
    }
    try (ZipFile arquivo = new ZipFile(zip.toFile())) {
      Map<String, Documento> documentos = arquivo.getEntry(ManualRagService.PASTA + "/index.json") != null
          ? lerRag(arquivo)
          : lerIndiceDeBusca(arquivo);
      List<Secao> secoes = documentos.values().stream().flatMap(d -> secoes(d).stream()).toList();
      Corpus corpus = new Corpus(publicacao.getId(), publicacao.getCliente().getNome(), publicacao.getVersao(),
          documentos, secoes);
      if (cache.size() >= MAX_EM_CACHE) {
        cache.clear();
      }
      cache.put(publicacao.getId(), corpus);
      return corpus;
    } catch (IOException ex) {
      throw new UncheckedIOException("Falha ao ler o pacote da publicação.", ex);
    }
  }

  private Map<String, Documento> lerRag(ZipFile arquivo) throws IOException {
    Map<String, Documento> documentos = new LinkedHashMap<>();
    JsonNode index = objectMapper.readTree(ler(arquivo, ManualRagService.PASTA + "/index.json"));
    for (JsonNode item : index.path("documentos")) {
      String markdown = ler(arquivo, item.path("arquivo").asText());
      documentos.put(item.path("codigoTela").asText(), new Documento(
          item.path("codigoTela").asText(),
          item.path("titulo").asText(),
          item.path("caminho").asText(),
          item.path("url").isNull() ? null : item.path("url").asText(null),
          semFrontmatter(markdown)));
    }
    return documentos;
  }

  /** Publicações anteriores à Onda A: texto plano do {@code search-index.json}. */
  private Map<String, Documento> lerIndiceDeBusca(ZipFile arquivo) throws IOException {
    Map<String, Documento> documentos = new LinkedHashMap<>();
    for (JsonNode item : objectMapper.readTree(ler(arquivo, "search-index.json"))) {
      String titulo = item.path("titulo").asText();
      documentos.put(item.path("codigoTela").asText(), new Documento(
          item.path("codigoTela").asText(), titulo, titulo, item.path("url").asText(null),
          "# " + titulo + "\n\n" + item.path("texto").asText()));
    }
    return documentos;
  }

  /** Corta nas linhas {@code ## }; o que vem antes (título, resumo) é a introdução. */
  static List<Secao> secoes(Documento documento) {
    List<Secao> secoes = new ArrayList<>();
    String titulo = null;
    StringBuilder texto = new StringBuilder();
    for (String linha : documento.markdown().split("\n")) {
      if (linha.startsWith("## ")) {
        adicionar(secoes, documento, titulo, texto);
        titulo = linha.substring(3).strip();
        texto.setLength(0);
      } else if (!linha.startsWith("# ")) {
        texto.append(linha).append('\n');
      }
    }
    adicionar(secoes, documento, titulo, texto);
    return secoes;
  }

  private static void adicionar(List<Secao> secoes, Documento documento, String titulo, StringBuilder texto) {
    String conteudo = texto.toString().strip();
    if (!conteudo.isEmpty() || titulo != null) {
      secoes.add(new Secao(documento, titulo, conteudo));
    }
  }

  static String semFrontmatter(String markdown) {
    if (!markdown.startsWith("---\n")) {
      return markdown.strip();
    }
    int fim = markdown.indexOf("\n---\n", 4);
    return fim < 0 ? markdown.strip() : markdown.substring(fim + 5).strip();
  }

  private static String ler(ZipFile arquivo, String nome) throws IOException {
    ZipEntry entrada = arquivo.getEntry(nome);
    if (entrada == null) {
      throw new BusinessException("Pacote sem " + nome + ".");
    }
    return new String(arquivo.getInputStream(entrada).readAllBytes(), StandardCharsets.UTF_8);
  }
}
