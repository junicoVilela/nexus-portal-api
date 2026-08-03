package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_release_template")
public class ReleaseTemplate extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 200)
  private String nome;

  @Column(length = 500)
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_release", length = 30)
  private TipoRelease tipoRelease;

  @Column(name = "produto_id")
  private UUID produtoId;

  @Column(columnDefinition = "TEXT")
  private String estrutura;

  @Column(nullable = false)
  private boolean ativo = true;

  public ReleaseTemplate(String nome, String descricao, TipoRelease tipoRelease,
      UUID produtoId, String estrutura, boolean ativo) {
    this.nome = nome;
    this.descricao = descricao;
    this.tipoRelease = tipoRelease;
    this.produtoId = produtoId;
    this.estrutura = estrutura;
    this.ativo = ativo;
  }

  public void atualizar(String nome, String descricao, TipoRelease tipoRelease,
      UUID produtoId, String estrutura, boolean ativo) {
    this.nome = nome;
    this.descricao = descricao;
    this.tipoRelease = tipoRelease;
    this.produtoId = produtoId;
    this.estrutura = estrutura;
    this.ativo = ativo;
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }
}
