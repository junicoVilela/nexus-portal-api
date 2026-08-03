package com.nexus.identityaccess.controller;

import com.nexus.identityaccess.dto.request.LoginRequest;
import com.nexus.identityaccess.dto.response.LoginResponse;
import com.nexus.identityaccess.dto.response.MeResponse;
import com.nexus.identityaccess.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
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
  public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    String token = usuarioService.autenticar(
        request.username(), request.password(),
        ipDe(http), http.getHeader("User-Agent"));
    return new LoginResponse(token, request.username());
  }

  @GetMapping("/me")
  public MeResponse me(Authentication authentication) {
    return usuarioService.me(authentication.getName());
  }

  /** Considera X-Forwarded-For quando presente (proxy/gateway). */
  private static String ipDe(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      int comma = xff.indexOf(',');
      return (comma < 0 ? xff : xff.substring(0, comma)).trim();
    }
    return req.getRemoteAddr();
  }
}
