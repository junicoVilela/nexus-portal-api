package br.com.softon.rbac.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Catálogo RBAC read-only, populado por seed Flyway (V10). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_dominio")
public class Dominio extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(length = 500)
  private String descricao;

  @Column(nullable = false)
  private boolean ativo = true;
}
