package br.com.softon.portal.releaseorchestrator.entity;

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
@Table(name = "tb_produto_rh")
public class ProdutoRh extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 200)
  private String nome;

  @Column(nullable = false, length = 20, unique = true)
  private String sigla;

  @Column(length = 500)
  private String descricao;

  @Column(nullable = false, length = 20)
  private String cor;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  @Column(nullable = false)
  private boolean ativo = true;

  public ProdutoRh(String nome, String sigla, String descricao, String cor,
      UUID responsavelId, boolean ativo) {
    this.nome = nome;
    this.sigla = sigla.toUpperCase().trim();
    this.descricao = descricao;
    this.cor = cor;
    this.responsavelId = responsavelId;
    this.ativo = ativo;
  }

  public void atualizar(String nome, String sigla, String descricao, String cor,
      UUID responsavelId, boolean ativo) {
    this.nome = nome;
    this.sigla = sigla.toUpperCase().trim();
    this.descricao = descricao;
    this.cor = cor;
    this.responsavelId = responsavelId;
    this.ativo = ativo;
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }
}
