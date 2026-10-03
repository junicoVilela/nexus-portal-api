package com.nexus.portal.ai.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ai_sessao")
public class AiSessao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AiObjetivo objetivo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AiSessaoStatus status = AiSessaoStatus.ABERTA;

  @Column(name = "projeto_id")
  private UUID projetoId;

  @Column(name = "modulo_id")
  private UUID moduloId;

  @Column(name = "cliente_id")
  private UUID clienteId;

  @Column(name = "pagina_id")
  private UUID paginaId;

  @Column(name = "template_id")
  private UUID templateId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "componentes_selecionados", nullable = false, columnDefinition = "jsonb")
  private List<String> componentesSelecionados = List.of();

  @Column(nullable = false, columnDefinition = "text")
  private String briefing;

  /** Ajuste de página: versão da página quando o ajuste foi pedido. */
  @Column(name = "version_base")
  private Long versionBase;

  /** Ajuste de página: seção do esboço que limita o ajuste; nulo = página inteira. */
  @Column(name = "secao_id", length = 20)
  private String secaoId;

  public AiSessao(
      AiObjetivo objetivo,
      String briefing,
      UUID projetoId,
      UUID moduloId,
      UUID clienteId,
      UUID paginaId,
      UUID templateId,
      List<String> componentesSelecionados) {
    this.objetivo = objetivo;
    this.briefing = briefing;
    this.projetoId = projetoId;
    this.moduloId = moduloId;
    this.clienteId = clienteId;
    this.paginaId = paginaId;
    this.templateId = templateId;
    this.componentesSelecionados = componentesSelecionados == null
        ? List.of()
        : List.copyOf(componentesSelecionados);
  }

  public AiSessao(
      AiObjetivo objetivo,
      String briefing,
      UUID projetoId,
      UUID moduloId,
      UUID clienteId,
      UUID paginaId,
      UUID templateId) {
    this(objetivo, briefing, projetoId, moduloId, clienteId, paginaId, templateId, List.of());
  }

  /** Sessão de ajuste (Fase B): parte de uma página existente, numa versão conhecida. */
  public static AiSessao ajuste(
      String instrucao, UUID projetoId, UUID moduloId, UUID paginaId, long versionBase, String secaoId) {
    AiSessao sessao = new AiSessao(
        AiObjetivo.ATUALIZAR_PAGINA, instrucao, projetoId, moduloId, null, paginaId, null, List.of());
    sessao.versionBase = versionBase;
    sessao.secaoId = secaoId;
    sessao.status = AiSessaoStatus.PRONTA_PARA_GERAR;
    return sessao;
  }

  public void aguardarUsuario() {
    this.status = AiSessaoStatus.AGUARDANDO_USUARIO;
  }

  public void prontaParaGerar() {
    this.status = AiSessaoStatus.PRONTA_PARA_GERAR;
  }

  public void gerando() {
    this.status = AiSessaoStatus.GERANDO;
  }

  public void pronta() {
    this.status = AiSessaoStatus.PRONTA;
  }

  public void erro() {
    this.status = AiSessaoStatus.ERRO;
  }

  public void aplicada() {
    this.status = AiSessaoStatus.APLICADA;
  }

  public void cancelar() {
    this.status = AiSessaoStatus.CANCELADA;
  }

  public void definirTemplateId(UUID templateId) {
    this.templateId = templateId;
  }

  public boolean cancelada() {
    return status == AiSessaoStatus.CANCELADA;
  }

  public boolean pertenceA(String usuario) {
    return usuario != null && usuario.equals(getCreatedBy());
  }

  public boolean terminal() {
    return status.terminal();
  }
}
