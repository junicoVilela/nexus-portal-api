package br.com.softon.portal.docflow.entity;

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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ajuda_conteudo")
public class AjudaConteudo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String codigo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoAjudaConteudo tipo;

  @Column(name = "jornada_codigo", length = 80)
  private String jornadaCodigo;

  @Column(nullable = false, length = 160)
  private String titulo;

  @Column(length = 400)
  private String resumo;

  @Column(columnDefinition = "TEXT")
  private String conteudo;

  @Column(name = "rota_contexto", length = 220)
  private String rotaContexto;

  @Column(name = "rota_acao", length = 220)
  private String rotaAcao;

  @Column(name = "rotulo_acao", length = 80)
  private String rotuloAcao;

  @Column(length = 50)
  private String icone;

  @Column(name = "seletor_alvo", length = 200)
  private String seletorAlvo;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_tipo", nullable = false, length = 20)
  private TipoAjudaMedia mediaTipo = TipoAjudaMedia.NENHUMA;

  @Column(name = "media_urls", columnDefinition = "TEXT")
  private String mediaUrls;

  @Column(name = "media_alt", length = 240)
  private String mediaAlt;

  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false)
  private boolean ativo = true;

  public AjudaConteudo(String codigo, TipoAjudaConteudo tipo, String titulo) {
    this.codigo = codigo;
    this.tipo = tipo;
    this.titulo = titulo;
  }

  public void atualizar(TipoAjudaConteudo tipo, String jornadaCodigo, String titulo, String resumo,
      String conteudo, String rotaContexto, String rotaAcao, String rotuloAcao, String icone,
      String seletorAlvo, TipoAjudaMedia mediaTipo, String mediaUrls, String mediaAlt, int ordem,
      boolean ativo) {
    this.tipo = tipo;
    this.jornadaCodigo = jornadaCodigo;
    this.titulo = titulo;
    this.resumo = resumo;
    this.conteudo = conteudo;
    this.rotaContexto = rotaContexto;
    this.rotaAcao = rotaAcao;
    this.rotuloAcao = rotuloAcao;
    this.icone = icone;
    this.seletorAlvo = seletorAlvo;
    this.mediaTipo = mediaTipo;
    this.mediaUrls = mediaUrls;
    this.mediaAlt = mediaAlt;
    this.ordem = ordem;
    this.ativo = ativo;
  }
}
