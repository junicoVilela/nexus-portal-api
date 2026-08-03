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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorContato")
@Table(name = "tb_contato_orchestrator")
public class Contato extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @Column(nullable = false, length = 200)
  private String nome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PapelContato papel;

  @Column(nullable = false, length = 200)
  private String email;

  @Column(length = 40)
  private String telefone;

  public Contato(Cliente cliente, String nome, PapelContato papel, String email, String telefone) {
    this.cliente = cliente;
    this.nome = nome.trim();
    this.papel = papel;
    this.email = email.trim().toLowerCase();
    this.telefone = telefone;
  }

  public void atualizar(String nome, PapelContato papel, String email, String telefone) {
    this.nome = nome.trim();
    this.papel = papel;
    this.email = email.trim().toLowerCase();
    this.telefone = telefone;
  }
}
