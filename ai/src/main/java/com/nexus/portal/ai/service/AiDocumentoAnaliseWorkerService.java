package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem;
import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiImportacaoStatus;
import com.nexus.portal.ai.repository.AiDocumentoImportacaoRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiDocumentoAnaliseWorkerService {

  private static final Logger log = LoggerFactory.getLogger(AiDocumentoAnaliseWorkerService.class);

  private final AiDocumentoImportacaoRepository repository;
  private final AiDocumentoAnaliseSemanticaService analiseService;
  private final ObjectMapper objectMapper;

  public AiDocumentoAnaliseWorkerService(
      AiDocumentoImportacaoRepository repository,
      AiDocumentoAnaliseSemanticaService analiseService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.analiseService = analiseService;
    this.objectMapper = objectMapper;
  }

  @Async("applicationTaskExecutor")
  public CompletableFuture<Void> analisar(java.util.UUID importacaoId) {
    AiDocumentoImportacao importacao = repository.findById(importacaoId).orElse(null);
    if (importacao == null || importacao.getStatus() != AiImportacaoStatus.ANALISANDO_ESTRUTURA) {
      return CompletableFuture.completedFuture(null);
    }
    AiDocumentoPlano base = lerPlano(importacao);
    try {
      AiDocumentoPlano atualizado;
      if (analiseService.disponivel()) {
        atualizado = analiseService.analisar(base, importacao.getNomeArquivo());
      } else {
        atualizado = fallback(base, "IA não configurada; foi aplicada a análise estrutural local.");
      }
      importacao.atualizarAnalise(escrever(atualizado), AiImportacaoStatus.PRONTO_PARA_REVISAO);
      repository.save(importacao);
    } catch (RuntimeException ex) {
      log.warn("Falha na análise semântica da importação {}: {}", importacaoId, ex.getMessage());
      AiDocumentoPlano fallback = fallback(
          base, "A análise semântica não respondeu; o plano estrutural seguro foi preservado.");
      importacao.atualizarAnalise(escrever(fallback), AiImportacaoStatus.PRONTO_PARA_REVISAO);
      repository.save(importacao);
    }
    return CompletableFuture.completedFuture(null);
  }

  private AiDocumentoPlano fallback(AiDocumentoPlano base, String mensagem) {
    List<String> nomes = base.projetoNomesSugeridos() == null || base.projetoNomesSugeridos().isEmpty()
        ? List.of(base.projetoNome())
        : base.projetoNomesSugeridos();
    return new AiDocumentoPlano(
        base.projetoNome(),
        base.projetoDescricao(),
        base.projetoId(),
        base.clienteId(),
        base.estruturaConfirmada(),
        base.modulos(),
        nomes,
        AiDocumentoAnaliseOrigem.ESTRUTURAL,
        mensagem,
        null,
        null,
        base.sugestoes());
  }

  private AiDocumentoPlano lerPlano(AiDocumentoImportacao importacao) {
    try {
      return objectMapper.readValue(importacao.getPlanoJson(), AiDocumentoPlano.class);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Plano estrutural da importação inválido.", ex);
    }
  }

  private String escrever(AiDocumentoPlano plano) {
    try {
      return objectMapper.writeValueAsString(plano);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível persistir a análise do documento.", ex);
    }
  }
}
