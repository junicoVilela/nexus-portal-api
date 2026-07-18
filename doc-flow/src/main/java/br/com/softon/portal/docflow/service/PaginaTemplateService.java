package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.dto.request.PaginaTemplateAplicacaoRequest;
import br.com.softon.portal.docflow.dto.request.PaginaTemplateDuplicarRequest;
import br.com.softon.portal.docflow.dto.request.PaginaTemplateRequest;
import br.com.softon.portal.docflow.dto.response.PaginaTemplateAplicacaoResponse;
import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.entity.PaginaTemplate;
import br.com.softon.portal.docflow.entity.PaginaTemplateVersao;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.repository.ClienteProjetoRepository;
import br.com.softon.portal.docflow.repository.ClienteRepository;
import br.com.softon.portal.docflow.repository.ModuloRepository;
import br.com.softon.portal.docflow.repository.PaginaRepository;
import br.com.softon.portal.docflow.repository.PaginaTemplateRepository;
import br.com.softon.portal.docflow.repository.PaginaTemplateVersaoRepository;
import br.com.softon.portal.docflow.repository.ProjetoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import br.com.softon.rbac.service.AuditoriaService;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaginaTemplateService {

  private static final Pattern VARIAVEL = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");
  private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final PaginaTemplateRepository paginaTemplateRepository;
  private final PaginaTemplateVersaoRepository paginaTemplateVersaoRepository;
  private final ProjetoRepository projetoRepository;
  private final ClienteRepository clienteRepository;
  private final ModuloRepository moduloRepository;
  private final ClienteProjetoRepository clienteProjetoRepository;
  private final PaginaRepository paginaRepository;
  private final AuditoriaService auditoriaService;

  public List<PaginaTemplate> listarAtivos() {
    return paginaTemplateRepository.findByAtivoTrueOrderByOrdemAscNomeAsc();
  }

  public List<PaginaTemplate> listar(UUID projetoId, UUID clienteId, boolean somenteContexto,
      boolean incluirArquivados) {
    return paginaTemplateRepository.listar(projetoId, clienteId, somenteContexto, incluirArquivados);
  }

  public long paginasOriginadas(UUID templateId) {
    return paginaRepository.countByTemplateOrigemId(templateId);
  }

  public long paginasOriginadas(UUID templateId, int versao) {
    return paginaRepository.countByTemplateOrigemIdAndTemplateOrigemVersao(templateId, versao);
  }

  @Transactional
  public PaginaTemplate criar(PaginaTemplateRequest request, Principal principal) {
    Escopo escopo = escopo(request.projetoId(), request.clienteId());
    String conteudoHtml = conteudoValido(request.conteudoHtml());
    UUID codigoId = UUID.randomUUID();
    PaginaTemplate template = paginaTemplateRepository.save(new PaginaTemplate(
        "CUSTOM_" + codigoId.toString().replace("-", ""),
        request.nome().trim(), textoOpcional(request.descricao()), conteudoHtml, 1000,
        escopo.projeto(), escopo.cliente()));
    registrarVersao(template);
    auditoriaService.registrar("PAGINA_TEMPLATE", template.getId(), "CRIAR",
        "Modelo personalizado criado: " + template.getNome(), principal);
    return template;
  }

  @Transactional
  public PaginaTemplate atualizar(UUID id, PaginaTemplateRequest request, Principal principal) {
    PaginaTemplate template = personalizado(id);
    Escopo escopo = escopo(request.projetoId(), request.clienteId());
    template.atualizar(request.nome().trim(), textoOpcional(request.descricao()),
        conteudoValido(request.conteudoHtml()), escopo.projeto(), escopo.cliente());
    registrarVersao(template);
    auditoriaService.registrar("PAGINA_TEMPLATE", id, "ATUALIZAR",
        "Modelo personalizado atualizado para a versão " + template.getVersaoAtual() + ": " + template.getNome(),
        principal);
    return template;
  }

  @Transactional
  public PaginaTemplate duplicar(UUID id, PaginaTemplateDuplicarRequest request, Principal principal) {
    PaginaTemplate origem = buscar(id);
    Escopo escopo = escopo(request.projetoId(), request.clienteId());
    PaginaTemplate copia = paginaTemplateRepository.save(new PaginaTemplate(
        "CUSTOM_" + UUID.randomUUID().toString().replace("-", ""),
        request.nome().trim(), origem.getDescricao(), origem.getConteudoHtml(), 1000,
        escopo.projeto(), escopo.cliente()));
    registrarVersao(copia);
    auditoriaService.registrar("PAGINA_TEMPLATE", copia.getId(), "DUPLICAR",
        origem.getNome() + " -> " + copia.getNome(), principal);
    return copia;
  }

  @Transactional
  public PaginaTemplate definirArquivado(UUID id, boolean arquivado, Principal principal) {
    PaginaTemplate template = personalizado(id);
    boolean ativo = !arquivado;
    if (template.isAtivo() == ativo) {
      return template;
    }
    template.definirAtivo(ativo);
    registrarVersao(template);
    auditoriaService.registrar("PAGINA_TEMPLATE", id, arquivado ? "ARQUIVAR" : "RESTAURAR",
        (arquivado ? "Modelo arquivado: " : "Modelo reativado: ") + template.getNome(), principal);
    return template;
  }

  public List<PaginaTemplateVersao> listarVersoes(UUID id) {
    buscar(id);
    return paginaTemplateVersaoRepository.findByTemplate_IdOrderByNumeroDesc(id);
  }

  @Transactional
  public PaginaTemplate restaurarVersao(UUID id, int numero, Principal principal) {
    PaginaTemplate template = personalizado(id);
    PaginaTemplateVersao versao = paginaTemplateVersaoRepository.findByTemplate_IdAndNumero(id, numero)
        .orElseThrow(() -> new NotFoundException("Versão do modelo não encontrada."));
    Escopo escopo = escopo(versao.getProjetoId(), versao.getClienteId());
    template.restaurar(versao.getNome(), versao.getDescricao(), versao.getConteudoHtml(), versao.isAtivo(),
        escopo.projeto(), escopo.cliente());
    registrarVersao(template);
    auditoriaService.registrar("PAGINA_TEMPLATE", id, "RESTAURAR_VERSAO",
        "Versão " + numero + " restaurada como versão " + template.getVersaoAtual(), principal);
    return template;
  }

  public PaginaTemplateAplicacaoResponse aplicar(UUID id, PaginaTemplateAplicacaoRequest request) {
    PaginaTemplate template = buscar(id);
    if (!template.isAtivo()) {
      throw new BusinessException("Modelos arquivados não podem ser aplicados.");
    }
    Contexto contexto = contexto(template, request);
    Map<String, String> valores = new LinkedHashMap<>();
    adicionar(valores, "cliente.nome", contexto.cliente() == null ? null : contexto.cliente().getNome());
    adicionar(valores, "projeto.nome", contexto.projeto() == null ? null : contexto.projeto().getNome());
    adicionar(valores, "modulo.nome", contexto.modulo() == null ? null : contexto.modulo().getNome());
    adicionar(valores, "pagina.titulo", request.titulo());
    adicionar(valores, "pagina.codigo", request.codigoTela());
    adicionar(valores, "data.atual", LocalDate.now().format(DATA_BR));

    Matcher matcher = VARIAVEL.matcher(template.getConteudoHtml());
    StringBuilder resolvido = new StringBuilder();
    while (matcher.find()) {
      String valor = valores.get(matcher.group(1));
      matcher.appendReplacement(resolvido, valor == null ? Matcher.quoteReplacement(matcher.group())
          : Matcher.quoteReplacement(escapeHtml(valor)));
    }
    matcher.appendTail(resolvido);
    List<String> pendentes = VARIAVEL.matcher(resolvido).results()
        .map(resultado -> resultado.group(1)).distinct().toList();
    return new PaginaTemplateAplicacaoResponse(template.getId(), template.getVersaoAtual(), resolvido.toString(),
        Map.copyOf(valores), pendentes);
  }

  @Transactional
  public void excluir(UUID id, Principal principal) {
    PaginaTemplate template = personalizado(id);
    paginaTemplateRepository.delete(template);
    auditoriaService.registrar("PAGINA_TEMPLATE", id, "EXCLUIR",
        "Modelo personalizado excluído: " + template.getNome(), principal);
  }

  private PaginaTemplate buscar(UUID id) {
    return paginaTemplateRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Modelo de página não encontrado."));
  }

  private PaginaTemplate personalizado(UUID id) {
    PaginaTemplate template = buscar(id);
    if (!template.isPersonalizado()) {
      throw new BusinessException("Modelos de sistema não podem ser alterados ou excluídos.");
    }
    return template;
  }

  private Contexto contexto(PaginaTemplate template, PaginaTemplateAplicacaoRequest request) {
    Modulo modulo = request.moduloId() == null ? null : moduloRepository.findById(request.moduloId())
        .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
    UUID projetoId = request.projetoId() != null ? request.projetoId()
        : modulo != null ? modulo.getProjeto().getId()
        : template.getProjeto() == null ? null : template.getProjeto().getId();
    Projeto projeto = projetoId == null ? null : projetoRepository.findById(projetoId)
        .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
    UUID clienteId = request.clienteId() != null ? request.clienteId()
        : template.getCliente() == null ? null : template.getCliente().getId();
    Cliente cliente = clienteId == null ? null : clienteRepository.findById(clienteId)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

    if (modulo != null && projeto != null && !modulo.getProjeto().getId().equals(projeto.getId())) {
      throw new BusinessException("O módulo não pertence ao projeto informado.");
    }
    if (template.getProjeto() != null && projeto != null && !template.getProjeto().getId().equals(projeto.getId())) {
      throw new BusinessException("Este modelo está disponível somente para o projeto "
          + template.getProjeto().getNome() + ".");
    }
    if (template.getCliente() != null) {
      if (cliente != null && !template.getCliente().getId().equals(cliente.getId())) {
        throw new BusinessException("Este modelo está disponível somente para o cliente "
            + template.getCliente().getNome() + ".");
      }
      if (projeto != null && !clienteProjetoRepository.existsByCliente_IdAndProjeto_Id(
          template.getCliente().getId(), projeto.getId())) {
        throw new BusinessException("O projeto selecionado não está vinculado ao cliente deste modelo.");
      }
    }
    return new Contexto(projeto, modulo, cliente);
  }

  private Escopo escopo(UUID projetoId, UUID clienteId) {
    Projeto projeto = projetoId == null ? null : projetoRepository.findById(projetoId)
        .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
    Cliente cliente = clienteId == null ? null : clienteRepository.findById(clienteId)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    if ((projeto == null) == (cliente == null)) {
      throw new BusinessException("Informe somente um projeto ou um cliente para o modelo.");
    }
    return new Escopo(projeto, cliente);
  }

  private void registrarVersao(PaginaTemplate template) {
    paginaTemplateVersaoRepository.save(new PaginaTemplateVersao(template));
  }

  private String conteudoValido(String html) {
    String conteudo = sanitizar(html);
    if (conteudo.isBlank()) {
      throw new BusinessException("O conteúdo do modelo não pode ficar vazio.");
    }
    return conteudo;
  }

  private String sanitizar(String html) {
    Safelist safelist = Safelist.relaxed()
        .addTags("section", "article", "aside", "figure", "figcaption")
        .addAttributes(":all", "class")
        .addAttributes("img", "src", "alt", "title")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https", "data");
    return Jsoup.clean(html, safelist);
  }

  private String textoOpcional(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }

  private void adicionar(Map<String, String> valores, String chave, String valor) {
    if (valor != null && !valor.isBlank()) {
      valores.put(chave, valor.trim());
    }
  }

  private String escapeHtml(String valor) {
    return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }

  private record Escopo(Projeto projeto, Cliente cliente) {
  }

  private record Contexto(Projeto projeto, Modulo modulo, Cliente cliente) {
  }
}
