package br.com.softon.portal.docflow.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
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
@Table(name = "tb_usuario")
public class Usuario extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String username;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(length = 150)
  private String nome;

  @Column(length = 200)
  private String email;

  @Column(nullable = false)
  private boolean ativo = true;

  public Usuario(String username, String password, String nome, String email) {
    this.username = username;
    this.password = password;
    this.nome = nome;
    this.email = email;
  }

  public void atualizar(String nome, String email, boolean ativo) {
    this.nome = nome;
    this.email = email;
    this.ativo = ativo;
  }

  public void alterarSenha(String encodedPassword) {
    this.password = encodedPassword;
  }
}
