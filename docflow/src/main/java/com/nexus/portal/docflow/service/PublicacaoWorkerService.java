package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.service.GeradorPacoteService.ResultadoGeracao;
import com.nexus.portal.docflow.dto.response.PaginaResponse;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.PublicacaoChangelog;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.PublicacaoChangelogRepository;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gera o pacote da publicação fora de transação: a escrita em disco leva
 * dezenas de segundos e não pode segurar uma conexão do pool. O banco só é
 * tocado em transações curtas — carregar o contexto no início e registrar o
 * desfecho no fim.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PublicacaoWorkerService {

  private final PublicacaoRepository publicacaoRepository;
  private final PublicacaoChangelogRepository changelogRepository;
  private final GeradorPacoteService geradorPacoteService;
  private final NotificacaoEmailService notificacaoEmailService;
  private final PublicacaoEventService publicacaoEventService;
  private final MeterRegistry meterRegistry;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  @Async
  public void processar(UUID publicacaoId, String username) {
    Timer.Sample tempoGeracao = Timer.start(meterRegistry);
    Contexto contexto = transactionTemplate.execute(status -> carregarContexto(publicacaoId));
    if (contexto == null) {
      finalizarMetricas(tempoGeracao, "cancelada");
      log.warn("Publicação {} não encontrada antes do processamento assíncrono", publicacaoId);
      return;
    }

    try {
      ResultadoGeracao resultado =
          geradorPacoteService.gerar(contexto.cliente(), contexto.versao(), publicacaoId);
      if (cancelamentoSolicitado(publicacaoId)) {
        descartarPacote(resultado);
        concluir(publicacaoId, tempoGeracao, "cancelada", Publicacao::registrarCancelamento);
        log.info("Publicação {} cancelada a pedido do usuário {}", publicacaoId, username);
        return;
      }
      concluir(publicacaoId, tempoGeracao, "sucesso", publicacao -> {
        publicacao.registrarSucesso(resultado.quantidadePaginas(), resultado.quantidadeModulos(),
            resultado.arquivoZipNome(), resultado.arquivoZipCaminho(), resultado.hashPacote(),
            resultado.relatorioValidacaoJson());
        publicacao.definirArvorePaginas(serializarArvorePaginas(contexto.paginas()));
        gerarChangelog(publicacao, contexto.paginas());
      });
      log.info("Publicação {} concluída: cliente={}, versao={}, paginas={}, modulos={}",
          publicacaoId, contexto.clienteId(), contexto.versao(),
          resultado.quantidadePaginas(), resultado.quantidadeModulos());
    } catch (RuntimeException | java.io.IOException ex) {
      concluir(publicacaoId, tempoGeracao, "erro", publicacao -> publicacao.registrarErro(ex.getMessage()));
      log.error("Falha ao gerar publicação {}: cliente={}, versao={}", publicacaoId,
          contexto.clienteId(), contexto.versao(), ex);
    }
  }

  /**
   * Carrega numa transação curta tudo o que a geração precisa, incluindo as
   * páginas elegíveis — depois disso o worker não toca mais no banco até o fim.
   */
  private Contexto carregarContexto(UUID publicacaoId) {
    Publicacao publicacao = publicacaoRepository.findById(publicacaoId).orElse(null);
    if (publicacao == null) {
      return null;
    }
    Cliente cliente = publicacao.getCliente();
    List<PaginaResponse> paginas = geradorPacoteService.selecionarPaginas(cliente.getId())
        .stream().map(PaginaResponse::from).toList();
    return new Contexto(cliente, cliente.getId(), publicacao.getVersao(), paginas);
  }

  private boolean cancelamentoSolicitado(UUID publicacaoId) {
    return Boolean.TRUE.equals(transactionTemplate.execute(status ->
        publicacaoRepository.findById(publicacaoId)
            .map(Publicacao::isCancelamentoSolicitado)
            .orElse(false)));
  }

  /** Aplica o desfecho numa transação curta e publica evento/notificação depois. */
  private void concluir(UUID publicacaoId, Timer.Sample tempoGeracao, String resultado,
      Consumer<Publicacao> desfecho) {
    Publicacao publicacao = transactionTemplate.execute(status -> {
      Publicacao atual = publicacaoRepository.findById(publicacaoId).orElse(null);
      if (atual != null) {
        desfecho.accept(atual);
      }
      return atual;
    });
    finalizarMetricas(tempoGeracao, resultado);
    if (publicacao == null) {
      return;
    }
    notificacaoEmailService.notificarPublicacaoGerada(publicacao);
    publicacaoEventService.publicar(publicacao);
  }

  private void finalizarMetricas(Timer.Sample tempoGeracao, String resultado) {
    meterRegistry.counter("docflow.publicacao.resultado", "status", resultado).increment();
    tempoGeracao.stop(meterRegistry.timer("docflow.publicacao.duracao"));
  }

  /** O ZIP já foi escrito quando o cancelamento chegou; não deixa lixo em disco. */
  private void descartarPacote(ResultadoGeracao resultado) {
    if (resultado.arquivoZipCaminho() == null || resultado.arquivoZipCaminho().isBlank()) {
      return;
    }
    try {
      Files.deleteIfExists(Path.of(resultado.arquivoZipCaminho()));
    } catch (java.io.IOException | SecurityException ex) {
      log.warn("Pacote da publicação cancelada não pôde ser removido: {}", ex.getMessage());
    }
  }

  private String serializarArvorePaginas(List<PaginaResponse> paginasAtuais) {
    try {
      return objectMapper.writeValueAsString(PublicacaoPaginaSnapshotBuilder.build(paginasAtuais));
    } catch (JsonProcessingException ex) {
      log.warn("Falha ao serializar árvore de páginas da publicação: {}", ex.getMessage());
      return null;
    }
  }

  private void gerarChangelog(Publicacao publicacao, List<PaginaResponse> paginasAtuais) {
    List<Publicacao> anteriores = publicacaoRepository
        .findByCliente_IdOrderByCreatedAtDesc(publicacao.getCliente().getId())
        .stream()
        .filter(p -> !p.getId().equals(publicacao.getId())
            && p.getStatus() == StatusPublicacao.SUCESSO)
        .limit(1)
        .toList();

    if (anteriores.isEmpty()) {
      paginasAtuais.forEach(p -> changelogRepository.save(
          new PublicacaoChangelog(publicacao.getId(), p.id(), p.titulo(), "ADICIONADO")));
      return;
    }

    Publicacao anterior = anteriores.get(0);
    List<PublicacaoChangelog> changelogAnterior = changelogRepository
        .findByPublicacaoIdOrderByCreatedAtAsc(anterior.getId());
    Set<UUID> idsAnteriores = changelogAnterior.stream()
        .map(PublicacaoChangelog::getPaginaId)
        .filter(id -> id != null)
        .collect(Collectors.toSet());
    Set<UUID> idsAtuais = paginasAtuais.stream().map(PaginaResponse::id).collect(Collectors.toSet());

    paginasAtuais.forEach(p -> {
      String tipo = idsAnteriores.contains(p.id()) ? "ATUALIZADO" : "ADICIONADO";
      changelogRepository.save(new PublicacaoChangelog(publicacao.getId(), p.id(), p.titulo(), tipo));
    });
    idsAnteriores.stream()
        .filter(id -> !idsAtuais.contains(id))
        .forEach(id -> {
          String titulo = changelogAnterior.stream()
              .filter(c -> id.equals(c.getPaginaId()))
              .map(PublicacaoChangelog::getPaginaTitulo)
              .findFirst().orElse("Página removida");
          changelogRepository.save(new PublicacaoChangelog(publicacao.getId(), id, titulo, "REMOVIDO"));
        });
  }

  private record Contexto(Cliente cliente, UUID clienteId, String versao, List<PaginaResponse> paginas) {
  }
}
