package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Máquina de execução das instalações do produto por cliente. Distinta do
 * {@code host} de {@link ConfigEntrega} (destino FTP/SFTP/bucket).
 *
 * <p>Spec: automação de implantação por cliente, RF-002.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorHost")
@Table(name = "tb_host_orchestrator")
public class Host extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 40)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String nome;

  @Column(nullable = false, length = 255)
  private String hostname;

  @Column(name = "endereco_ip", length = 45)
  private String enderecoIp;

  @Enumerated(EnumType.STRING)
  @Column(name = "sistema_operacional", nullable = false, length = 20)
  private SistemaOperacionalHost sistemaOperacional;

  @Column(name = "docker_disponivel", nullable = false)
  private boolean dockerDisponivel = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_conexao", nullable = false, length = 20)
  private TipoConexaoHost tipoConexao;

  @Column(name = "porta_conexao")
  private Integer portaConexao;

  @Column(name = "usuario_conexao", length = 120)
  private String usuarioConexao;

  /** Referência a segredo. Nunca armazena senha em texto puro. */
  @Column(name = "credencial_ref", length = 200)
  private String credencialRef;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(columnDefinition = "TEXT")
  private String observacoes;

  public Host(String codigo, String nome, String hostname, SistemaOperacionalHost sistemaOperacional,
      TipoConexaoHost tipoConexao) {
    this.codigo = codigo.trim().toUpperCase();
    this.nome = nome.trim();
    this.hostname = hostname.trim();
    this.sistemaOperacional = sistemaOperacional;
    this.tipoConexao = tipoConexao;
    this.ativo = true;
  }

  public void atualizar(String codigo, String nome, String hostname, String enderecoIp,
      SistemaOperacionalHost sistemaOperacional, boolean dockerDisponivel, TipoConexaoHost tipoConexao,
      Integer portaConexao, String usuarioConexao, String credencialRef, String observacoes) {
    this.codigo = codigo.trim().toUpperCase();
    this.nome = nome.trim();
    this.hostname = hostname.trim();
    this.enderecoIp = blankToNull(enderecoIp);
    this.sistemaOperacional = sistemaOperacional;
    this.dockerDisponivel = dockerDisponivel;
    this.tipoConexao = tipoConexao;
    this.portaConexao = portaConexao;
    this.usuarioConexao = blankToNull(usuarioConexao);
    this.credencialRef = blankToNull(credencialRef);
    this.observacoes = blankToNull(observacoes);
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
