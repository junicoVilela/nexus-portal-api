package com.nexus.portal.docflow.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "tb_cliente")
public class Cliente extends AuditableEntity {

  /** Indigo do portal / templates de edição (#4f46e5). */
  public static final String TEMA_COR_PRIMARIA_PADRAO = "#4f46e5";
  public static final String TEMA_COR_FUNDO_PADRAO = "#f7f8fa";
  public static final String TEMA_COR_PRIMARIA_HOVER_PADRAO = "#4338ca";
  public static final String TEMA_COR_SOFT_PADRAO = "#eef0ff";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, unique = true, length = 150)
  private String slug;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "logo_path", length = 500)
  private String logoPath;

  @Column(name = "logo_content_type", length = 100)
  private String logoContentType;

  @Column(name = "tema_cor_primaria", length = 20)
  private String temaCorPrimaria;

  @Column(name = "tema_cor_fundo", length = 20)
  private String temaCorFundo;

  /**
   * Tema padronizado (V29): todo manual usa a identidade do portal. As colunas de tema ficam com o
   * padrão e não vêm mais da API.
   */
  public Cliente(String nome, String slug, boolean ativo) {
    this.nome = nome;
    this.slug = slug;
    this.ativo = ativo;
    this.temaCorPrimaria = TEMA_COR_PRIMARIA_PADRAO;
    this.temaCorFundo = TEMA_COR_FUNDO_PADRAO;
  }

  public void atualizar(String nome, String slug, boolean ativo) {
    this.nome = nome;
    this.slug = slug;
    this.ativo = ativo;
  }

  public void definirLogo(String path, String contentType) {
    this.logoPath = path;
    this.logoContentType = contentType;
  }

  public void removerLogo() {
    this.logoPath = null;
    this.logoContentType = null;
  }

  public String getTemaCorPrimariaOuPadrao() {
    return TEMA_COR_PRIMARIA_PADRAO;
  }

  public String getTemaCorFundoOuPadrao() {
    return TEMA_COR_FUNDO_PADRAO;
  }
}
