package com.nexus.identityaccess.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
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

/** Catálogo RBAC read-only, populado por seed Flyway (V10). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_funcionalidade")
public class Funcionalidade extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dominio_id", nullable = false)
  private Dominio dominio;

  @Column(nullable = false, length = 80)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(length = 500)
  private String descricao;

  @Column(nullable = false)
  private boolean ativo = true;
}
