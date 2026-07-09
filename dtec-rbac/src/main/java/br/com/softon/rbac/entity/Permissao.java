package br.com.softon.rbac.entity;

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

/** Catálogo RBAC read-only, populado por seed Flyway (V10). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_permissao")
public class Permissao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "funcionalidade_id", nullable = false)
  private Funcionalidade funcionalidade;

  @Column(nullable = false, length = 40)
  private String acao;

  @Column(nullable = false, unique = true, length = 120)
  private String codigo;

  @Column(length = 500)
  private String descricao;

  @Column(nullable = false)
  private boolean ativo = true;
}
