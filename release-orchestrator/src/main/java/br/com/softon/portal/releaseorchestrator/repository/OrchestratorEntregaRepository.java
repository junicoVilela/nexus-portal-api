package br.com.softon.portal.releaseorchestrator.repository;

import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.StatusEntrega;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrchestratorEntregaRepository
    extends JpaRepository<Entrega, UUID>, JpaSpecificationExecutor<Entrega> {

  /**
   * Entregas candidatas à retenção (F4 fase 2): status alvo, ainda têm
   * referência ao arquivo de pacote e foram concluídas antes do cutoff.
   * Usado pelo {@code RetencaoPacotesJob} para apagar ZIPs antigos.
   */
  List<Entrega> findByStatusAndArquivoPacoteCaminhoNotNullAndDataConclusaoBefore(
      StatusEntrega status, OffsetDateTime cutoff);
}
