package br.com.softon.portal.docflow.config;

import br.com.softon.portal.docflow.entity.Usuario;
import br.com.softon.portal.docflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    if (!usuarioRepository.existsByUsername("admin")) {
      usuarioRepository.save(new Usuario("admin",
          passwordEncoder.encode("admin"), "Administrador", null, "ADMIN,EDITOR"));
    }
    if (!usuarioRepository.existsByUsername("editor")) {
      usuarioRepository.save(new Usuario("editor",
          passwordEncoder.encode("editor"), "Editor", null, "EDITOR"));
    }
  }
}
