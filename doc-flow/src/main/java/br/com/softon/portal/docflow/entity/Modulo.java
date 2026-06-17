package br.com.softon.portal.docflow.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_modulo")
public class Modulo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, length = 150)
  private String slug;

  private String descricao;

  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false)
  private boolean ativo = true;

  @ManyToOne(optional = false)
  @JoinColumn(name = "projeto_id", nullable = false)
  private Projeto projeto;

  public Modulo(String nome, String slug, String descricao, int ordem, boolean ativo, Projeto projeto) {
    this.nome = nome;
    this.slug = slug;
    this.descricao = descricao;
    this.ordem = ordem;
    this.ativo = ativo;
    this.projeto = projeto;
  }

  public void atualizar(String nome, String slug, String descricao, int ordem, boolean ativo, Projeto projeto) {
    this.nome = nome;
    this.slug = slug;
    this.descricao = descricao;
    this.ordem = ordem;
    this.ativo = ativo;
    this.projeto = projeto;
  }
}
