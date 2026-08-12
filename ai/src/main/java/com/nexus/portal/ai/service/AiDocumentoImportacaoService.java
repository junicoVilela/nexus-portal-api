package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.dto.request.AiConfirmarEstruturaDocumentoRequest;
import com.nexus.portal.ai.dto.response.AiImportacaoDocumentoResponse;
import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiDocumentoClienteModo;
import com.nexus.portal.ai.entity.AiDocumentoProjetoModo;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.repository.AiDocumentoImportacaoRepository;
import com.nexus.portal.ai.service.AiDocumentoExtratorService.DocumentoExtraido;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.portal.shared.util.SlugUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import org.springframework.stereotype.Service;
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

  public AiDocumentoImportacaoService(
      AiDocumentoImportacaoRepository repository,
      AiDocumentoExtratorService extratorService,
      AiDocumentoPlanejadorService planejadorService,
      ObjectMapper objectMapper,
      AuditoriaService auditoriaService,
      DocFlowAiBridge docFlowAiBridge) {
    this.repository = repository;
    this.extratorService = extratorService;
    this.planejadorService = planejadorService;
    this.objectMapper = objectMapper;
    this.auditoriaService = auditoriaService;
    this.docFlowAiBridge = docFlowAiBridge;
  }

  @Transactional
  public AiImportacaoDocumentoResponse importar(
      MultipartFile arquivo,
      UUID projetoId,
      UUID clienteId,
      Principal principal) {
    DocumentoExtraido extraido = extratorService.extrair(arquivo);
    AiDocumentoPlano plano = planejadorService.planejar(extraido, projetoId, clienteId);
    var importacao = new AiDocumentoImportacao(
        extraido.nomeArquivo(),
        extraido.tipo(),
        mimeType(arquivo),
        arquivo.getSize(),
        sha256(arquivo),
        extraido.texto(),
        extraido.totalPaginasOrigem(),
        escrever(plano),
        escrever(extraido.avisos()));
    repository.save(importacao);
    auditoriaService.registrar(
        AiAuditoriaAcoes.ENTIDADE_DOCUMENTO_IMPORTACAO,
        importacao.getId(),
        AiAuditoriaAcoes.DOCUMENTO_IMPORTADO,
        "Documento " + extraido.tipo() + " · " + extraido.texto().length() + " caracteres",
        principal);
    return response(importacao, plano, extraido.avisos());
  }

  @Transactional(readOnly = true)
  public AiImportacaoDocumentoResponse buscar(UUID id, Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    return response(importacao, lerPlano(importacao), lerAvisos(importacao));
  }

  @Transactional
  public AiImportacaoDocumentoResponse confirmarEstrutura(
      UUID id,
      AiConfirmarEstruturaDocumentoRequest request,
      Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = lerPlano(importacao);
    if (atual.estruturaConfirmada()) {
      return response(importacao, atual, lerAvisos(importacao));
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
        modulos);
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
        modulos);
    importacao.iniciarRevisao(escrever(atualizado));
    repository.flush();
    return response(importacao, atualizado, lerAvisos(importacao));
  }

  private AiDocumentoImportacao carregar(UUID id, Principal principal) {
    String usuario = principal == null ? "system" : principal.getName();
    return repository.findByIdAndCreatedBy(id, usuario)
        .orElseThrow(() -> new NotFoundException("Importação de documento não encontrada."));
  }

  private AiImportacaoDocumentoResponse response(
      AiDocumentoImportacao importacao,
      AiDocumentoPlano plano,
      List<String> avisos) {
    List<AiImportacaoDocumentoResponse.Modulo> modulos = plano.modulos().stream()
        .map(modulo -> new AiImportacaoDocumentoResponse.Modulo(
            modulo.id(),
            modulo.moduloId(),
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .map(pagina -> new AiImportacaoDocumentoResponse.Pagina(
                    pagina.id(),
                    pagina.titulo(),
                    pagina.ordem(),
                    pagina.briefing(),
                    pagina.templateId(),
                    pagina.templateCodigo(),
                    pagina.templateNome(),
                    pagina.confiancaTemplate(),
                    pagina.motivoTemplate(),
                    pagina.status()))
                .toList()))
        .toList();
    return AiImportacaoDocumentoResponse.from(
        importacao,
        plano.projetoNome(),
        plano.projetoDescricao(),
        plano.projetoId(),
        plano.clienteId(),
        plano.estruturaConfirmada(),
        modulos,
        avisos);
  }

  private AiDocumentoPlano.Pagina atualizarContexto(
      AiDocumentoPlano.Pagina pagina,
      String projetoNome,
      String moduloNome) {
    String briefing = pagina.briefing()
        .replaceFirst("(?m)^# Projeto:.*$", Matcher.quoteReplacement("# Projeto: " + projetoNome))
        .replaceFirst("(?m)^## Módulo:.*$", Matcher.quoteReplacement("## Módulo: " + moduloNome));
    return new AiDocumentoPlano.Pagina(
        pagina.id(),
        pagina.titulo(),
        pagina.ordem(),
        briefing,
        pagina.templateId(),
        pagina.templateCodigo(),
        pagina.templateNome(),
        pagina.confiancaTemplate(),
        pagina.motivoTemplate(),
        pagina.status());
  }

  private String normalizar(String valor) {
    return valor == null ? null : valor.trim();
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
