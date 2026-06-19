package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.dto.request.LoginRequest;
import br.com.softon.portal.docflow.dto.response.LoginResponse;
import br.com.softon.portal.docflow.dto.response.MeResponse;
import br.com.softon.portal.docflow.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UsuarioService usuarioService;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    String token = usuarioService.autenticar(request.username(), request.password());
    return new LoginResponse(token, request.username());
  }

  @GetMapping("/me")
  public MeResponse me(Authentication authentication) {
    return usuarioService.me(authentication.getName());
  }
}
