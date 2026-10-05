package com.nexus.portal.docflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Grupo de termos equivalentes na busca do manual de um cliente ("NF" = "nota fiscal"). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_manual_sinonimo")
public class ManualSinonimo {

  @Id
  private UUID id;

  @Column(name = "cliente_id", nullable = false, updatable = false)
  private UUID clienteId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<String> termos = List.of();

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "created_by", length = 120, updatable = false)
  private String createdBy;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  public ManualSinonimo(UUID clienteId, List<String> termos, String createdBy) {
    this.id = UUID.randomUUID();
    this.clienteId = clienteId;
    this.termos = List.copyOf(termos);
    this.createdBy = createdBy;
  }

  public void atualizar(List<String> termos) {
    this.termos = List.copyOf(termos);
    this.updatedAt = OffsetDateTime.now();
  }
}
