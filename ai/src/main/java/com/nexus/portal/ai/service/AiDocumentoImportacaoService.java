package com.nexus.portal.ai.service;

import static com.nexus.portal.ai.service.AiDocumentoPlanoOperacoes.aplicarSugestao;
import static com.nexus.portal.ai.service.AiDocumentoPlanoOperacoes.atualizarContexto;
import static com.nexus.portal.ai.service.AiDocumentoPlanoOperacoes.atualizarPagina;
import static com.nexus.portal.ai.service.AiDocumentoPlanoOperacoes.copiarPagina;
import static com.nexus.portal.ai.service.AiDocumentoPlanoOperacoes.montarBriefing;
import static com.nexus.portal.ai.service.AiDocumentoPlanoOperacoes.removerCabecalho;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.dto.request.AiAtualizarComposicaoDocumentoRequest;
import com.nexus.portal.ai.dto.request.AiConfirmarEstruturaDocumentoRequest;
import com.nexus.portal.ai.dto.request.AiGerarLoteDocumentoRequest;
import com.nexus.portal.ai.dto.request.AiReordenarEstruturaDocumentoRequest;
import com.nexus.portal.ai.dto.request.AiTemplateRecomendacaoRequest;
import com.nexus.portal.ai.dto.request.AplicarAiPropostaRequest;
import com.nexus.portal.ai.dto.request.CriarAiSessaoRequest;
import com.nexus.portal.ai.dto.response.AiEstimativaLoteDocumentoResponse;
import com.nexus.portal.ai.dto.response.AiImportacaoDocumentoResponse;
import com.nexus.portal.ai.dto.response.AiImportacaoResumoResponse;
import com.nexus.portal.ai.dto.response.AiTemplateRecomendacaoResponse;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiDocumentoClienteModo;
import com.nexus.portal.ai.entity.AiDocumentoProjetoModo;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoStatus;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoTipo;
import com.nexus.portal.ai.entity.AiImportacaoStatus;
import com.nexus.portal.ai.entity.AiPaginaPlanoOrigem;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.entity.AiObjetivo;
import com.nexus.portal.ai.entity.AiSessaoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiDocumentoImportacaoRepository;
import com.nexus.portal.ai.repository.AiSessaoRepository;
import com.nexus.portal.ai.service.AiDocumentoExtratorService.DocumentoExtraido;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.ConflictException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.portal.shared.util.SlugUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiDocumentoImportacaoService {

  private final AiDocumentoImportacaoRepository repository;
  private final AiDocumentoExtratorService extratorService;
  private final AiDocumentoPlanejadorService planejadorService;
  private final ObjectMapper objectMapper;
  private final AuditoriaService auditoriaService;
  private final DocFlowAiBridge docFlowAiBridge;
  private final AiDocumentoAnaliseWorkerService analiseWorkerService;
  private final AiSessaoService sessaoService;
  private final AiPropostaService propostaService;
  private final AiSessaoRepository sessaoRepository;
  private final AiProperties aiProperties;
  private final AiTemplateRecomendacaoService templateRecomendacaoService;

  public AiDocumentoImportacaoService(
      AiDocumentoImportacaoRepository repository,
      AiDocumentoExtratorService extratorService,
      AiDocumentoPlanejadorService planejadorService,
      ObjectMapper objectMapper,
      AuditoriaService auditoriaService,
      DocFlowAiBridge docFlowAiBridge,
      AiDocumentoAnaliseWorkerService analiseWorkerService,
      AiSessaoService sessaoService,
      AiPropostaService propostaService,
      AiSessaoRepository sessaoRepository,
      AiProperties aiProperties,
      AiTemplateRecomendacaoService templateRecomendacaoService) {
    this.repository = repository;
    this.extratorService = extratorService;
    this.planejadorService = planejadorService;
    this.objectMapper = objectMapper;
    this.auditoriaService = auditoriaService;
    this.docFlowAiBridge = docFlowAiBridge;
    this.analiseWorkerService = analiseWorkerService;
    this.sessaoService = sessaoService;
    this.propostaService = propostaService;
    this.sessaoRepository = sessaoRepository;
    this.aiProperties = aiProperties;
    this.templateRecomendacaoService = templateRecomendacaoService;
  }

  @Transactional
  public AiImportacaoDocumentoResponse importar(
      MultipartFile arquivo,
      UUID projetoId,
      UUID clienteId,
      Principal principal) {
    return importar(arquivo, projetoId, clienteId, false, principal);
  }

  /**
   * Importa o documento. Se o mesmo arquivo (hash) já tem importação em andamento do usuário e
   * {@code novaImportacao} é falso, devolve essa importação ({@code retomada = true}) em vez de
   * criar outra paralela — que, ao confirmar a estrutura, colidiria com o projeto já criado.
   */
  @Transactional
  public AiImportacaoDocumentoResponse importar(
      MultipartFile arquivo,
      UUID projetoId,
      UUID clienteId,
      boolean novaImportacao,
      Principal principal) {
    String hash = sha256(arquivo);
    if (!novaImportacao) {
      var existente = repository.findFirstByCreatedByAndHashSha256AndStatusNotOrderByUpdatedAtDesc(
          usuario(principal), hash, AiImportacaoStatus.CONCLUIDA);
      if (existente.isPresent()) {
        AiDocumentoImportacao importacao = existente.get();
        return response(importacao, lerPlano(importacao), lerAvisos(importacao)).comoRetomada();
      }
    }
    DocumentoExtraido extraido = extratorService.extrair(arquivo);
    AiDocumentoPlano plano = planejadorService.planejar(extraido, projetoId, clienteId);
    plano = new AiDocumentoPlano(
        plano.projetoNome(),
        plano.projetoDescricao(),
        plano.projetoId(),
        plano.clienteId(),
        false,
        plano.modulos(),
        List.of(plano.projetoNome()),
        com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem.ESTRUTURAL,
        "Análise semântica em andamento.",
        null,
        null,
        List.of());
    var importacao = new AiDocumentoImportacao(
        extraido.nomeArquivo(),
        extraido.tipo(),
        mimeType(arquivo),
        arquivo.getSize(),
        hash,
        extraido.texto(),
        extraido.totalPaginasOrigem(),
        escrever(plano),
        escrever(extraido.avisos()));
    repository.saveAndFlush(importacao);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_IMPORTADO,
        "Documento " + extraido.tipo() + " · " + extraido.texto().length() + " caracteres",
        principal);
    UUID importacaoId = importacao.getId();
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        analiseWorkerService.analisar(importacaoId);
      }
    });
    return response(importacao, plano, extraido.avisos());
  }

  /** Importações não concluídas do usuário, para retomar de onde parou. */
  @Transactional(readOnly = true)
  public List<AiImportacaoResumoResponse> emAndamento(Principal principal) {
    return repository.findTop10ByCreatedByAndStatusNotOrderByUpdatedAtDesc(
            usuario(principal), AiImportacaoStatus.CONCLUIDA)
        .stream()
        .map(importacao -> {
          AiDocumentoPlano plano = lerPlano(importacao);
          List<AiDocumentoPlano.Pagina> paginas = plano.modulos().stream()
              .flatMap(modulo -> modulo.paginas().stream())
              .toList();
          return new AiImportacaoResumoResponse(
              importacao.getId(),
              importacao.getNomeArquivo(),
              plano.projetoNome(),
              importacao.getStatus(),
              plano.estruturaConfirmada(),
              paginas.size(),
              (int) paginas.stream()
                  .filter(pagina -> pagina.status() == AiPaginaPlanoStatus.REVISADA)
                  .count(),
              importacao.getUpdatedAt());
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public AiImportacaoDocumentoResponse buscar(UUID id, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    return response(importacao, lerPlano(importacao), lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse aceitarSugestao(
      UUID id, UUID sugestaoId, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = exigirPlanoEditavel(importacao);
    AiDocumentoPlano.Sugestao sugestao = encontrarSugestao(atual, sugestaoId);
    if (sugestao.status() == AiDocumentoSugestaoStatus.APLICADA) {
      return response(importacao, atual, lerAvisos(importacao));
    }
    if (sugestao.status() == AiDocumentoSugestaoStatus.IGNORADA) {
      throw new BusinessException("A sugestão já foi ignorada e não pode mais ser aplicada.");
    }

    List<AiDocumentoPlano.Modulo> modulos = aplicarSugestao(atual, sugestao);
    List<AiDocumentoPlano.Sugestao> sugestoes = atualizarStatusSugestoes(
        atual.sugestoes(), sugestao, AiDocumentoSugestaoStatus.APLICADA);
    AiDocumentoPlano atualizado = copiarPlano(atual, modulos, sugestoes);
    persistirPlanoEditavel(importacao, atualizado);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_SUGESTAO_APLICADA,
        sugestao.tipo() + " · " + sugestao.titulo(),
        principal);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse ignorarSugestao(
      UUID id, UUID sugestaoId, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = exigirPlanoEditavel(importacao);
    AiDocumentoPlano.Sugestao sugestao = encontrarSugestao(atual, sugestaoId);
    if (sugestao.status() == AiDocumentoSugestaoStatus.IGNORADA) {
      return response(importacao, atual, lerAvisos(importacao));
    }
    if (sugestao.status() == AiDocumentoSugestaoStatus.APLICADA) {
      throw new BusinessException("A sugestão já foi aplicada e não pode mais ser ignorada.");
    }

    List<AiDocumentoPlano.Sugestao> sugestoes = atual.sugestoes().stream()
        .map(item -> item.id().equals(sugestaoId)
            ? item.comStatus(AiDocumentoSugestaoStatus.IGNORADA)
            : item)
        .toList();
    AiDocumentoPlano atualizado = copiarPlano(atual, atual.modulos(), sugestoes);
    persistirPlanoEditavel(importacao, atualizado);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_SUGESTAO_IGNORADA,
        sugestao.tipo() + " · " + sugestao.titulo(),
        principal);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse aplicarSugestoesSeguras(UUID id, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atualizado = exigirPlanoEditavel(importacao);
    int aplicadas = 0;
    for (AiDocumentoPlano.Sugestao sugestao : List.copyOf(atualizado.sugestoes())) {
      if (sugestao.status() != AiDocumentoSugestaoStatus.PENDENTE
          || !sugestao.aplicacaoSegura()) {
        continue;
      }
      List<AiDocumentoPlano.Modulo> modulos = aplicarSugestao(atualizado, sugestao);
      List<AiDocumentoPlano.Sugestao> sugestoes = atualizarStatusSugestoes(
          atualizado.sugestoes(), sugestao, AiDocumentoSugestaoStatus.APLICADA);
      atualizado = copiarPlano(atualizado, modulos, sugestoes);
      aplicadas++;
    }
    if (aplicadas == 0) {
      return response(importacao, atualizado, lerAvisos(importacao));
    }
    persistirPlanoEditavel(importacao, atualizado);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_SUGESTOES_SEGURAS_APLICADAS,
        aplicadas + " sugestões aplicadas",
        principal);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse reordenarEstrutura(
      UUID id,
      AiReordenarEstruturaDocumentoRequest request,
      Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = exigirPlanoEditavel(importacao);
    if (importacao.getVersion() != request.version()) {
      throw new ConflictException(
          "A estrutura foi alterada em outra tela. Recarregue o plano antes de reorganizar.");
    }

    Map<UUID, AiDocumentoPlano.Modulo> modulosAtuais = atual.modulos().stream()
        .collect(LinkedHashMap::new, (map, modulo) -> map.put(modulo.id(), modulo), Map::putAll);
    Map<UUID, AiDocumentoPlano.Pagina> paginasAtuais = atual.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .collect(LinkedHashMap::new, (map, pagina) -> map.put(pagina.id(), pagina), Map::putAll);
    Set<UUID> modulosRecebidos = new HashSet<>();
    Set<UUID> paginasRecebidas = new HashSet<>();
    Set<String> nomesRecebidos = new HashSet<>();
    List<AiDocumentoPlano.Modulo> modulos = new ArrayList<>();
    int totalPaginas = 0;
    int paginasCriadas = 0;
    int paginasAlteradas = 0;

    for (int indiceModulo = 0; indiceModulo < request.modulos().size(); indiceModulo++) {
      AiReordenarEstruturaDocumentoRequest.Modulo recebido = request.modulos().get(indiceModulo);
      if (!modulosRecebidos.add(recebido.planoId())) {
        throw new BusinessException("A organização contém um módulo repetido.");
      }
      AiDocumentoPlano.Modulo moduloAtual = modulosAtuais.get(recebido.planoId());
      String nomeModulo = moduloAtual == null ? normalizar(recebido.nome()) : moduloAtual.nome();
      if (nomeModulo == null || nomeModulo.isBlank()) {
        throw new BusinessException("Informe um nome para todos os módulos.");
      }
      if (!nomesRecebidos.add(SlugUtils.normalize(nomeModulo))) {
        throw new BusinessException("Use nomes diferentes para os módulos do documento.");
      }
      List<AiDocumentoPlano.Pagina> paginas = new ArrayList<>();
      Set<String> titulosRecebidos = new HashSet<>();
      for (int indicePagina = 0; indicePagina < recebido.paginas().size(); indicePagina++) {
        AiReordenarEstruturaDocumentoRequest.Pagina paginaRecebida =
            recebido.paginas().get(indicePagina);
        UUID paginaId = paginaRecebida.planoId();
        if (!paginasRecebidas.add(paginaId)) {
          throw new BusinessException("A organização contém uma página repetida.");
        }
        String titulo = normalizar(paginaRecebida.titulo());
        String conteudo = normalizarConteudo(paginaRecebida.conteudo());
        if (!titulosRecebidos.add(SlugUtils.normalize(titulo))) {
          throw new BusinessException("Use títulos diferentes para as páginas de cada módulo.");
        }
        if (++totalPaginas > AiDocumentoPlanejadorService.MAXIMO_PAGINAS) {
          throw new BusinessException(
              "O plano pode ter no máximo "
                  + AiDocumentoPlanejadorService.MAXIMO_PAGINAS + " páginas.");
        }

        AiDocumentoPlano.Pagina paginaAtual = paginasAtuais.get(paginaId);
        if (paginaAtual == null) {
          paginasCriadas++;
          paginas.add(novaPaginaRascunho(
              paginaId,
              titulo,
              indicePagina + 1,
              atual.projetoNome(),
              nomeModulo,
              conteudo,
              paginaRecebida.origem()));
          continue;
        }
        boolean conteudoAlterado = !paginaAtual.titulo().equals(titulo)
            || !normalizarConteudo(removerCabecalho(paginaAtual.briefing())).equals(conteudo);
        if (conteudoAlterado && !paginaEditavel(paginaAtual)) {
          throw new BusinessException(
              "A página '" + paginaAtual.titulo() + "' já foi gerada e não pode mais ser alterada.");
        }
        if (conteudoAlterado) {
          paginasAlteradas++;
          paginas.add(paginaAjustada(
              paginaAtual,
              titulo,
              indicePagina + 1,
              atual.projetoNome(),
              nomeModulo,
              conteudo,
              paginaRecebida.origem()));
          continue;
        }
        AiDocumentoPlano.Pagina contextualizada = atualizarContexto(
            paginaAtual, atual.projetoNome(), nomeModulo);
        paginas.add(copiarPagina(
            contextualizada,
            contextualizada.titulo(),
            contextualizada.briefing(),
            indicePagina + 1));
      }
      modulos.add(new AiDocumentoPlano.Modulo(
          recebido.planoId(),
          moduloAtual == null ? null : moduloAtual.moduloId(),
          nomeModulo,
          indiceModulo + 1,
          List.copyOf(paginas)));
    }
    if (totalPaginas == 0) {
      throw new BusinessException("O documento precisa manter pelo menos uma página.");
    }
    Set<UUID> paginasRemovidas = new HashSet<>(paginasAtuais.keySet());
    paginasRemovidas.removeAll(paginasRecebidas);
    for (UUID paginaId : paginasRemovidas) {
      AiDocumentoPlano.Pagina pagina = paginasAtuais.get(paginaId);
      if (!paginaEditavel(pagina)) {
        throw new BusinessException(
            "A página '" + pagina.titulo() + "' já foi gerada e não pode ser removida.");
      }
    }

    List<AiDocumentoPlano.Sugestao> sugestoes = resolverSugestoesDaEstrutura(
        atual.sugestoes(), modulos);
    AiDocumentoPlano atualizado = copiarPlano(atual, List.copyOf(modulos), sugestoes);
    persistirPlanoEditavel(importacao, atualizado);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_ESTRUTURA_REORDENADA,
        modulos.size() + " módulos · " + totalPaginas + " páginas · "
            + paginasCriadas + " criadas · " + paginasAlteradas + " alteradas · "
            + paginasRemovidas.size() + " removidas",
        principal);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse atualizarComposicao(
      UUID id,
      UUID paginaPlanoId,
      AiAtualizarComposicaoDocumentoRequest request,
      Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    if (importacao.getStatus() == AiImportacaoStatus.ANALISANDO_ESTRUTURA) {
      throw new BusinessException("Aguarde a conclusão da análise semântica do documento.");
    }
    if (importacao.getVersion() != request.version()) {
      throw new ConflictException(
          "A composição foi alterada em outra tela. Recarregue o plano antes de continuar.");
    }
    AiDocumentoPlano atual = lerPlano(importacao);
    AiDocumentoPlano.Pagina pagina = atual.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .filter(item -> item.id().equals(paginaPlanoId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Página não encontrada no plano importado."));
    if (!paginaEditavel(pagina)) {
      throw new BusinessException(
          "A composição da página não pode ser alterada depois que a geração foi iniciada.");
    }
    List<String> selecionados = templateRecomendacaoService.validarComponentes(
        pagina.briefing(),
        atual.projetoId(),
        atual.clienteId(),
        pagina.templateId(),
        request.componentesSelecionados());
    AiTemplateRecomendacaoResponse recomendacao = templateRecomendacaoService.recomendar(
        new AiTemplateRecomendacaoRequest(
            pagina.briefing(), atual.projetoId(), atual.clienteId(), pagina.templateId()));
    List<String> obrigatorios = recomendacao.componentes().stream()
        .filter(item -> item.obrigatorio())
        .map(item -> item.id())
        .toList();
    AiDocumentoPlano atualizado = atualizarPagina(
        atual,
        paginaPlanoId,
        item -> item.comComposicao(
            recomendacao.blueprintId(),
            recomendacao.blueprintNome(),
            selecionados,
            obrigatorios,
            true));
    if (atual.estruturaConfirmada()) persistirPlano(importacao, atualizado);
    else persistirPlanoEditavel(importacao, atualizado);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_COMPOSICAO_ATUALIZADA,
        pagina.titulo() + " · " + selecionados.size() + " componentes",
        principal);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse confirmarEstrutura(
      UUID id,
      AiConfirmarEstruturaDocumentoRequest request,
      Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano planoLido = lerPlano(importacao);
    if (importacao.getStatus() == AiImportacaoStatus.ANALISANDO_ESTRUTURA) {
      throw new BusinessException("Aguarde a conclusão da análise semântica do documento.");
    }
    AiDocumentoPlano atual = assegurarComposicoes(planoLido);
    if (atual.estruturaConfirmada()) {
      return response(importacao, atual, lerAvisos(importacao));
    }
    if (atual.modulos().stream().anyMatch(modulo -> modulo.paginas().isEmpty())) {
      throw new BusinessException(
          "Mova ao menos uma página para cada módulo ou remova os módulos vazios antes de continuar.");
    }

    boolean novoProjeto = request.modoProjeto() == AiDocumentoProjetoModo.NOVO_PROJETO;
    if (novoProjeto && (request.projetoNome() == null || request.projetoNome().isBlank())) {
      throw new BusinessException("Informe o nome do novo projeto.");
    }
    if (!novoProjeto && request.projetoId() == null) {
      throw new BusinessException("Selecione o projeto existente.");
    }
    if (request.modoCliente() == AiDocumentoClienteModo.CLIENTE_EXISTENTE
        && request.clienteId() == null) {
      throw new BusinessException("Selecione o cliente existente.");
    }
    if (request.modoCliente() == AiDocumentoClienteModo.NOVO_CLIENTE
        && (request.clienteNome() == null || request.clienteNome().isBlank())) {
      throw new BusinessException("Informe o nome do novo cliente.");
    }

    Map<UUID, AiConfirmarEstruturaDocumentoRequest.Modulo> modulosRecebidos = new LinkedHashMap<>();
    var nomesModulos = new HashSet<String>();
    for (var modulo : request.modulos()) {
      if (modulosRecebidos.putIfAbsent(modulo.planoId(), modulo) != null) {
        throw new BusinessException("O plano contém módulos repetidos.");
      }
      if (!nomesModulos.add(SlugUtils.normalize(modulo.nome()))) {
        throw new BusinessException("Use nomes diferentes para os módulos do documento.");
      }
    }
    List<UUID> idsPlanejados = atual.modulos().stream().map(AiDocumentoPlano.Modulo::id).toList();
    if (modulosRecebidos.size() != idsPlanejados.size()
        || !modulosRecebidos.keySet().containsAll(idsPlanejados)) {
      throw new BusinessException("Revise todos os módulos sugeridos antes de criar a estrutura.");
    }

    var estrutura = docFlowAiBridge.confirmarEstruturaDocumento(
        novoProjeto,
        request.projetoId(),
        normalizar(request.projetoNome()),
        normalizarDescricao(request.projetoDescricao(), atual.projetoDescricao()),
        request.modoCliente() == AiDocumentoClienteModo.NOVO_CLIENTE,
        request.modoCliente() == AiDocumentoClienteModo.CLIENTE_EXISTENTE ? request.clienteId() : null,
        normalizar(request.clienteNome()),
        atual.modulos().stream()
            .map(modulo -> new DocFlowAiBridge.ModuloDocumento(
                modulo.id(), modulosRecebidos.get(modulo.id()).nome().trim(), modulo.ordem()))
            .toList());
    Map<UUID, DocFlowAiBridge.ModuloDocumentoConfirmado> confirmados = estrutura.modulos().stream()
        .collect(LinkedHashMap::new, (map, modulo) -> map.put(modulo.planoId(), modulo), Map::putAll);
    List<AiDocumentoPlano.Modulo> modulos = atual.modulos().stream()
        .map(modulo -> {
          var confirmado = confirmados.get(modulo.id());
          List<AiDocumentoPlano.Pagina> paginas = modulo.paginas().stream()
              .map(pagina -> atualizarContexto(pagina, estrutura.projetoNome(), confirmado.nome()))
              .toList();
          return new AiDocumentoPlano.Modulo(
              modulo.id(), confirmado.moduloId(), confirmado.nome(), modulo.ordem(), paginas);
        })
        .toList();
    AiDocumentoPlano atualizado = new AiDocumentoPlano(
        estrutura.projetoNome(),
        normalizarDescricao(request.projetoDescricao(), atual.projetoDescricao()),
        estrutura.projetoId(),
        estrutura.clienteId(),
        true,
        modulos,
        atual.projetoNomesSugeridos(),
        atual.analiseOrigem(),
        atual.analiseMensagem(),
        atual.tokensEntradaAnalise(),
        atual.tokensSaidaAnalise(),
        atual.sugestoes());
    importacao.iniciarRevisao(escrever(atualizado));
    repository.flush();
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_ESTRUTURA_CONFIRMADA,
        "Projeto " + estrutura.projetoNome() + " · " + modulos.size() + " módulos",
        principal);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse selecionarPagina(
      UUID id,
      UUID paginaPlanoId,
      Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = lerPlano(importacao);
    if (!atual.estruturaConfirmada()) {
      throw new BusinessException("Confirme o projeto e os módulos antes de gerar as páginas.");
    }
    boolean encontrada = atual.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .anyMatch(pagina -> pagina.id().equals(paginaPlanoId));
    if (!encontrada) throw new NotFoundException("Página não encontrada no plano importado.");

    List<AiDocumentoPlano.Modulo> modulos = atual.modulos().stream()
        .map(modulo -> new AiDocumentoPlano.Modulo(
            modulo.id(),
            modulo.moduloId(),
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .map(pagina -> pagina.id().equals(paginaPlanoId)
                    ? pagina.comStatus(AiPaginaPlanoStatus.EM_EDICAO)
                    : pagina.status() == AiPaginaPlanoStatus.EM_EDICAO
                        ? pagina.comStatus(AiPaginaPlanoStatus.PENDENTE)
                        : pagina)
                .toList()))
        .toList();
    AiDocumentoPlano atualizado = new AiDocumentoPlano(
        atual.projetoNome(),
        atual.projetoDescricao(),
        atual.projetoId(),
        atual.clienteId(),
        true,
        modulos,
        atual.projetoNomesSugeridos(),
        atual.analiseOrigem(),
        atual.analiseMensagem(),
        atual.tokensEntradaAnalise(),
        atual.tokensSaidaAnalise(),
        atual.sugestoes());
    importacao.iniciarRevisao(escrever(atualizado));
    repository.flush();
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse vincularPagina(
      UUID id, UUID paginaPlanoId, UUID paginaId, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = lerPlano(importacao);
    if (!atual.estruturaConfirmada()) {
      throw new BusinessException("Confirme a estrutura antes de vincular páginas geradas.");
    }
    AiDocumentoPlano.Modulo moduloPlano = atual.modulos().stream()
        .filter(modulo -> modulo.paginas().stream().anyMatch(pagina -> pagina.id().equals(paginaPlanoId)))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Página não encontrada no plano importado."));
    var paginaDocFlow = docFlowAiBridge.buscarPaginaDocumento(paginaId);
    if (!atual.projetoId().equals(paginaDocFlow.projetoId())
        || !moduloPlano.moduloId().equals(paginaDocFlow.moduloId())) {
      throw new BusinessException("A página salva não pertence ao projeto e módulo planejados.");
    }

    AiPaginaPlanoStatus status = statusPaginaDocFlow(paginaDocFlow.status());
    AiDocumentoPlano atualizado = atualizarPagina(
        atual,
        paginaPlanoId,
        pagina -> pagina.comVinculo(paginaDocFlow.id(), status));
    persistirPlano(importacao, atualizado);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  /**
   * Aceita a proposta da página e cria o rascunho DocFlow na mesma transação do vínculo com o
   * plano importado. Também recupera com segurança uma proposta já aplicada caso uma tentativa
   * anterior tenha criado a página antes de atualizar o plano.
   */
  @Transactional
  public AiImportacaoDocumentoResponse aceitarPagina(
      UUID id, UUID paginaPlanoId, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = lerPlano(importacao);
    if (!atual.estruturaConfirmada()) {
      throw new BusinessException("Confirme a estrutura antes de aceitar páginas geradas.");
    }

    AiDocumentoPlano.Modulo moduloPlano = atual.modulos().stream()
        .filter(modulo -> modulo.paginas().stream().anyMatch(pagina -> pagina.id().equals(paginaPlanoId)))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Página não encontrada no plano importado."));
    AiDocumentoPlano.Pagina paginaPlano = moduloPlano.paginas().stream()
        .filter(pagina -> pagina.id().equals(paginaPlanoId))
        .findFirst()
        .orElseThrow();
    if (paginaPlano.paginaId() != null) {
      return response(importacao, atual, lerAvisos(importacao));
    }
    if (paginaPlano.sessaoId() == null) {
      throw new BusinessException("Gere uma proposta para esta página antes de aceitá-la.");
    }

    var proposta = propostaService.propostaAtual(paginaPlano.sessaoId(), principal);
    UUID paginaId = proposta.paginaId();
    if (paginaId == null) {
      var aplicacao = propostaService.aplicar(
          paginaPlano.sessaoId(),
          new AplicarAiPropostaRequest(
              AplicarAiPropostaRequest.ModoAplicacao.PERSISTIR,
              moduloPlano.moduloId(),
              null,
              paginaPlano.ordem()),
          principal);
      paginaId = aplicacao.paginaId();
    }
    if (paginaId == null) {
      throw new IllegalStateException("A proposta foi aceita sem criar a página do manual.");
    }

    var paginaDocFlow = docFlowAiBridge.buscarPaginaDocumento(paginaId);
    if (!atual.projetoId().equals(paginaDocFlow.projetoId())
        || !moduloPlano.moduloId().equals(paginaDocFlow.moduloId())) {
      throw new BusinessException("A página criada não pertence ao projeto e módulo planejados.");
    }
    AiDocumentoPlano atualizado = atualizarPagina(
        atual,
        paginaPlanoId,
        pagina -> pagina.comVinculo(
            paginaDocFlow.id(), statusPaginaDocFlow(paginaDocFlow.status())));
    persistirPlano(importacao, atualizado);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse sincronizar(UUID id, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = lerPlano(importacao);
    AiDocumentoPlano atualizado = atual;
    for (AiDocumentoPlano.Pagina pagina : atual.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .filter(pagina -> pagina.sessaoId() != null && pagina.paginaId() == null)
        .toList()) {
      var sessao = sessaoRepository.findById(pagina.sessaoId()).orElse(null);
      if (sessao == null) {
        atualizado = atualizarPagina(
            atualizado,
            pagina.id(),
            item -> item.comSessao(null, AiPaginaPlanoStatus.ERRO, "Sessão de geração não encontrada."));
        continue;
      }
      AiPaginaPlanoStatus status = switch (sessao.getStatus()) {
        case PRONTA, APLICADA -> AiPaginaPlanoStatus.GERADA;
        case ERRO, CANCELADA -> AiPaginaPlanoStatus.ERRO;
        default -> AiPaginaPlanoStatus.EM_GERACAO;
      };
      String erro = status == AiPaginaPlanoStatus.ERRO
          ? "A geração não foi concluída. Abra a sessão para revisar ou tentar novamente."
          : null;
      atualizado = atualizarPagina(
          atualizado,
          pagina.id(),
          item -> item.comSessao(item.sessaoId(), status, erro));
    }
    for (AiDocumentoPlano.Pagina pagina : atual.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .filter(pagina -> pagina.paginaId() != null)
        .toList()) {
      try {
        var paginaDocFlow = docFlowAiBridge.buscarPaginaDocumento(pagina.paginaId());
        AiPaginaPlanoStatus status = statusPaginaDocFlow(paginaDocFlow.status());
        atualizado = atualizarPagina(
            atualizado,
            pagina.id(),
            item -> item.comVinculo(paginaDocFlow.id(), status));
      } catch (NotFoundException ex) {
        atualizado = atualizarPagina(
            atualizado,
            pagina.id(),
            item -> item.comVinculo(null, AiPaginaPlanoStatus.PENDENTE));
      }
    }
    persistirPlano(importacao, atualizado);
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  @Transactional(readOnly = true)
  public AiEstimativaLoteDocumentoResponse estimarLote(
      UUID id, AiGerarLoteDocumentoRequest request, Principal principal) {
    AiDocumentoPlano plano = lerPlano(carregar(id, principal));
    List<AiDocumentoPlano.Pagina> paginas = paginasSelecionadas(plano, request.paginas());
    int caracteres = paginas.stream().mapToInt(pagina -> pagina.briefing().length()).sum();
    int tokensEntrada = (int) Math.ceil(caracteres / 3.6d) + paginas.size() * 650;
    int tokensSaida = paginas.size() * Math.min(3_000, aiProperties.maxTokensSaida());
    return new AiEstimativaLoteDocumentoResponse(
        paginas.size(),
        caracteres,
        tokensEntrada,
        tokensSaida,
        aiProperties.model(),
        "Estimativa técnica; o consumo real depende do conteúdo e dos componentes escolhidos.");
  }

  @Transactional
  public AiImportacaoDocumentoResponse gerarLote(
      UUID id, AiGerarLoteDocumentoRequest request, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = lerPlano(importacao);
    if (!atual.estruturaConfirmada()) {
      throw new BusinessException("Confirme o projeto e os módulos antes de gerar em lote.");
    }
    List<AiDocumentoPlano.Pagina> selecionadas = paginasSelecionadas(atual, request.paginas());
    AiDocumentoPlano atualizado = atual;
    for (AiDocumentoPlano.Pagina pagina : selecionadas) {
      AiDocumentoPlano.Modulo modulo = atual.modulos().stream()
          .filter(item -> item.paginas().stream().anyMatch(p -> p.id().equals(pagina.id())))
          .findFirst()
          .orElseThrow();
      if (pagina.paginaId() != null || pagina.status() == AiPaginaPlanoStatus.REVISADA) {
        throw new BusinessException("Uma das páginas selecionadas já foi salva no manual.");
      }
      if (pagina.status() == AiPaginaPlanoStatus.EM_GERACAO) {
        throw new BusinessException("Uma das páginas selecionadas já está em geração.");
      }
      // codigo_tela é único no DocFlow inteiro: uma segunda importação colidiria com DOC-M01-P01.
      String codigoTela = docFlowAiBridge.codigoTelaLivre(
          "DOC-M%02d-P%02d".formatted(modulo.ordem(), pagina.ordem()));
      String briefing = "titulo: " + pagina.titulo() + "\n"
          + "codigoTela: " + codigoTela + "\n"
          + "publico: Ambos\n\n"
          + pagina.briefing();
      var sessao = sessaoService.criar(
          new CriarAiSessaoRequest(
              AiObjetivo.CRIAR_PAGINA,
              briefing,
              atual.projetoId(),
              modulo.moduloId(),
              atual.clienteId(),
              pagina.templateId(),
              null,
              assegurarComposicao(pagina, atual.projetoId(), atual.clienteId())
                  .componentesSelecionados()),
          principal);
      if (sessao.status() != AiSessaoStatus.PRONTA_PARA_GERAR) {
        throw new BusinessException(
            "A página '" + pagina.titulo() + "' ainda precisa de informações antes da geração em lote.");
      }
      propostaService.gerar(sessao.id(), principal);
      atualizado = atualizarPagina(
          atualizado,
          pagina.id(),
          item -> item.comSessao(sessao.id(), AiPaginaPlanoStatus.EM_GERACAO, null));
    }
    importacao.atualizarAnalise(escrever(atualizado), AiImportacaoStatus.EM_REVISAO);
    repository.flush();
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  private List<AiDocumentoPlano.Pagina> paginasSelecionadas(
      AiDocumentoPlano plano, List<UUID> ids) {
    Set<UUID> solicitadas = new HashSet<>(ids);
    if (solicitadas.size() != ids.size()) {
      throw new BusinessException("A seleção do lote contém páginas repetidas.");
    }
    List<AiDocumentoPlano.Pagina> paginas = plano.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .filter(pagina -> solicitadas.contains(pagina.id()))
        .toList();
    if (paginas.size() != solicitadas.size()) {
      throw new NotFoundException("Uma das páginas selecionadas não pertence a esta importação.");
    }
    return paginas;
  }

  private void persistirPlano(AiDocumentoImportacao importacao, AiDocumentoPlano plano) {
    boolean concluida = plano.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .allMatch(pagina -> pagina.status() == AiPaginaPlanoStatus.REVISADA);
    importacao.atualizarAnalise(
        escrever(plano), concluida ? AiImportacaoStatus.CONCLUIDA : AiImportacaoStatus.EM_REVISAO);
    repository.flush();
  }

  private AiDocumentoPlano exigirPlanoEditavel(AiDocumentoImportacao importacao) {
    if (importacao.getStatus() == AiImportacaoStatus.ANALISANDO_ESTRUTURA) {
      throw new BusinessException("Aguarde a conclusão da análise semântica do documento.");
    }
    AiDocumentoPlano plano = lerPlano(importacao);
    if (plano.estruturaConfirmada()) {
      throw new BusinessException(
          "As sugestões estruturais devem ser revisadas antes de confirmar projeto e módulos.");
    }
    return plano;
  }

  private AiDocumentoPlano.Sugestao encontrarSugestao(
      AiDocumentoPlano plano, UUID sugestaoId) {
    return plano.sugestoes().stream()
        .filter(item -> item.id().equals(sugestaoId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Sugestão não encontrada nesta importação."));
  }

  private void persistirPlanoEditavel(
      AiDocumentoImportacao importacao, AiDocumentoPlano plano) {
    importacao.atualizarAnalise(escrever(plano), importacao.getStatus());
    repository.flush();
  }

  private AiDocumentoPlano copiarPlano(
      AiDocumentoPlano atual,
      List<AiDocumentoPlano.Modulo> modulos,
      List<AiDocumentoPlano.Sugestao> sugestoes) {
    return new AiDocumentoPlano(
        atual.projetoNome(),
        atual.projetoDescricao(),
        atual.projetoId(),
        atual.clienteId(),
        atual.estruturaConfirmada(),
        modulos,
        atual.projetoNomesSugeridos(),
        atual.analiseOrigem(),
        atual.analiseMensagem(),
        atual.tokensEntradaAnalise(),
        atual.tokensSaidaAnalise(),
        sugestoes);
  }

  private List<AiDocumentoPlano.Sugestao> atualizarStatusSugestoes(
      List<AiDocumentoPlano.Sugestao> atuais,
      AiDocumentoPlano.Sugestao selecionada,
      AiDocumentoSugestaoStatus status) {
    return atuais.stream()
        .map(item -> {
          if (item.id().equals(selecionada.id())) return item.comStatus(status);
          if (status == AiDocumentoSugestaoStatus.APLICADA
              && selecionada.tipo() == AiDocumentoSugestaoTipo.MESCLAR_PAGINAS
              && item.status() == AiDocumentoSugestaoStatus.PENDENTE
              && (selecionada.paginaOrigemId().equals(item.paginaOrigemId())
                  || selecionada.paginaOrigemId().equals(item.paginaDestinoId()))) {
            return item.comStatus(AiDocumentoSugestaoStatus.IGNORADA);
          }
          return item;
        })
        .toList();
  }

  private List<AiDocumentoPlano.Sugestao> resolverSugestoesDaEstrutura(
      List<AiDocumentoPlano.Sugestao> sugestoes,
      List<AiDocumentoPlano.Modulo> modulos) {
    Map<UUID, UUID> moduloPorPagina = new LinkedHashMap<>();
    Map<UUID, AiDocumentoPlano.Pagina> paginasAtivas = new LinkedHashMap<>();
    Set<UUID> modulosAtivos = new HashSet<>();
    for (AiDocumentoPlano.Modulo modulo : modulos) {
      modulosAtivos.add(modulo.id());
      modulo.paginas().forEach(pagina -> {
        moduloPorPagina.put(pagina.id(), modulo.id());
        paginasAtivas.put(pagina.id(), pagina);
      });
    }
    return sugestoes.stream()
        .map(sugestao -> {
          boolean referenciaModuloRemovido =
              sugestao.moduloOrigemId() != null
                  && !modulosAtivos.contains(sugestao.moduloOrigemId())
              || sugestao.moduloDestinoId() != null
                  && !modulosAtivos.contains(sugestao.moduloDestinoId());
          boolean referenciaPaginaRemovida =
              sugestao.paginaOrigemId() != null
                  && !paginasAtivas.containsKey(sugestao.paginaOrigemId())
              || sugestao.paginaDestinoId() != null
                  && !paginasAtivas.containsKey(sugestao.paginaDestinoId());
          if (referenciaModuloRemovido || referenciaPaginaRemovida) {
            return sugestao.comStatus(AiDocumentoSugestaoStatus.IGNORADA);
          }
          if (sugestao.tipo() == AiDocumentoSugestaoTipo.RENOMEAR_PAGINA
              && sugestao.status() != AiDocumentoSugestaoStatus.IGNORADA) {
            AiDocumentoPlano.Pagina pagina = paginasAtivas.get(sugestao.paginaOrigemId());
            boolean atendida = pagina != null
                && sugestao.valorSugerido() != null
                && pagina.titulo().equalsIgnoreCase(sugestao.valorSugerido().trim());
            return sugestao.comStatus(atendida
                ? AiDocumentoSugestaoStatus.APLICADA
                : AiDocumentoSugestaoStatus.PENDENTE);
          }
          if (sugestao.tipo() != AiDocumentoSugestaoTipo.MOVER_PAGINA
              || sugestao.status() == AiDocumentoSugestaoStatus.IGNORADA) {
            return sugestao;
          }
          boolean atendida = sugestao.moduloDestinoId() != null
              && sugestao.moduloDestinoId().equals(moduloPorPagina.get(sugestao.paginaOrigemId()));
          return sugestao.comStatus(atendida
              ? AiDocumentoSugestaoStatus.APLICADA
              : AiDocumentoSugestaoStatus.PENDENTE);
        })
        .toList();
  }

  private AiDocumentoPlano.Pagina novaPaginaRascunho(
      UUID paginaId,
      String titulo,
      int ordem,
      String projetoNome,
      String moduloNome,
      String conteudo,
      AiPaginaPlanoOrigem origem) {
    AiPaginaPlanoOrigem origemSegura = origem == AiPaginaPlanoOrigem.DOCUMENTO
        || origem == AiPaginaPlanoOrigem.IA
        ? AiPaginaPlanoOrigem.MANUAL
        : origem;
    return new AiDocumentoPlano.Pagina(
        paginaId,
        titulo,
        ordem,
        montarBriefing(projetoNome, moduloNome, titulo, conteudo),
        null,
        null,
        null,
        0,
        "Conteúdo criado durante a revisão; o modelo será escolhido na geração.",
        AiPaginaPlanoStatus.PENDENTE,
        null,
        null,
        null,
        origemSegura,
        true);
  }

  private AiDocumentoPlano.Pagina paginaAjustada(
      AiDocumentoPlano.Pagina pagina,
      String titulo,
      int ordem,
      String projetoNome,
      String moduloNome,
      String conteudo,
      AiPaginaPlanoOrigem origemInformada) {
    AiPaginaPlanoOrigem origem = origemInformada == AiPaginaPlanoOrigem.MESCLAGEM
        ? AiPaginaPlanoOrigem.MESCLAGEM
        : pagina.origem();
    return new AiDocumentoPlano.Pagina(
        pagina.id(),
        titulo,
        ordem,
        montarBriefing(projetoNome, moduloNome, titulo, conteudo),
        null,
        null,
        null,
        0,
        "Conteúdo ajustado manualmente; o modelo será reavaliado na geração.",
        AiPaginaPlanoStatus.PENDENTE,
        null,
        null,
        null,
        origem,
        true);
  }

  private boolean paginaEditavel(AiDocumentoPlano.Pagina pagina) {
    return pagina.status() == AiPaginaPlanoStatus.PENDENTE
        && pagina.paginaId() == null
        && pagina.sessaoId() == null;
  }

















  private AiPaginaPlanoStatus statusPaginaDocFlow(String status) {
    return switch (status) {
      case "APROVADO", "PUBLICADO" -> AiPaginaPlanoStatus.REVISADA;
      default -> AiPaginaPlanoStatus.GERADA;
    };
  }

  private static String usuario(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }

  private AiDocumentoImportacao carregar(UUID id, Principal principal) {
    return repository.findByIdAndCreatedBy(id, usuario(principal))
        .orElseThrow(() -> new NotFoundException("Importação de documento não encontrada."));
  }

  private AiImportacaoDocumentoResponse response(
      AiDocumentoImportacao importacao,
      AiDocumentoPlano plano,
      List<String> avisos) {
    List<AiImportacaoDocumentoResponse.Sugestao> sugestoes = plano.sugestoes().stream()
        .map(sugestao -> new AiImportacaoDocumentoResponse.Sugestao(
            sugestao.id(),
            sugestao.tipo(),
            sugestao.titulo(),
            sugestao.justificativa(),
            sugestao.confianca(),
            sugestao.status(),
            sugestao.aplicacaoSegura(),
            sugestao.paginaOrigemId(),
            sugestao.paginaDestinoId(),
            sugestao.moduloOrigemId(),
            sugestao.moduloDestinoId(),
            sugestao.valorSugerido(),
            sugestao.conteudoSugerido()))
        .toList();
    List<AiImportacaoDocumentoResponse.Modulo> modulos = plano.modulos().stream()
        .map(modulo -> new AiImportacaoDocumentoResponse.Modulo(
            modulo.id(),
            modulo.moduloId(),
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .map(pagina -> {
                  AiDocumentoPlano.Pagina efetiva =
                      composicaoEfetiva(pagina, plano.projetoId(), plano.clienteId());
                  return new AiImportacaoDocumentoResponse.Pagina(
                      pagina.id(),
                      pagina.titulo(),
                      pagina.ordem(),
                      pagina.briefing(),
                      pagina.templateId(),
                      pagina.templateCodigo(),
                      pagina.templateNome(),
                      pagina.confiancaTemplate(),
                      pagina.motivoTemplate(),
                      pagina.status(),
                      pagina.paginaId(),
                      pagina.sessaoId(),
                      pagina.erroMensagem(),
                      pagina.origem(),
                      pagina.ajustadaManualmente(),
                      efetiva.blueprintId(),
                      efetiva.blueprintNome(),
                      efetiva.componentesSelecionados(),
                      efetiva.componentesObrigatorios(),
                      pagina.composicaoAjustadaManualmente());
                })
                .toList()))
        .toList();
    return AiImportacaoDocumentoResponse.from(
        importacao,
        plano.projetoNome(),
        plano.projetoDescricao(),
        plano.projetoId(),
        plano.clienteId(),
        plano.estruturaConfirmada(),
        plano.projetoNomesSugeridos() == null ? List.of(plano.projetoNome()) : plano.projetoNomesSugeridos(),
        plano.analiseOrigem(),
        plano.analiseMensagem(),
        plano.tokensEntradaAnalise(),
        plano.tokensSaidaAnalise(),
        sugestoes,
        modulos,
        avisos);
  }


  private AiDocumentoPlano assegurarComposicoes(AiDocumentoPlano plano) {
    List<AiDocumentoPlano.Modulo> modulos = plano.modulos().stream()
        .map(modulo -> new AiDocumentoPlano.Modulo(
            modulo.id(),
            modulo.moduloId(),
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .map(pagina -> assegurarComposicao(pagina, plano.projetoId(), plano.clienteId()))
                .toList()))
        .toList();
    return copiarPlano(plano, modulos, plano.sugestoes());
  }

  private AiDocumentoPlano.Pagina composicaoEfetiva(
      AiDocumentoPlano.Pagina pagina, UUID projetoId, UUID clienteId) {
    return pagina.componentesSelecionados().isEmpty()
        ? assegurarComposicao(pagina, projetoId, clienteId)
        : pagina;
  }

  private AiDocumentoPlano.Pagina assegurarComposicao(
      AiDocumentoPlano.Pagina pagina, UUID projetoId, UUID clienteId) {
    if (!pagina.componentesSelecionados().isEmpty()) return pagina;
    try {
      AiTemplateRecomendacaoResponse recomendacao = templateRecomendacaoService.recomendar(
          new AiTemplateRecomendacaoRequest(
              pagina.briefing(), projetoId, clienteId, pagina.templateId()));
      List<String> selecionados = recomendacao.componentes().stream().map(item -> item.id()).toList();
      if (selecionados.isEmpty()) return pagina;
      List<String> obrigatorios = recomendacao.componentes().stream()
          .filter(item -> item.obrigatorio())
          .map(item -> item.id())
          .toList();
      return pagina.comComposicao(
          recomendacao.blueprintId(),
          recomendacao.blueprintNome(),
          selecionados,
          obrigatorios,
          false);
    } catch (RuntimeException ex) {
      return pagina;
    }
  }

  private String normalizar(String valor) {
    return valor == null ? null : valor.trim();
  }

  private String normalizarConteudo(String valor) {
    return valor == null ? "" : valor.replace("\r\n", "\n").replace('\r', '\n').trim();
  }

  private String normalizarDescricao(String descricao, String padrao) {
    String valor = descricao == null || descricao.isBlank() ? padrao : descricao;
    return valor == null ? null : valor.trim();
  }

  private AiDocumentoPlano lerPlano(AiDocumentoImportacao importacao) {
    try {
      return objectMapper.readValue(importacao.getPlanoJson(), AiDocumentoPlano.class);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Plano da importação está inválido.", ex);
    }
  }

  private List<String> lerAvisos(AiDocumentoImportacao importacao) {
    try {
      return objectMapper.readValue(importacao.getAvisosJson(), new TypeReference<>() {});
    } catch (JsonProcessingException ex) {
      return List.of();
    }
  }

  private String escrever(Object valor) {
    try {
      return objectMapper.writeValueAsString(valor);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível persistir o plano da importação.", ex);
    }
  }

  private String sha256(MultipartFile arquivo) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(arquivo.getBytes()));
    } catch (NoSuchAlgorithmException | java.io.IOException ex) {
      throw new IllegalStateException("Não foi possível calcular a assinatura do documento.", ex);
    }
  }

  private String mimeType(MultipartFile arquivo) {
    String mimeType = arquivo.getContentType();
    return mimeType == null || mimeType.isBlank() ? "application/octet-stream" : mimeType;
  }
}
