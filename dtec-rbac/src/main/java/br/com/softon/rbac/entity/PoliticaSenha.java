package br.com.softon.rbac.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Singleton — só existe uma linha (id fixo semeado em V13). */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_politica_senha")
public class PoliticaSenha extends AuditableEntity {

  public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");

  @Id
  private UUID id;

  @Column(name = "tamanho_minimo", nullable = false)
  private int tamanhoMinimo = 8;

  @Column(name = "exigir_maiuscula", nullable = false)
  private boolean exigirMaiuscula = true;

  @Column(name = "exigir_minuscula", nullable = false)
  private boolean exigirMinuscula = true;

  @Column(name = "exigir_numero", nullable = false)
  private boolean exigirNumero = true;

  @Column(name = "exigir_especial", nullable = false)
  private boolean exigirEspecial = false;

  @Column(name = "expira_senha_dias")
  private Integer expiraSenhaDias;

  @Column(name = "quantidade_historico", nullable = false)
  private int quantidadeHistorico = 3;

  @Column(name = "max_tentativas_invalidas", nullable = false)
  private int maxTentativasInvalidas = 5;

  @Column(nullable = false)
  private boolean ativo = true;

  public void atualizar(int tamanhoMinimo, boolean exigirMaiuscula, boolean exigirMinuscula,
      boolean exigirNumero, boolean exigirEspecial, Integer expiraSenhaDias,
      int quantidadeHistorico, int maxTentativasInvalidas) {
    this.tamanhoMinimo = tamanhoMinimo;
    this.exigirMaiuscula = exigirMaiuscula;
    this.exigirMinuscula = exigirMinuscula;
    this.exigirNumero = exigirNumero;
    this.exigirEspecial = exigirEspecial;
    this.expiraSenhaDias = expiraSenhaDias;
    this.quantidadeHistorico = quantidadeHistorico;
    this.maxTentativasInvalidas = maxTentativasInvalidas;
  }
}
