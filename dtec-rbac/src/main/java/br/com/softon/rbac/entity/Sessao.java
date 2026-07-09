package br.com.softon.rbac.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_sessao")
public class Sessao {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 120)
  private String jti;

  @Column(name = "usuario_id", nullable = false)
  private UUID usuarioId;

  @Column(name = "ip_origem", length = 45)
  private String ipOrigem;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  @Column(nullable = false)
  private boolean ativa = true;

  @Column(nullable = false)
  private boolean revogada = false;

  @Column(name = "motivo_encerramento", length = 200)
  private String motivoEncerramento;

  @Column(name = "iniciada_em", nullable = false)
  private OffsetDateTime iniciadaEm;

  @Column(name = "encerrada_em")
  private OffsetDateTime encerradaEm;

  @Column(name = "expira_em")
  private OffsetDateTime expiraEm;

  public Sessao(String jti, UUID usuarioId, String ipOrigem, String userAgent, OffsetDateTime expiraEm) {
    this.jti = jti;
    this.usuarioId = usuarioId;
    this.ipOrigem = ipOrigem;
    this.userAgent = userAgent;
    this.expiraEm = expiraEm;
  }

  public void revogar(String motivo) {
    this.ativa = false;
    this.revogada = true;
    this.motivoEncerramento = motivo;
    this.encerradaEm = OffsetDateTime.now();
  }

  public void encerrarPorLogout() {
    this.ativa = false;
    this.motivoEncerramento = "Logout";
    this.encerradaEm = OffsetDateTime.now();
  }

  @PrePersist
  void prePersist() {
    if (this.iniciadaEm == null) this.iniciadaEm = OffsetDateTime.now();
  }
}
