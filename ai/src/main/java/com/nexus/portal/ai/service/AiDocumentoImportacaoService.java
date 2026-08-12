package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.portal.ai.audit.AiAuditoriaAcoes;
import com.nexus.portal.ai.dto.response.AiImportacaoDocumentoResponse;
import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.repository.AiDocumentoImportacaoRepository;
import com.nexus.portal.ai.service.AiDocumentoExtratorService.DocumentoExtraido;
import com.nexus.portal.shared.exception.NotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
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

  public AiDocumentoImportacaoService(
      AiDocumentoImportacaoRepository repository,
      AiDocumentoExtratorService extratorService,
      AiDocumentoPlanejadorService planejadorService,
      ObjectMapper objectMapper,
      AuditoriaService auditoriaService) {
    this.repository = repository;
    this.extratorService = extratorService;
    this.planejadorService = planejadorService;
    this.objectMapper = objectMapper;
    this.auditoriaService = auditoriaService;
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
  public AiImportacaoDocumentoResponse selecionarPagina(
      UUID id,
      UUID paginaPlanoId,
      Principal principal) {
    AiDocumentoImportacao importacao = carregar(id, principal);
    AiDocumentoPlano atual = lerPlano(importacao);
    boolean encontrada = atual.modulos().stream()
        .flatMap(modulo -> modulo.paginas().stream())
        .anyMatch(pagina -> pagina.id().equals(paginaPlanoId));
    if (!encontrada) throw new NotFoundException("Página não encontrada no plano importado.");

    List<AiDocumentoPlano.Modulo> modulos = atual.modulos().stream()
        .map(modulo -> new AiDocumentoPlano.Modulo(
            modulo.id(),
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
    AiDocumentoPlano atualizado = new AiDocumentoPlano(atual.projetoNome(), modulos);
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
    return AiImportacaoDocumentoResponse.from(importacao, plano.projetoNome(), modulos, avisos);
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
