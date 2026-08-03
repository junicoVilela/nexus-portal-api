package com.nexus.identityaccess.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_grupo")
public class Grupo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(length = 500)
  private String descricao;

  @Column(nullable = false)
  private boolean ativo = true;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "tb_grupo_permissao", joinColumns = @JoinColumn(name = "grupo_id"))
  @Column(name = "permissao_id")
  private List<UUID> permissaoIds = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "tb_grupo_usuario", joinColumns = @JoinColumn(name = "grupo_id"))
  @Column(name = "usuario_id")
  private List<UUID> usuarios = new ArrayList<>();

  public Grupo(String codigo, String nome, String descricao, boolean ativo) {
    this.codigo = codigo;
    this.nome = nome;
    this.descricao = descricao;
    this.ativo = ativo;
  }

  public void atualizar(String nome, String descricao, boolean ativo) {
    this.nome = nome;
    this.descricao = descricao;
    this.ativo = ativo;
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }

  public void atualizarPermissaoIds(List<UUID> novasPermissoes) {
    this.permissaoIds.clear();
    if (novasPermissoes != null) {
      this.permissaoIds.addAll(novasPermissoes);
    }
  }

  public void atualizarUsuarios(List<UUID> novosUsuarios) {
    this.usuarios.clear();
    if (novosUsuarios != null) {
      this.usuarios.addAll(novosUsuarios);
    }
  }
}
