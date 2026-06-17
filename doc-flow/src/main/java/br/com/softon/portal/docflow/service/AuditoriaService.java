package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.entity.AuditoriaEvento;
import br.com.softon.portal.docflow.repository.AuditoriaRepository;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

  private final AuditoriaRepository auditoriaRepository;

  public void registrar(String entidade, UUID entidadeId, String acao, String descricao, Principal principal) {
    auditoriaRepository.save(new AuditoriaEvento(entidade, entidadeId, acao, descricao, username(principal)));
  }

  public List<AuditoriaEvento> recentes() {
    return auditoriaRepository.findTop100ByOrderByCreatedAtDesc();
  }

  public Page<AuditoriaEvento> listar(Pageable pageable) {
    return auditoriaRepository.findAll(pageable);
  }

  private String username(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }
}
