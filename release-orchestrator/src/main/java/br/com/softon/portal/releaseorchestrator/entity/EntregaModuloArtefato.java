package br.com.softon.portal.releaseorchestrator.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Link N:M entre {@link EntregaModulo} e {@link ArtefatoReleaseModulo} — registra
 * quais artefatos da release entram no pacote desta entrega para cada módulo
 * selecionado (F1.10).
 *
 * No MVP é o delta inteiro do módulo (todos os artefatos uploadados na release
 * que pertencem ao mesmo módulo). Pós-MVP (F2.10) vai filtrar via diff Git por
 * tag FROM..TO.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorEntregaModuloArtefato")
@Table(name = "tb_entrega_modulo_artefato")
public class EntregaModuloArtefato extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entrega_modulo_id", nullable = false)
  private EntregaModulo entregaModulo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "artefato_release_modulo_id", nullable = false)
  private ArtefatoReleaseModulo artefato;

  @Column(nullable = false)
  private int ordem;

  public EntregaModuloArtefato(EntregaModulo entregaModulo, ArtefatoReleaseModulo artefato,
      int ordem) {
    this.entregaModulo = entregaModulo;
    this.artefato = artefato;
    this.ordem = ordem;
  }
}
