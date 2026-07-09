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
@Table(name = "tb_historico_login")
public class HistoricoLogin {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "usuario_id")
  private UUID usuarioId;

  @Column(name = "login_informado", nullable = false, length = 120)
  private String loginInformado;

  @Column(name = "ip_origem", length = 45)
  private String ipOrigem;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  @Column(nullable = false)
  private boolean sucesso;

  @Column(name = "motivo_falha", length = 200)
  private String motivoFalha;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public HistoricoLogin(UUID usuarioId, String loginInformado, String ipOrigem, String userAgent,
      boolean sucesso, String motivoFalha) {
    this.usuarioId = usuarioId;
    this.loginInformado = loginInformado;
    this.ipOrigem = ipOrigem;
    this.userAgent = userAgent;
    this.sucesso = sucesso;
    this.motivoFalha = motivoFalha;
  }

  @PrePersist
  void prePersist() {
    this.createdAt = OffsetDateTime.now();
  }
}
