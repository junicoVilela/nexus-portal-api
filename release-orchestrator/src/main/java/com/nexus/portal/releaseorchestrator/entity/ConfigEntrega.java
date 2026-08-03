package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuração de entrega 1:1 com o {@link Cliente} operacional. MVP: só
 * destino PASTA (validado no service).
 *
 * Spec: docs/release-orchestrator/07-cliente-configuracoes-entrega.md
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorConfigEntrega")
@Table(name = "tb_config_entrega_orchestrator")
public class ConfigEntrega extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false, unique = true)
  private Cliente cliente;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_destino", nullable = false, length = 20)
  private TipoDestinoEntrega tipoDestino;

  /** Required quando tipoDestino = PASTA (validado no service). */
  @Column(name = "caminho_base", length = 500)
  private String caminhoBase;

  @Column(name = "exigir_aprovacao", nullable = false)
  private boolean exigirAprovacao;

  /** E-mails separados por vírgula (max 1000 chars). */
  @Column(name = "emails_notificacao", length = 1000)
  private String emailsNotificacao;

  /** Required quando tipoDestino = FTP/SFTP. */
  @Column(name = "host", length = 200)
  private String host;

  /** Default 21 (FTP) ou 22 (SFTP); aplicado no service quando nulo. */
  @Column(name = "porta")
  private Integer porta;

  @Column(name = "usuario", length = 120)
  private String usuario;

  /** Senha cifrada em AES-GCM (base64). Setter preserva quando recebe vazio. */
  @Column(name = "senha_cifrada", length = 1000)
  private String senhaCifrada;

  /** FTP em modo passivo. Default TRUE. */
  @Column(name = "modo_passivo")
  private Boolean modoPassivo;

  /** SFTP: valida fingerprint do servidor antes de conectar. Default TRUE. */
  @Column(name = "strict_host_check")
  private Boolean strictHostCheck;

  /** Required quando tipoDestino = BUCKET. */
  @Column(name = "bucket", length = 200)
  private String bucket;

  /** Endpoint custom (MinIO, Backblaze, etc). Vazio = AWS S3 padrão. */
  @Column(name = "endpoint", length = 500)
  private String endpoint;

  /** Região S3 (us-east-1, sa-east-1...). Default us-east-1 quando endpoint custom. */
  @Column(name = "regiao", length = 60)
  private String regiao;

  /** Path-style: TRUE para MinIO/endpoints custom; FALSE (default) para AWS S3. */
  @Column(name = "path_style_access")
  private Boolean pathStyleAccess;

  public ConfigEntrega(Cliente cliente, TipoDestinoEntrega tipoDestino, String caminhoBase) {
    this.cliente = cliente;
    this.tipoDestino = tipoDestino;
    this.caminhoBase = caminhoBase;
    this.exigirAprovacao = false;
  }

  public void atualizar(TipoDestinoEntrega tipoDestino, String caminhoBase,
      boolean exigirAprovacao, String emailsNotificacao) {
    this.tipoDestino = tipoDestino;
    this.caminhoBase = caminhoBase;
    this.exigirAprovacao = exigirAprovacao;
    this.emailsNotificacao = emailsNotificacao;
  }

  /**
   * Atualiza campos do destino remoto. {@code senhaCifrada} em branco/nulo
   * preserva o valor atual (segurança: nunca devolvemos a senha no GET, então
   * a UI manda em branco quando o operador não alterou).
   */
  public void atualizarDestinoRemoto(String host, Integer porta, String usuario,
      String senhaCifrada, Boolean modoPassivo, Boolean strictHostCheck) {
    this.host = host;
    this.porta = porta;
    this.usuario = usuario;
    if (senhaCifrada != null && !senhaCifrada.isBlank()) {
      this.senhaCifrada = senhaCifrada;
    }
    this.modoPassivo = modoPassivo;
    this.strictHostCheck = strictHostCheck;
  }

  public boolean temSenhaConfigurada() {
    return senhaCifrada != null && !senhaCifrada.isBlank();
  }

  /**
   * Atualiza campos do destino BUCKET (S3/MinIO). Reaproveita o trio
   * {@code usuario}/{@code senhaCifrada} para access-key/secret-key (mesmo
   * esquema de cifragem). Senha em branco preserva valor atual.
   */
  public void atualizarDestinoBucket(String bucket, String endpoint, String regiao,
      Boolean pathStyleAccess, String accessKey, String secretCifrada) {
    this.bucket = bucket;
    this.endpoint = endpoint;
    this.regiao = regiao;
    this.pathStyleAccess = pathStyleAccess;
    this.usuario = accessKey;
    if (secretCifrada != null && !secretCifrada.isBlank()) {
      this.senhaCifrada = secretCifrada;
    }
  }
}
