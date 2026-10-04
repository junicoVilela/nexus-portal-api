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

/** Chave de integração do manual de um cliente (Onda D). Só o hash do token é guardado. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_manual_acesso")
public class ManualAcesso {

  @Id
  private UUID id;

  @Column(name = "cliente_id", nullable = false, updatable = false)
  private UUID clienteId;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, length = 16, updatable = false)
  private String prefixo;

  @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
  private String tokenHash;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<String> origens = List.of();

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "expira_em")
  private OffsetDateTime expiraEm;

  @Column(name = "ultimo_uso_em")
  private OffsetDateTime ultimoUsoEm;

  @Column(name = "revogado_em")
  private OffsetDateTime revogadoEm;

  @Column(name = "revogado_por", length = 120)
  private String revogadoPor;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "created_by", length = 120, updatable = false)
  private String createdBy;

  public ManualAcesso(UUID clienteId, String nome, String prefixo, String tokenHash, List<String> origens,
      OffsetDateTime expiraEm, String createdBy) {
    this.id = UUID.randomUUID();
    this.clienteId = clienteId;
    this.nome = nome;
    this.prefixo = prefixo;
    this.tokenHash = tokenHash;
    this.origens = List.copyOf(origens);
    this.expiraEm = expiraEm;
    this.createdBy = createdBy;
  }

  public boolean valido(OffsetDateTime agora) {
    return ativo && (expiraEm == null || expiraEm.isAfter(agora));
  }

  /** Sem origens cadastradas, qualquer origem (uso servidor a servidor ou app interno). */
  public boolean aceitaOrigem(String origem) {
    return origem == null || origens.isEmpty() || origens.contains(origem);
  }

  public void registrarUso(OffsetDateTime agora) {
    this.ultimoUsoEm = agora;
  }

  public void revogar(String usuario) {
    this.ativo = false;
    this.revogadoEm = OffsetDateTime.now();
    this.revogadoPor = usuario;
  }
}
