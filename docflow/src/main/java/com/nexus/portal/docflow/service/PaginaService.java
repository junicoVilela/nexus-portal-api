package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.service.ModuloService;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.dto.request.PaginaRequest;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.PaginaAnexo;
import com.nexus.portal.docflow.entity.PaginaRevisao;
import com.nexus.portal.docflow.entity.StatusPagina;
import com.nexus.portal.docflow.entity.TipoRevisaoPagina;
import com.nexus.portal.docflow.repository.PaginaRepository;
import com.nexus.portal.docflow.repository.PaginaAnexoRepository;
import com.nexus.portal.docflow.repository.PaginaRevisaoRepository;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.ConflictException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.portal.shared.util.SlugUtils;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
  private final PaginaEventService paginaEventService;
  private final NotificacaoEmailService notificacaoEmailService;
  private final AnexoStorage anexoStorage;

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
    sincronizarIndicePaiSeAplicavel(pagina, principal);
    return pagina;
  }

  @Transactional
  public Pagina atualizar(UUID id, PaginaRequest request, Principal principal) {
    Pagina pagina = buscar(id);
    validarVersao(pagina, request.version());
    String slug = slugFrom(request.slug(), request.titulo());
    String conteudoHtml = sanitizar(request.conteudoHtml());
    exigirConteudoEditavel(pagina, request, slug, conteudoHtml);
    validarUnicos(id, slug, request.codigoTela());
    Modulo modulo = modulo(request.moduloId());
    Pagina parent = parent(request.parentId(), id, modulo);
    pagina.atualizar(request.titulo().trim(), slug, request.codigoTela().trim(), request.resumo(),
        conteudoHtml, request.ordem() == null ? 0 : request.ordem(),
        request.ativo() == null || request.ativo(), modulo, parent);
    pagina.definirOrigemTemplate(request.templateOrigemId(), request.templateOrigemVersao());
    paginaRepository.flush();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.SALVAMENTO_MANUAL,
        "Conteúdo salvo manualmente.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "ATUALIZAR", pagina.getTitulo(), principal);
    sincronizarIndicePaiSeAplicavel(pagina, principal);
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
    excluirRecursivo(buscar(id), principal);
  }

  private void excluirRecursivo(Pagina pagina, Principal principal) {
    for (Pagina filho : paginaRepository.findByParent_Id(pagina.getId())) {
      excluirRecursivo(filho, principal);
    }
    List<Path> anexos = paginaAnexoRepository.findByPagina_Id(pagina.getId()).stream()
        .map(anexo -> Path.of(anexo.getCaminho()))
        .toList();
    paginaRepository.delete(pagina);
    auditoriaService.registrar("PAGINA", pagina.getId(), "EXCLUIR",
        "Página excluída: " + pagina.getTitulo(), principal);
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
    StatusPagina statusAnterior = pagina.getStatus();
    pagina.salvarRascunho();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.RETORNO_RASCUNHO,
        "Página retornada ao estado de rascunho.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "SALVAR_RASCUNHO", pagina.getTitulo(), principal);
    if (statusAnterior == StatusPagina.EM_REVISAO) {
      paginaEventService.publicar(pagina, "DEVOLVER", username(principal));
    }
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
    paginaEventService.publicar(pagina, "ENVIAR_REVISAO", username(principal));
    notificacaoEmailService.notificarPaginaEmRevisao(pagina);
    return pagina;
  }

  /**
   * Atribui a revisão a alguém, com prazo opcional. Só faz sentido enquanto a
   * página não foi publicada.
   */
  @Transactional
  public Pagina atribuirRevisor(UUID id, String revisorUsername, OffsetDateTime prazo,
      Principal principal) {
    Pagina pagina = buscar(id);
    if (pagina.getStatus() == StatusPagina.PUBLICADO || pagina.getStatus() == StatusPagina.ARQUIVADO) {
      throw new BusinessException("Páginas publicadas ou arquivadas não entram na fila de revisão.");
    }
    if (revisorUsername == null || revisorUsername.isBlank()) {
      throw new BusinessException("Informe o responsável pela revisão.");
    }
    if (prazo != null && prazo.isBefore(OffsetDateTime.now())) {
      throw new BusinessException("O prazo de revisão não pode estar no passado.");
    }
    pagina.atribuirRevisor(revisorUsername.trim(), prazo);
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.ATRIBUICAO_REVISOR,
        "Revisão atribuída a " + revisorUsername.trim() + ".");
    auditoriaService.registrar("PAGINA", pagina.getId(), "ATRIBUIR_REVISOR",
        pagina.getTitulo() + " -> " + revisorUsername.trim(), principal);
    paginaEventService.publicar(pagina, "ATRIBUIR_REVISOR", username(principal));
    return pagina;
  }

  /** Fila do revisor: o que está em revisão sob a responsabilidade dele. */
  public Page<Pagina> filaDoRevisor(String revisorUsername, Pageable pageable) {
    return paginaRepository.findByRevisorUsernameAndStatus(
        revisorUsername, StatusPagina.EM_REVISAO, pageable);
  }

  @Transactional
  public Pagina aprovar(UUID id, Principal principal) {
    Pagina pagina = buscar(id);
    if (pagina.getStatus() != StatusPagina.EM_REVISAO) {
      throw new BusinessException("Somente páginas em revisão podem ser aprovadas.");
    }
    validarDonoDaRevisao(pagina, principal);
    pagina.aprovar();
    pagina.limparRevisor();
    registrarRevisao(pagina, username(principal), TipoRevisaoPagina.APROVACAO,
        "Página aprovada para publicação.");
    auditoriaService.registrar("PAGINA", pagina.getId(), "APROVAR", pagina.getTitulo(), principal);
    paginaEventService.publicar(pagina, "APROVAR", username(principal));
    notificarAutorDoEnvio(pagina);
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
    paginaEventService.publicar(pagina, "PUBLICAR", username(principal));
    sincronizarIndicePaiSeAplicavel(pagina, principal);
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
    paginaEventService.publicar(pagina, "ARQUIVAR", username(principal));
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

  /**
   * Duplica a página inteira: anexos (arquivo e referência no HTML), origem de
   * modelo e subpáginas. Copiar só o HTML deixava a cópia apontando para os
   * anexos do original — apagar a original quebrava as duas.
   */
  @Transactional
  public Pagina duplicar(UUID id, Principal principal) {
    Pagina origem = buscar(id);
    Pagina copia = duplicarRecursivo(origem, origem.getParent(), username(principal));
    auditoriaService.registrar("PAGINA", copia.getId(), "DUPLICAR",
        origem.getTitulo() + " -> " + copia.getTitulo(), principal);
    return copia;
  }

  private Pagina duplicarRecursivo(Pagina origem, Pagina novoParent, String usuario) {
    Pagina copia = paginaRepository.save(new Pagina(
        origem.getTitulo() + " (cópia)",
        slugUnico(origem.getSlug() + "-copia"),
        codigoTelaUnico(origem.getCodigoTela() + "-COPIA"),
        origem.getResumo(),
        origem.getConteudoHtml(),
        origem.getOrdem() + 1,
        origem.isAtivo(),
        origem.getModulo(),
        novoParent));
    copia.definirOrigemTemplate(origem.getTemplateOrigemId(), origem.getTemplateOrigemVersao());
    copia.atualizarConteudo(duplicarAnexos(origem, copia));
    registrarRevisao(copia, usuario, TipoRevisaoPagina.DUPLICACAO,
        "Página criada a partir de uma duplicação.");
    for (Pagina filho : paginaRepository.findByParent_Id(origem.getId())) {
      duplicarRecursivo(filho, copia, usuario);
    }
    return copia;
  }

  /**
   * Copia os arquivos anexos para a nova página e devolve o HTML com os links
   * de download reapontados para os anexos recém-criados.
   */
  private String duplicarAnexos(Pagina origem, Pagina copia) {
    String html = origem.getConteudoHtml();
    for (PaginaAnexo anexo : paginaAnexoRepository.findByPagina_Id(origem.getId())) {
      PaginaAnexo novo = copiarAnexo(copia, anexo);
      if (novo == null) {
        continue;
      }
      html = reapontarAnexo(html, origem.getId(), anexo.getId(), copia.getId(), novo.getId());
    }
    return html;
  }

  /** Devolve {@code null} quando o arquivo da origem sumiu do disco — a cópia da página continua. */
  private PaginaAnexo copiarAnexo(Pagina copia, PaginaAnexo origem) {
    Path arquivoOrigem = Path.of(origem.getCaminho());
    if (!Files.exists(arquivoOrigem)) {
      log.warn("Anexo {} não encontrado em disco; não foi copiado para a página {}.",
          origem.getId(), copia.getId());
      return null;
    }
    Path destino = anexoStorage.novoArquivo(copia.getId(), origem.getNomeOriginal(),
        origem.getContentType());
    try {
      Files.createDirectories(destino.getParent());
      Files.copy(arquivoOrigem, destino);
    } catch (java.io.IOException ex) {
      throw new BusinessException("Falha ao copiar anexo da página: " + ex.getMessage());
    }
    return paginaAnexoRepository.save(new PaginaAnexo(copia, origem.getNomeOriginal(),
        origem.getContentType(), origem.getTamanhoBytes(), destino.toString()));
  }

  private String reapontarAnexo(String html, UUID origemPaginaId, UUID origemAnexoId,
      UUID copiaPaginaId, UUID copiaAnexoId) {
    if (html == null || html.isBlank()) {
      return html;
    }
    return html.replace(
        "/paginas/" + origemPaginaId + "/anexos/" + origemAnexoId + "/download",
        "/paginas/" + copiaPaginaId + "/anexos/" + copiaAnexoId + "/download");
  }

  @Transactional
  public void reordenar(List<UUID> paginaIds, Principal principal) {
    if (paginaIds.isEmpty() || paginaIds.size() != paginaIds.stream().distinct().count()) {
      throw new BusinessException("A ordenação deve conter páginas distintas.");
    }
    // findAllById não devolve na ordem pedida; a ordenação é justamente o dado
    // que o cliente enviou, então reindexa pela posição do id na requisição.
    Map<UUID, Pagina> porId = buscarTodos(paginaIds).stream()
        .collect(java.util.stream.Collectors.toMap(Pagina::getId, pagina -> pagina));
    List<Pagina> paginas = paginaIds.stream().map(porId::get).toList();
    UUID moduloId = paginas.getFirst().getModulo().getId();
    UUID parentId = paginas.getFirst().getParent() == null ? null : paginas.getFirst().getParent().getId();
    if (paginas.stream().anyMatch(p -> !p.getModulo().getId().equals(moduloId)
        || !java.util.Objects.equals(parentId, p.getParent() == null ? null : p.getParent().getId()))) {
      throw new BusinessException("Só é possível reordenar páginas do mesmo módulo e nível.");
    }
    for (int i = 0; i < paginas.size(); i++) {
      paginas.get(i).definirOrdem(i);
    }
    auditoriaService.registrar("PAGINA", null, "REORDENAR", "Páginas reordenadas.", principal);
  }

  /**
   * Quando a revisão tem dono, é ele quem aprova. Sem responsável definido, o
   * fluxo antigo continua valendo e qualquer editor aprova.
   */
  private void validarDonoDaRevisao(Pagina pagina, Principal principal) {
    String revisor = pagina.getRevisorUsername();
    if (revisor == null || revisor.isBlank()) {
      return;
    }
    if (!revisor.equalsIgnoreCase(username(principal))) {
      throw new BusinessException(
          "Esta revisão está atribuída a " + revisor + " e só pode ser aprovada por essa pessoa.");
    }
  }

  /** Avisa quem enviou a página para revisão — não quem aprovou. */
  private void notificarAutorDoEnvio(Pagina pagina) {
    paginaRevisaoRepository
        .findFirstByPagina_IdAndTipoOrderByNumeroDesc(pagina.getId(), TipoRevisaoPagina.ENVIO_REVISAO)
        .map(PaginaRevisao::getCreatedBy)
        .ifPresent(autor -> notificacaoEmailService.notificarPaginaAprovada(pagina, autor));
  }

  private void sincronizarIndicePaiSeAplicavel(Pagina pagina, Principal principal) {
    if (pagina.getParent() == null) {
      return;
    }
    try {
      sincronizarIndicePai(pagina.getParent(), principal);
    } catch (Exception ex) {
      log.warn("Falha ao sincronizar índice de guias do pai {}: {}",
          pagina.getParent().getId(), ex.getMessage());
    }
  }

  private void sincronizarIndicePai(Pagina parent, Principal principal) {
    if (!temSecaoGuiasDisponiveis(parent.getConteudoHtml())) {
      return;
    }
    List<Pagina> filhos = paginaRepository.findByParent_Id(parent.getId()).stream()
        .sorted(Comparator.comparingInt(Pagina::getOrdem).thenComparing(Pagina::getTitulo))
        .toList();
    String secaoHtml = montarSecaoGuiasDisponiveis(filhos);
    String novoHtml = substituirOuAdicionarSecaoGuias(parent.getConteudoHtml(), secaoHtml);
    if (novoHtml.equals(parent.getConteudoHtml())) {
      return;
    }
    parent.atualizar(parent.getTitulo(), parent.getSlug(), parent.getCodigoTela(), parent.getResumo(),
        novoHtml, parent.getOrdem(), parent.isAtivo(), parent.getModulo(), parent.getParent());
    paginaRepository.save(parent);
    registrarRevisao(parent, username(principal), TipoRevisaoPagina.SALVAMENTO_MANUAL,
        "Índice de guias sincronizado.");
  }

  private boolean temSecaoGuiasDisponiveis(String html) {
    if (html == null || html.isBlank()) {
      return false;
    }
    Document doc = Jsoup.parseBodyFragment(html);
    return doc.select("section").stream()
        .anyMatch(secao -> {
          Element h2 = secao.selectFirst("h2");
          return h2 != null && "guias disponíveis".equals(h2.text().trim().toLowerCase(Locale.ROOT));
        });
  }

  private String montarSecaoGuiasDisponiveis(List<Pagina> filhos) {
    StringBuilder items = new StringBuilder();
    for (int i = 0; i < filhos.size(); i++) {
      Pagina filho = filhos.get(i);
      String resumo = filho.getResumo() == null || filho.getResumo().isBlank()
          ? "Sem resumo"
          : filho.getResumo().trim();
      if (resumo.length() > 80) {
        resumo = resumo.substring(0, 80);
      }
      items.append("<article class=\"resource-item\"><span class=\"number-badge\">").append(i + 1)
          .append("</span><span><strong>").append(HtmlUtils.htmlEscape(filho.getTitulo()))
          .append("</strong><small>").append(HtmlUtils.htmlEscape(resumo))
          .append("</small></span><span class=\"resource-item__meta\">")
          .append(HtmlUtils.htmlEscape(filho.getCodigoTela()))
          .append("</span></article>");
    }
    return "<section class=\"doc-section\"><h2>Guias disponíveis</h2>"
        + "<div class=\"resource-list resource-list--large\">" + items + "</div></section>";
  }

  private String substituirOuAdicionarSecaoGuias(String html, String secaoHtml) {
    if (html == null || html.isBlank()) {
      return secaoHtml;
    }
    Document doc = Jsoup.parseBodyFragment(html);
    Element existente = doc.select("section").stream()
        .filter(secao -> {
          Element h2 = secao.selectFirst("h2");
          return h2 != null && "guias disponíveis".equals(h2.text().trim().toLowerCase(Locale.ROOT));
        })
        .findFirst()
        .orElse(null);
    Element novaSecao = Jsoup.parseBodyFragment(secaoHtml).body().child(0);
    if (existente != null) {
      existente.replaceWith(novaSecao);
    } else {
      doc.body().appendChild(novaSecao);
    }
    return doc.body().html();
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
        .addAttributes("img", "src", "alt", "title", "loading")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https", "data")
        // Mantém src relativo (/api/.../anexos/{id}/download) usado pelo editor e pacote.
        // Jsoup exige baseUri para validar o protocolo de links relativos.
        .preserveRelativeLinks(true);
    return Jsoup.clean(html, "https://localhost/", safelist);
  }

  private String username(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }

  private PaginaRevisao registrarRevisao(Pagina pagina, String username, TipoRevisaoPagina tipo,
      String descricao) {
    int numero = paginaRevisaoRepository.countByPagina_Id(pagina.getId()) + 1;
    return paginaRevisaoRepository.save(new PaginaRevisao(pagina, numero, username, tipo, descricao));
  }

  /**
   * Conteúdo aprovado ou publicado só muda depois de voltar para rascunho. O pacote publica o
   * conteúdo atual das páginas {@code PUBLICADO}; sem esta trava, uma edição entraria na próxima
   * publicação sem passar por revisão. Metadados (ordem, módulo, pai, ativo) seguem livres.
   */
  private void exigirConteudoEditavel(
      Pagina pagina, PaginaRequest request, String slug, String conteudoHtml) {
    if (pagina.getStatus() != StatusPagina.APROVADO && pagina.getStatus() != StatusPagina.PUBLICADO) {
      return;
    }
    boolean conteudoAlterado = !Objects.equals(pagina.getTitulo(), request.titulo().trim())
        || !Objects.equals(pagina.getSlug(), slug)
        || !Objects.equals(pagina.getCodigoTela(), request.codigoTela().trim())
        || !Objects.equals(textoOuNulo(pagina.getResumo()), textoOuNulo(request.resumo()))
        || !Objects.equals(htmlNormalizado(pagina.getConteudoHtml()), htmlNormalizado(conteudoHtml));
    if (conteudoAlterado) {
      throw new BusinessException(
          "Página " + (pagina.getStatus() == StatusPagina.PUBLICADO ? "publicada" : "aprovada")
              + ": volte para rascunho antes de alterar o conteúdo. Ela sai das próximas "
              + "publicações até ser aprovada e publicada de novo.");
    }
  }

  private static String textoOuNulo(String valor) {
    return valor == null || valor.isBlank() ? null : valor;
  }

  /** O editor reformata o HTML ao carregar (espaços, ordem de atributos); isso não é edição. */
  private String htmlNormalizado(String html) {
    String sanitizado = sanitizar(html);
    if (sanitizado == null || sanitizado.isBlank()) {
      return null;
    }
    Document documento = Jsoup.parseBodyFragment(sanitizado);
    documento.outputSettings().prettyPrint(false);
    return documento.body().html().replaceAll("\\s+", " ").replaceAll("> <", "><").trim();
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
        predicates.add(predicadoDeBusca(root, criteriaBuilder, buscaFiltro));
      }
      return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  /**
   * Busca livre: o índice de texto resolve palavras inteiras (com radical), e o
   * LIKE cobre o que o editor digita parcialmente — "cad" ainda encontra
   * "cadastro". Os dois combinados evitam a busca vazia que o tsquery sozinho
   * devolveria.
   */
  private Predicate predicadoDeBusca(jakarta.persistence.criteria.Root<Pagina> root,
      jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, String termo) {
    List<Predicate> alternativas = new ArrayList<>();
    List<UUID> idsPorTexto = paginaRepository.buscarIdsPorTexto(termo);
    if (!idsPorTexto.isEmpty()) {
      alternativas.add(root.get("id").in(idsPorTexto));
    }
    alternativas.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), "%" + termo + "%"));
    alternativas.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), "%" + termo + "%"));
    alternativas.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("codigoTela")), "%" + termo + "%"));
    alternativas.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("resumo")), "%" + termo + "%"));
    return criteriaBuilder.or(alternativas.toArray(Predicate[]::new));
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
