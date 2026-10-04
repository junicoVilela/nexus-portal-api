package com.nexus.portal.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** PR mergeado recebido pelo webhook do GitHub (Fase C) e o que a IA fez com ele. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ai_pr_evento")
public class AiPrEvento {

  private static final int MAX_MENSAGEM = 500;

  @Id
  private UUID id;

  @Column(name = "delivery_id", length = 80, updatable = false)
  private String deliveryId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private AiFilaOrigem origem = AiFilaOrigem.PR;

  @Column(name = "release_id", updatable = false)
  private UUID releaseId;

  @Column(nullable = false, length = 200, updatable = false)
  private String repositorio;

  @Column(name = "numero_pr", updatable = false)
  private Integer numeroPr;

  @Column(nullable = false, length = 500)
  private String titulo;

  /** Descrição do PR (Markdown), base do briefing; guardada para reprocessar sem ir ao GitHub. */
  @Column(columnDefinition = "text")
  private String corpo;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<String> rotulos = List.of();

  @Column(nullable = false, length = 500)
  private String url;

  @Column(length = 120)
  private String autor;

  @Column(name = "branch_base", nullable = false, length = 200)
  private String branchBase;

  @Column(name = "merge_sha", length = 64)
  private String mergeSha;

  @Column(name = "merged_at")
  private OffsetDateTime mergedAt;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private AiPrClassificacao classificacao;

  @Column(name = "codigo_tela", length = 120)
  private String codigoTela;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AiPrEventoStatus status = AiPrEventoStatus.RECEBIDO;

  @Column(length = MAX_MENSAGEM)
  private String mensagem;

  @Column(name = "sessao_id")
  private UUID sessaoId;

  @Column(name = "pagina_id")
  private UUID paginaId;

  @Column(length = 120)
  private String responsavel;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  public AiPrEvento(
      String deliveryId,
      String repositorio,
      int numeroPr,
      String titulo,
      String corpo,
      List<String> rotulos,
      String url,
      String autor,
      String branchBase,
      String mergeSha,
      OffsetDateTime mergedAt) {
    this.id = UUID.randomUUID();
    this.deliveryId = deliveryId;
    this.repositorio = repositorio;
    this.numeroPr = numeroPr;
    this.titulo = truncar(titulo, 500);
    this.corpo = corpo;
    this.rotulos = rotulos == null ? List.of() : List.copyOf(rotulos);
    this.url = url;
    this.autor = autor;
    this.branchBase = branchBase;
    this.mergeSha = mergeSha;
    this.mergedAt = mergedAt;
  }

  /**
   * Tela citada numa release publicada (INT-301): fica PARA_REVISAR até alguém gerar o ajuste
   * ou dispensar. {@code texto} (resumo e itens) vira a instrução do ajuste.
   */
  public static AiPrEvento daRelease(UUID releaseId, String rotulo, String titulo, String texto, String caminho,
      String codigoTela, UUID paginaId) {
    AiPrEvento evento = new AiPrEvento();
    evento.id = UUID.randomUUID();
    evento.origem = AiFilaOrigem.RELEASE;
    evento.releaseId = releaseId;
    evento.repositorio = rotulo;
    evento.titulo = truncar(titulo == null || titulo.isBlank() ? rotulo : rotulo + " — " + titulo, 500);
    evento.corpo = texto;
    evento.url = caminho;
    evento.branchBase = "release";
    evento.classificacao = AiPrClassificacao.UI_ALTERACAO;
    evento.codigoTela = codigoTela;
    evento.paginaId = paginaId;
    evento.status = AiPrEventoStatus.PARA_REVISAR;
    evento.mensagem = "A release " + rotulo + " cita esta tela: gere o ajuste ou dispense se nada mudou.";
    return evento;
  }

  /** "Revisado, nada a mudar." */
  public void dispensar(String usuario) {
    this.responsavel = usuario;
    mudar(AiPrEventoStatus.IGNORADO, "Dispensado por " + usuario + ": a página já está correta.");
  }

  public void classificar(AiPrClassificacao classificacao, String codigoTela) {
    this.classificacao = classificacao;
    this.codigoTela = codigoTela;
    tocar();
  }

  public void ignorar(String motivo) {
    mudar(AiPrEventoStatus.IGNORADO, motivo);
  }

  public void aguardarRascunho(UUID paginaId, String motivo) {
    this.paginaId = paginaId;
    mudar(AiPrEventoStatus.AGUARDANDO_RASCUNHO, motivo);
  }

  public void emFila(UUID sessaoId, UUID paginaId, String mensagem) {
    this.sessaoId = sessaoId;
    this.paginaId = paginaId;
    mudar(AiPrEventoStatus.EM_FILA, mensagem);
  }

  public void falhar(String motivo) {
    mudar(AiPrEventoStatus.ERRO, motivo);
  }

  public void assumir(String usuario) {
    this.responsavel = usuario;
    tocar();
  }

  /** Só itens sem sessão em andamento voltam a ser processados. */
  public boolean podeReprocessar() {
    return status == AiPrEventoStatus.ERRO || status == AiPrEventoStatus.AGUARDANDO_RASCUNHO
        || status == AiPrEventoStatus.RECEBIDO || status == AiPrEventoStatus.PARA_REVISAR;
  }

  private void mudar(AiPrEventoStatus novo, String texto) {
    this.status = novo;
    this.mensagem = truncar(texto, MAX_MENSAGEM);
    tocar();
  }

  private void tocar() {
    this.updatedAt = OffsetDateTime.now();
  }

  private static String truncar(String texto, int max) {
    if (texto == null) {
      return null;
    }
    return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
  }
}
