package com.nexus.identityaccess.service;

import com.nexus.identityaccess.entity.HistoricoSenha;
import com.nexus.identityaccess.entity.PoliticaSenha;
import com.nexus.identityaccess.repository.HistoricoSenhaRepository;
import com.nexus.identityaccess.repository.PoliticaSenhaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PoliticaSenhaService {

  private static final Pattern MAIUSCULA = Pattern.compile("[A-ZÁÀÂÃÉÊÍÓÔÕÚÇ]");
  private static final Pattern MINUSCULA = Pattern.compile("[a-záàâãéêíóôõúç]");
  private static final Pattern NUMERO = Pattern.compile("\\d");
  private static final Pattern ESPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>\\[\\]\\\\/_\\-+=`~';]");

  private final PoliticaSenhaRepository politicaRepository;
  private final HistoricoSenhaRepository historicoRepository;
  private final AuditoriaService auditoriaService;
  private final PasswordEncoder passwordEncoder;

  public PoliticaSenha atual() {
    return politicaRepository.findById(PoliticaSenha.SINGLETON_ID)
        .orElseThrow(() -> new IllegalStateException(
            "Política de senha não inicializada (seed V13 ausente?)."));
  }

  @Transactional
  public PoliticaSenha atualizar(int tamanhoMinimo, boolean exigirMaiuscula,
      boolean exigirMinuscula, boolean exigirNumero, boolean exigirEspecial,
      Integer expiraSenhaDias, int quantidadeHistorico, int maxTentativasInvalidas,
      Principal principal) {
    PoliticaSenha p = atual();
    p.atualizar(tamanhoMinimo, exigirMaiuscula, exigirMinuscula, exigirNumero, exigirEspecial,
        expiraSenhaDias, quantidadeHistorico, maxTentativasInvalidas);
    auditoriaService.registrar("PoliticaSenha", p.getId(), "EDITAR",
        "Política de senha atualizada.", principal);
    return p;
  }

  /** Valida a senha crua contra a política atual. Lança BusinessException se inválida. */
  public void validarOuFalhar(String senha) {
    List<String> violacoes = validar(senha, atual());
    if (!violacoes.isEmpty()) {
      throw new BusinessException("Senha não atende à política: " + String.join(" ", violacoes));
    }
  }

  public List<String> validar(String senha, PoliticaSenha p) {
    List<String> out = new ArrayList<>();
    if (senha == null || senha.length() < p.getTamanhoMinimo()) {
      out.add("Mínimo de " + p.getTamanhoMinimo() + " caracteres.");
    }
    if (p.isExigirMaiuscula() && (senha == null || !MAIUSCULA.matcher(senha).find())) {
      out.add("Deve conter ao menos uma letra maiúscula.");
    }
    if (p.isExigirMinuscula() && (senha == null || !MINUSCULA.matcher(senha).find())) {
      out.add("Deve conter ao menos uma letra minúscula.");
    }
    if (p.isExigirNumero() && (senha == null || !NUMERO.matcher(senha).find())) {
      out.add("Deve conter ao menos um número.");
    }
    if (p.isExigirEspecial() && (senha == null || !ESPECIAL.matcher(senha).find())) {
      out.add("Deve conter ao menos um caractere especial.");
    }
    return out;
  }

  /** Verifica se a senha crua bate com algum hash das N últimas do usuário. */
  public boolean reutilizada(UUID usuarioId, String senhaRaw) {
    PoliticaSenha p = atual();
    if (p.getQuantidadeHistorico() <= 0) return false;
    List<HistoricoSenha> historico = historicoRepository
        .findByUsuarioIdOrderByCreatedAtDesc(usuarioId).stream()
        .limit(p.getQuantidadeHistorico()).toList();
    return historico.stream().anyMatch(h -> passwordEncoder.matches(senhaRaw, h.getSenhaHash()));
  }

  /** Grava o hash da senha no histórico e faz trim para respeitar quantidadeHistorico. */
  @Transactional
  public void registrarNoHistorico(UUID usuarioId, String senhaHash) {
    historicoRepository.save(new HistoricoSenha(usuarioId, senhaHash));
    PoliticaSenha p = atual();
    List<HistoricoSenha> todos = historicoRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId);
    if (todos.size() > p.getQuantidadeHistorico()) {
      historicoRepository.deleteAll(todos.subList(p.getQuantidadeHistorico(), todos.size()));
    }
  }
}
