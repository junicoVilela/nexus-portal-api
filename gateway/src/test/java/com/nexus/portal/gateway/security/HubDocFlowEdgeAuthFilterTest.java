package com.nexus.portal.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HubDocFlowEdgeAuthFilterTest {

  @Test
  void skipsNonDocFlowPaths() throws Exception {
    var jwt = new GatewayEdgeJwtService(
        "doc-flow-dev-only-jwt-secret-min-256-bits-do-not-use-in-production-change-me");
    var filter = new HubDocFlowEdgeAuthFilter(jwt, "k");
    var req = new MockHttpServletRequest();
    req.setRequestURI("/api/auth/login");

    AtomicInteger downstream = new AtomicInteger();
    FilterChain chain = (r, s) -> downstream.incrementAndGet();

    filter.doFilter(req, new MockHttpServletResponse(), chain);
    assertThat(downstream.get()).isEqualTo(1);
  }

  @Test
  void unauthorizedWithoutBearerOnDocFlowPath() throws Exception {
    var jwt = new GatewayEdgeJwtService(
        "doc-flow-dev-only-jwt-secret-min-256-bits-do-not-use-in-production-change-me");
    var filter = new HubDocFlowEdgeAuthFilter(jwt, "k");
    var req = new MockHttpServletRequest();
    req.setRequestURI("/api/doc-flow/usuarios");
    var res = new MockHttpServletResponse();

    filter.doFilter(req, res, (r, s) -> { });

    assertThat(res.getStatus()).isEqualTo(401);
  }
}
