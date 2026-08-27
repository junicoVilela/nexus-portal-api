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
 * Configuração da instalação do cliente (RF-004). Distinta de
 * {@link ConfigEntrega} (destino do pacote).
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorConfiguracaoInstalacao")
@Table(name = "tb_configuracao_instalacao")
public class ConfiguracaoInstalacao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instalacao_id", nullable = false, unique = true)
  private InstalacaoCliente instalacao;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_banco", length = 20)
  private TipoBanco tipoBanco;

  @Column(name = "banco_host", length = 200)
  private String bancoHost;

  @Column(name = "banco_porta")
  private Integer bancoPorta;

  @Column(name = "banco_nome", length = 120)
  private String bancoNome;

  @Column(name = "banco_usuario", length = 120)
  private String bancoUsuario;

  /** Referência a segredo. Nunca armazena senha em texto puro. */
  @Column(name = "banco_credencial_ref", length = 200)
  private String bancoCredencialRef;

  @Column(name = "url_backend", length = 500)
  private String urlBackend;

  @Column(name = "url_frontend", length = 500)
  private String urlFrontend;

  @Column(columnDefinition = "TEXT")
  private String parametros;

  public ConfiguracaoInstalacao(InstalacaoCliente instalacao) {
    this.instalacao = instalacao;
  }

  public void atualizar(TipoBanco tipoBanco, String bancoHost, Integer bancoPorta, String bancoNome,
      String bancoUsuario, String bancoCredencialRef, String urlBackend, String urlFrontend,
      String parametros) {
    this.tipoBanco = tipoBanco;
    this.bancoHost = blankToNull(bancoHost);
    this.bancoPorta = bancoPorta;
    this.bancoNome = blankToNull(bancoNome);
    this.bancoUsuario = blankToNull(bancoUsuario);
    this.bancoCredencialRef = blankToNull(bancoCredencialRef);
    this.urlBackend = blankToNull(urlBackend);
    this.urlFrontend = blankToNull(urlFrontend);
    this.parametros = blankToNull(parametros);
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
