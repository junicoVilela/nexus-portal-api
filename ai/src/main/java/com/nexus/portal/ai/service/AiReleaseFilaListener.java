package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiPrEvento;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge.PaginaAjuste;
import com.nexus.portal.ai.repository.AiPrEventoRepository;
import com.nexus.portal.shared.events.ReleasePublicadaEvento;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Release publicada → fila de propostas (INT-301…303). Cada tela citada na release que tem página
 * no DocFlow fica marcada como desatualizada e entra na fila PARA_REVISAR. A IA só gera o ajuste
 * quando alguém pede ("Gerar ajuste"): uma release pode citar dezenas de telas.
 */
@Component
public class AiReleaseFilaListener {

  private static final Logger log = LoggerFactory.getLogger(AiReleaseFilaListener.class);

  private final DocFlowAiBridge docFlowAiBridge;
  private final AiPrEventoRepository eventoRepository;

  public AiReleaseFilaListener(DocFlowAiBridge docFlowAiBridge, AiPrEventoRepository eventoRepository) {
    this.docFlowAiBridge = docFlowAiBridge;
    this.eventoRepository = eventoRepository;
  }

  /** Depois do commit da publicação: falha aqui não desfaz a release. */
  @Async
  @TransactionalEventListener(fallbackExecution = true)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void aoPublicar(ReleasePublicadaEvento release) {
    Map<String, PaginaAjuste> telas = new LinkedHashMap<>();
    for (String codigo : AiPrClassificador.codigosTela(release.titulo(), release.texto())) {
      Optional<PaginaAjuste> pagina = docFlowAiBridge.buscarPaginaPorCodigoTela(codigo);
      pagina.ifPresent(p -> telas.putIfAbsent(p.codigoTela(), p));
    }
    telas.values().forEach(pagina -> {
      docFlowAiBridge.marcarPaginaDesatualizada(pagina.id(), release.rotulo());
      if (!eventoRepository.existsByReleaseIdAndCodigoTela(release.releaseId(), pagina.codigoTela())) {
        eventoRepository.save(AiPrEvento.daRelease(release.releaseId(), release.rotulo(), release.titulo(),
            release.texto(), release.caminho(), pagina.codigoTela(), pagina.id()));
      }
    });
    log.info("ai.release.fila release={} telas={}", release.rotulo(), telas.keySet());
  }
}
