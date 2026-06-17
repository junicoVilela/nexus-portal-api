package br.com.softon.portal.docflow.entity;

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
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_projeto")
public class Projeto extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, unique = true, length = 150)
  private String slug;

  private String descricao;

  @Column(nullable = false)
  private boolean ativo = true;

  public Projeto(String nome, String slug, String descricao, boolean ativo) {
    this.nome = nome;
    this.slug = slug;
    this.descricao = descricao;
    this.ativo = ativo;
  }

  public void atualizar(String nome, String slug, String descricao, boolean ativo) {
    this.nome = nome;
    this.slug = slug;
    this.descricao = descricao;
    this.ativo = ativo;
  }
}
