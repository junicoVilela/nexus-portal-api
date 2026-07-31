package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.service.ModuloService;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.dto.request.PaginaRequest;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.PaginaRevisao;
import br.com.softon.portal.docflow.entity.StatusPagina;
import br.com.softon.portal.docflow.entity.TipoRevisaoPagina;
import br.com.softon.portal.docflow.repository.PaginaRepository;
import br.com.softon.portal.docflow.repository.PaginaAnexoRepository;
import br.com.softon.portal.docflow.repository.PaginaRevisaoRepository;
import br.com.softon.rbac.service.AuditoriaService;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.ConflictException;
import br.com.softon.portal.shared.exception.NotFoundException;
import br.com.softon.portal.shared.util.SlugUtils;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PaginaService {

  private final PaginaRepository paginaRepository;
  private final PaginaRevisaoRepository paginaRevisaoRepository;
  private final ModuloService moduloService;
  private final AuditoriaService auditoriaService;
  private final PaginaQualidadeService paginaQualidadeService;
  private final PaginaAnexoRepository paginaAnexoRepository;
  private final ArquivoRemocaoService arquivoRemocaoService;

  @Transactional
  public Pagina criar(PaginaRequest request, Principal principal) {
    String slug = slugFrom(request.slug(), request.titulo());
    validarUnicos(null, slug, request.codigoTela());
    Modulo modulo = modulo(request.moduloId());
    Pagina parent = parent(request.parentId(), null, modulo);
    String usuario = username(principal);
    Pagina pagina = new Pagina(request.titulo().trim(), slug, request.codigoTela().trim(),
        request.resumo(), sanitizar(request.conteudoHtml()), request.ordem() == null ? 0 : request.ordem(),
        request.ativo() == null || request.ativo(), modulo, parent);
    pagina.definirOrigemTemplate(request.templateOrigemId(), request.templateOrigemVersao());
    pagina = paginaRepository.save(pagina);
    registrarRevisao(pagina, usuario, TipoRevisaoPagina.CRIACAO, "Página criada como rascunho.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "CRIAR", pagina.getTitulo(), principal);
    return pagina;
  }

  @Transactional
  public Pagina atualizar(UUID id, PaginaRequest request, Principal principal) {
    Pagina pagina = buscar(id);
    validarVersao(pagina, request.version());
    String slug = slugFrom(request.slug(), request.titulo());
    validarUnicos(id, slug, request.codigoTela());
    Modulo modulo = modulo(request.moduloId());
    Pagina parent = parent(request.parentId(), id, modulo);
    pagina.atualizar(request.titulo().trim(), slug, request.codigoTela().trim(), request.resumo(),
        sanitizar(request.conteudoHtml()), request.ordem() == null ? 0 : request.ordem(),
        request.ativo() == null || request.ativo(), modulo, parent);
    pagina.definirOrigemTemplate(request.templateOrigemId(), request.templateOrigemVersao());
    paginaRepository.flush();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.SALVAMENTO_MANUAL,
        "Conteúdo salvo manualmente.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "ATUALIZAR", pagina.getTitulo(), principal);
    return pagina;
  }

  @Transactional
  public Pagina autosave(UUID id, PaginaRequest request) {
    Pagina pagina = buscar(id);
    validarVersao(pagina, request.version());
    if (pagina.getStatus() != StatusPagina.RASCUNHO) {
      throw new BusinessException("O salvamento automático está disponível somente para páginas em rascunho.");
    }
    String slug = slugFrom(request.slug(), request.titulo());
    validarUnicos(id, slug, request.codigoTela());
    Modulo modulo = modulo(request.moduloId());
    Pagina parent = parent(request.parentId(), id, modulo);
    pagina.atualizar(request.titulo().trim(), slug, request.codigoTela().trim(), request.resumo(),
        sanitizar(request.conteudoHtml()), request.ordem() == null ? 0 : request.ordem(),
        request.ativo() == null || request.ativo(), modulo, parent);
    pagina.definirOrigemTemplate(request.templateOrigemId(), request.templateOrigemVersao());
    paginaRepository.flush();
    return pagina;
  }

  public Page<Pagina> listar(String titulo, UUID moduloId, UUID projetoId, StatusPagina status,
      String codigoTela, String busca, Pageable pageable) {
    return paginaRepository.findAll(specification(titulo, moduloId, projetoId, status, codigoTela, busca), pageable);
  }

  public List<Pagina> listar(String titulo, UUID moduloId, UUID projetoId, StatusPagina status, String codigoTela) {
    return paginaRepository.findAll(specification(titulo, moduloId, projetoId, status, codigoTela, null), Sort.by(
        Sort.Order.asc("modulo.projeto.nome"),
        Sort.Order.asc("modulo.ordem"),
        Sort.Order.asc("parent.ordem").nullsFirst(),
        Sort.Order.asc("ordem"),
        Sort.Order.asc("titulo")));
  }

  public Pagina buscar(UUID id) {
    return paginaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Página não encontrada."));
  }

  @Transactional
  public void excluir(UUID id, Principal principal) {
    Pagina pagina = buscar(id);
    if (paginaRepository.existsByParent_Id(id)) {
      throw new BusinessException("Exclua primeiro as subpáginas desta página.");
    }
    List<Path> anexos = paginaAnexoRepository.findByPagina_Id(id).stream()
        .map(anexo -> Path.of(anexo.getCaminho()))
        .toList();
    paginaRepository.delete(pagina);
    auditoriaService.registrar("PAGINA", id, "EXCLUIR", "Página excluída: " + pagina.getTitulo(), principal);
    arquivoRemocaoService.removerAposCommit(anexos);
  }

  public List<Pagina> buscarTodos(List<UUID> ids) {
    List<Pagina> paginas = paginaRepository.findAllById(ids);
    if (paginas.size() != ids.stream().distinct().count()) {
      throw new BusinessException("Uma ou mais páginas informadas não existem.");
    }
    return paginas;
  }

  @Transactional
  public Pagina salvarRascunho(UUID id, Principal principal) {
    Pagina pagina = buscar(id);
    pagina.salvarRascunho();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.RETORNO_RASCUNHO,
        "Página retornada ao estado de rascunho.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "SALVAR_RASCUNHO", pagina.getTitulo(), principal);
    return pagina;
  }

  @Transactional
  public Pagina enviarRevisao(UUID id, Principal principal) {
    Pagina pagina = buscar(id);
    validarPublicacao(pagina);
    var qualidade = paginaQualidadeService.avaliar(pagina);
    if (!qualidade.aptoParaRevisao()) {
      String pendencias = qualidade.itens().stream()
          .filter(item -> item.severidade() == PaginaQualidadeService.Severidade.ERRO && !item.ok())
          .map(PaginaQualidadeService.ItemQualidade::titulo)
          .collect(java.util.stream.Collectors.joining(", "));
      throw new BusinessException("A página ainda não está pronta para revisão: " + pendencias + ".");
    }
    pagina.enviarRevisao();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.ENVIO_REVISAO,
        "Página enviada para revisão editorial.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "ENVIAR_REVISAO", pagina.getTitulo(), principal);
    return pagina;
  }

  @Transactional
  public Pagina aprovar(UUID id, Principal principal) {
    Pagina pagina = buscar(id);
    if (pagina.getStatus() != StatusPagina.EM_REVISAO) {
      throw new BusinessException("Somente páginas em revisão podem ser aprovadas.");
    }
    pagina.aprovar();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.APROVACAO,
        "Página aprovada para publicação.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "APROVAR", pagina.getTitulo(), principal);
    return pagina;
  }

  @Transactional
  public Pagina publicar(UUID id, Principal principal) {
    Pagina pagina = buscar(id);
    validarPublicacao(pagina);
    if (pagina.getStatus() != StatusPagina.APROVADO && pagina.getStatus() != StatusPagina.PUBLICADO) {
      throw new BusinessException("A página precisa estar aprovada antes de publicar.");
    }
    pagina.publicar();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.PUBLICACAO,
        "Página publicada no manual.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "PUBLICAR", pagina.getTitulo(), principal);
    return pagina;
  }

  @Transactional
  public Pagina arquivar(UUID id, Principal principal) {
    Pagina pagina = buscar(id);
    arquivarRecursivo(pagina, principal);
    return pagina;
  }

  private void arquivarRecursivo(Pagina pagina, Principal principal) {
    for (Pagina filho : paginaRepository.findByParent_Id(pagina.getId())) {
      arquivarRecursivo(filho, principal);
    }
    if (pagina.getStatus() == StatusPagina.ARQUIVADO) {
      return;
    }
    pagina.arquivar();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.ARQUIVAMENTO,
        "Página arquivada.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "ARQUIVAR", pagina.getTitulo(), principal);
  }

  public Page<PaginaRevisao> revisoes(UUID id, Pageable pageable) {
    buscar(id);
    return paginaRevisaoRepository.findByPagina_Id(id, pageable);
  }

  @Transactional
  public PaginaRevisao comentarRevisao(UUID id, String comentario, Principal principal) {
    Pagina pagina = buscar(id);
    if (pagina.getStatus() != StatusPagina.EM_REVISAO) {
      throw new BusinessException("Comentários editoriais estão disponíveis somente durante a revisão.");
    }
    PaginaRevisao revisao = registrarRevisao(
        pagina,
        username(principal),
        TipoRevisaoPagina.COMENTARIO,
        comentario.trim());
    auditoriaService.registrar("PAGINA", pagina.getId(), "COMENTAR_REVISAO", pagina.getTitulo(), principal);
    return revisao;
  }

  public PaginaQualidadeService.ResultadoQualidade qualidade(UUID id) {
    return paginaQualidadeService.avaliar(buscar(id));
  }

  @Transactional
  public Pagina duplicar(UUID id, Principal principal) {
    Pagina origem = buscar(id);
    String usuario = username(principal);
    String titulo = origem.getTitulo() + " (cópia)";
    String slug = slugUnico(origem.getSlug() + "-copia");
    String codigoTela = codigoTelaUnico(origem.getCodigoTela() + "-COPIA");
    Pagina copia = paginaRepository.save(new Pagina(titulo, slug, codigoTela, origem.getResumo(),
        origem.getConteudoHtml(), origem.getOrdem() + 1, origem.isAtivo(), origem.getModulo(), origem.getParent()));
    registrarRevisao(copia, usuario, TipoRevisaoPagina.DUPLICACAO,
        "Página criada a partir de uma duplicação.");
    auditoriaService.registrar("PAGINA", copia.getId(), "DUPLICAR", origem.getTitulo() + " -> " + copia.getTitulo(),
        principal);
    return copia;
  }

  @Transactional
  public void reordenar(List<UUID> paginaIds, Principal principal) {
    if (paginaIds.isEmpty() || paginaIds.size() != paginaIds.stream().distinct().count()) {
      throw new BusinessException("A ordenação deve conter páginas distintas.");
    }
    List<Pagina> paginas = buscarTodos(paginaIds);
    UUID moduloId = paginas.getFirst().getModulo().getId();
    UUID parentId = paginas.getFirst().getParent() == null ? null : paginas.getFirst().getParent().getId();
    if (paginas.stream().anyMatch(p -> !p.getModulo().getId().equals(moduloId)
        || !java.util.Objects.equals(parentId, p.getParent() == null ? null : p.getParent().getId()))) {
      throw new BusinessException("Só é possível reordenar páginas do mesmo módulo e nível.");
    }
    for (int i = 0; i < paginas.size(); i++) {
      Pagina pagina = paginas.get(i);
      int novaOrdem = i;
        pagina.atualizar(pagina.getTitulo(), pagina.getSlug(), pagina.getCodigoTela(),
            pagina.getResumo(), pagina.getConteudoHtml(), novaOrdem, pagina.isAtivo(),
            pagina.getModulo(), pagina.getParent());
    }
    auditoriaService.registrar("PAGINA", null, "REORDENAR", "Páginas reordenadas.", principal);
  }

  public String preview(UUID id) {
    Pagina pagina = buscar(id);
    return """
        <!doctype html>
        <html lang="pt-BR">
        <head><meta charset="utf-8"><title>%s</title></head>
        <body><main>%s</main></body>
        </html>
        """.formatted(pagina.getTitulo(), pagina.getConteudoHtml() == null ? "" : pagina.getConteudoHtml());
  }

  private void validarPublicacao(Pagina pagina) {
    if (pagina.getTitulo() == null || pagina.getTitulo().isBlank()
        || pagina.getSlug() == null || pagina.getSlug().isBlank()
        || pagina.getCodigoTela() == null || pagina.getCodigoTela().isBlank()
        || pagina.getConteudoHtml() == null || Jsoup.parse(pagina.getConteudoHtml()).text().isBlank()) {
      throw new BusinessException("Título, slug, código da tela e conteúdo HTML são obrigatórios para publicar.");
    }
  }

  private void validarUnicos(UUID id, String slug, String codigoTela) {
    if (id == null && paginaRepository.existsBySlug(slug)
        || id != null && paginaRepository.existsBySlugAndIdNot(slug, id)) {
      throw new BusinessException("Já existe página com o slug informado.");
    }
    String codigo = codigoTela == null ? null : codigoTela.trim();
    if (codigo == null || codigo.isBlank()) {
      throw new BusinessException("Código da tela é obrigatório.");
    }
    if (id == null && paginaRepository.existsByCodigoTela(codigo)
        || id != null && paginaRepository.existsByCodigoTelaAndIdNot(codigo, id)) {
      throw new BusinessException("Já existe página com o código da tela informado.");
    }
  }

  private Modulo modulo(UUID id) {
    return moduloService.buscar(id);
  }

  private Pagina parent(UUID parentId, UUID paginaId, Modulo modulo) {
    if (parentId == null) {
      return null;
    }
    if (parentId.equals(paginaId)) {
      throw new BusinessException("A página não pode ser subpágina dela mesma.");
    }

    Pagina parent = buscar(parentId);
    if (!parent.getModulo().getId().equals(modulo.getId())) {
      throw new BusinessException("A página pai deve pertencer ao mesmo módulo.");
    }
    for (Pagina atual = parent; atual != null; atual = atual.getParent()) {
      if (atual.getId().equals(paginaId)) {
        throw new BusinessException("A hierarquia de páginas não pode formar ciclo.");
      }
    }
    return parent;
  }

  private String slugFrom(String slug, String titulo) {
    String normalized = SlugUtils.normalize(slug == null || slug.isBlank() ? titulo : slug);
    if (normalized == null) {
      throw new BusinessException("Slug inválido.");
    }
    return normalized;
  }

  private String slugUnico(String base) {
    String normalized = SlugUtils.normalize(base);
    if (normalized == null) {
      throw new BusinessException("Slug inválido.");
    }
    String candidato = normalized;
    int suffix = 2;
    while (paginaRepository.existsBySlug(candidato)) {
      candidato = normalized + "-" + suffix++;
    }
    return candidato;
  }

  private String codigoTelaUnico(String base) {
    String normalized = base.replaceAll("[^a-zA-Z0-9._-]", "-");
    String candidato = normalized;
    int suffix = 2;
    while (paginaRepository.existsByCodigoTela(candidato)) {
      candidato = normalized + "-" + suffix++;
    }
    return candidato;
  }

  private String sanitizar(String html) {
    if (html == null || html.isBlank()) {
      return html;
    }
    Safelist safelist = Safelist.relaxed()
        .addTags("section", "article", "aside", "figure", "figcaption")
        .addAttributes(":all", "class")
        .addAttributes("img", "src", "alt", "title")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https", "data");
    return Jsoup.clean(html, safelist);
  }

  private String username(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }

  private PaginaRevisao registrarRevisao(Pagina pagina, String username, TipoRevisaoPagina tipo,
      String descricao) {
    int numero = paginaRevisaoRepository.countByPagina_Id(pagina.getId()) + 1;
    return paginaRevisaoRepository.save(new PaginaRevisao(pagina, numero, username, tipo, descricao));
  }

  private void validarVersao(Pagina pagina, Long versaoEsperada) {
    if (versaoEsperada == null) {
      throw new ConflictException("A versão da página é obrigatória. Recarregue o editor e tente novamente.");
    }
    if (pagina.getVersion() != versaoEsperada) {
      throw new ConflictException(
          "Esta página foi alterada por outro usuário. Compare as versões antes de continuar.");
    }
  }

  private String lowerBlankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  private Specification<Pagina> specification(String titulo, UUID moduloId, UUID projetoId, StatusPagina status,
      String codigoTela, String busca) {
    String tituloFiltro = lowerBlankToNull(titulo);
    String codigoTelaFiltro = lowerBlankToNull(codigoTela);
    String buscaFiltro = lowerBlankToNull(busca);

    return (root, query, criteriaBuilder) -> {
      if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
        root.fetch("modulo", JoinType.INNER).fetch("projeto", JoinType.INNER);
        root.fetch("parent", JoinType.LEFT);
        query.distinct(true);
      }

      List<Predicate> predicates = new ArrayList<>();
      if (tituloFiltro != null) {
        predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), "%" + tituloFiltro + "%"));
      }
      if (moduloId != null) {
        predicates.add(criteriaBuilder.equal(root.get("modulo").get("id"), moduloId));
      }
      if (projetoId != null) {
        predicates.add(criteriaBuilder.equal(root.get("modulo").get("projeto").get("id"), projetoId));
      }
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }
      if (codigoTelaFiltro != null) {
        predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("codigoTela")),
            "%" + codigoTelaFiltro + "%"));
      }
      if (buscaFiltro != null) {
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), "%" + buscaFiltro + "%"),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), "%" + buscaFiltro + "%"),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("codigoTela")), "%" + buscaFiltro + "%"),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("resumo")), "%" + buscaFiltro + "%")));
      }
      return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  public Map<StatusPagina, Long> resumoContagemPorStatusGlobal() {
    EnumMap<StatusPagina, Long> map = new EnumMap<>(StatusPagina.class);
    for (StatusPagina s : StatusPagina.values()) {
      map.put(s, 0L);
    }
    for (Object[] row : paginaRepository.contarPorStatusAgrupado()) {
      map.put((StatusPagina) row[0], ((Number) row[1]).longValue());
    }
    return map;
  }
}
