package br.com.softon.rbac.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_escopo_acesso")
public class EscopoAcesso extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "usuario_id")
  private UUID usuarioId;

  @Column(name = "grupo_id")
  private UUID grupoId;

  @Column(name = "cliente_id")
  private UUID clienteId;

  @Column(name = "ambiente_id")
  private UUID ambienteId;

  @Column(name = "produto_id")
  private UUID produtoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_ambiente", length = 20)
  private TipoAmbiente tipoAmbiente;

  @Column(name = "somente_leitura", nullable = false)
  private boolean somenteLeitura = false;

  @Column(nullable = false)
  private boolean ativo = true;

  public EscopoAcesso(UUID usuarioId, UUID grupoId, UUID clienteId, UUID ambienteId,
      UUID produtoId, TipoAmbiente tipoAmbiente, boolean somenteLeitura, boolean ativo) {
    this.usuarioId = usuarioId;
    this.grupoId = grupoId;
    this.clienteId = clienteId;
    this.ambienteId = ambienteId;
    this.produtoId = produtoId;
    this.tipoAmbiente = tipoAmbiente;
    this.somenteLeitura = somenteLeitura;
    this.ativo = ativo;
  }

  public void atualizar(UUID clienteId, UUID ambienteId, UUID produtoId,
      TipoAmbiente tipoAmbiente, boolean somenteLeitura, boolean ativo) {
    this.clienteId = clienteId;
    this.ambienteId = ambienteId;
    this.produtoId = produtoId;
    this.tipoAmbiente = tipoAmbiente;
    this.somenteLeitura = somenteLeitura;
    this.ativo = ativo;
  }

  public enum TipoAmbiente { DEV, HML, PRD, SIMULADO }
}
