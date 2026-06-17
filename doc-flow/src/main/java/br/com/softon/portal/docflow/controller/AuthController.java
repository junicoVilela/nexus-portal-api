package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.softon.portal.docflow.dto.request.LoginRequest;
import br.com.softon.portal.docflow.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;

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
}
