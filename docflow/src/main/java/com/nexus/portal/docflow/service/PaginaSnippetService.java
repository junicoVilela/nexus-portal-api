package com.nexus.portal.docflow.service;

import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.docflow.dto.request.PaginaSnippetRequest;
import com.nexus.portal.docflow.entity.PaginaSnippet;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.docflow.repository.PaginaSnippetRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * CRUD dos trechos reutilizáveis e resolução das referências
 * {@code {{snippet:CODIGO}}} no conteúdo publicado.
 */
@Service
@RequiredArgsConstructor
public class PaginaSnippetService {

  private static final Pattern REFERENCIA =
      Pattern.compile("\\{\\{\\s*snippet:\\s*([A-Za-z0-9_-]+)\\s*}}");

  private final PaginaSnippetRepository repository;
  private final PaginaRepository paginaRepository;
  private final AuditoriaService auditoriaService;

  public List<PaginaSnippet> listar(boolean incluirInativos) {
    return incluirInativos
        ? repository.findAllByOrderByCodigoAsc()
        : repository.findByAtivoTrueOrderByCodigoAsc();
  }

  public PaginaSnippet buscar(java.util.UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Trecho reutilizável não encontrado."));
  }

  @Transactional
  public PaginaSnippet criar(PaginaSnippetRequest request, Principal principal) {
    String codigo = normalizarCodigo(request.codigo());
    if (repository.existsByCodigo(codigo)) {
      throw new BusinessException("Já existe um trecho com este código.");
    }
    PaginaSnippet snippet = repository.save(new PaginaSnippet(codigo, request.titulo().trim(),
        textoOpcional(request.descricao()), sanitizar(request.conteudoHtml()),
        request.ativo() == null || request.ativo()));
    auditoriaService.registrar("PAGINA_SNIPPET", snippet.getId(), "CRIAR", codigo, principal);
    return snippet;
  }

  @Transactional
  public PaginaSnippet atualizar(java.util.UUID id, PaginaSnippetRequest request, Principal principal) {
    PaginaSnippet snippet = buscar(id);
    if (!snippet.getCodigo().equals(normalizarCodigo(request.codigo()))) {
      // O código é a chave usada dentro do HTML das páginas; trocar quebraria as referências.
      throw new BusinessException("O código do trecho não pode ser alterado.");
    }
    snippet.atualizar(request.titulo().trim(), textoOpcional(request.descricao()),
        sanitizar(request.conteudoHtml()), request.ativo() == null || request.ativo());
    auditoriaService.registrar("PAGINA_SNIPPET", id, "ATUALIZAR", snippet.getCodigo(), principal);
    return snippet;
  }

  @Transactional
  public void excluir(java.util.UUID id, Principal principal) {
    PaginaSnippet snippet = buscar(id);
    repository.delete(snippet);
    auditoriaService.registrar("PAGINA_SNIPPET", id, "EXCLUIR", snippet.getCodigo(), principal);
  }

  /**
   * Substitui as referências pelo HTML do trecho ativo. Código inexistente ou
   * inativo vira um aviso visível no lugar — silenciar deixaria o manual sair
   * com um buraco que ninguém percebe.
   */
  public String resolver(String html) {
    if (html == null || html.isBlank() || !html.contains("{{")) {
      return html;
    }
    Matcher matcher = REFERENCIA.matcher(html);
    if (!matcher.find()) {
      return html;
    }
    Map<String, PaginaSnippet> ativos = repository.findByAtivoTrueOrderByCodigoAsc().stream()
        .collect(Collectors.toMap(PaginaSnippet::getCodigo, Function.identity()));
    matcher.reset();
    StringBuilder resultado = new StringBuilder();
    while (matcher.find()) {
      PaginaSnippet snippet = ativos.get(normalizarCodigo(matcher.group(1)));
      String substituicao = snippet == null
          ? "<p class=\"snippet-ausente\">Trecho \"" + matcher.group(1) + "\" indisponível.</p>"
          : snippet.getConteudoHtml();
      matcher.appendReplacement(resultado, Matcher.quoteReplacement(substituicao));
    }
    matcher.appendTail(resultado);
    return resultado.toString();
  }

  /** Códigos ativos, para o checklist validar as referências sem carregar o conteúdo. */
  public Set<String> codigosAtivos() {
    return repository.findByAtivoTrueOrderByCodigoAsc().stream()
        .map(PaginaSnippet::getCodigo)
        .collect(Collectors.toSet());
  }

  /**
   * Quantas páginas citam cada trecho — para medir o impacto antes de desativar
   * ou excluir. A referência mora dentro do HTML, então a contagem é uma busca
   * por texto; o catálogo de trechos é pequeno o bastante para uma consulta por
   * código.
   */
  public Map<String, Long> usoPorCodigo(Collection<String> codigos) {
    Map<String, Long> uso = new LinkedHashMap<>();
    for (String codigo : codigos) {
      uso.put(codigo, paginaRepository.contarPaginasQueCitam("{{snippet:" + codigo + "}}"));
    }
    return uso;
  }

  /** Códigos referenciados em um HTML — usado pelo checklist de qualidade. */
  public List<String> referencias(String html) {
    if (html == null || html.isBlank()) {
      return List.of();
    }
    return REFERENCIA.matcher(html).results()
        .map(resultado -> normalizarCodigo(resultado.group(1)))
        .distinct()
        .toList();
  }

  private String normalizarCodigo(String codigo) {
    if (codigo == null || codigo.isBlank()) {
      throw new BusinessException("Informe o código do trecho.");
    }
    return codigo.trim().toUpperCase(Locale.ROOT);
  }

  private String sanitizar(String html) {
    if (html == null || html.isBlank()) {
      throw new BusinessException("O trecho precisa ter conteúdo.");
    }
    Safelist safelist = Safelist.relaxed()
        .addTags("section", "article", "aside", "figure", "figcaption")
        .addAttributes(":all", "class")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https", "data")
        .preserveRelativeLinks(true);
    return Jsoup.clean(html, "https://localhost/", safelist);
  }

  private String textoOpcional(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }
}
