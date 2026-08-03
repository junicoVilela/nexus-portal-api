package com.nexus.portal.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HubDocFlowPathRewriteFilterTest {

  @Test
  void rewritesDocFlowPrefixToApi() throws Exception {
    var filter = new HubDocFlowPathRewriteFilter();
    var request = new MockHttpServletRequest();
    request.setRequestURI("/api/doc-flow/auth/login");

    AtomicReference<HttpServletRequest> downstream = new AtomicReference<>();
    FilterChain chain = (req, res) -> downstream.set((HttpServletRequest) req);

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(downstream.get().getRequestURI()).isEqualTo("/api/v1/auth/login");
    assertThat(downstream.get().getServletPath()).isEqualTo("/api/v1/auth/login");
  }

  @Test
  void leavesOrdinaryApiPathsUnchanged() throws Exception {
    var filter = new HubDocFlowPathRewriteFilter();
    var request = new MockHttpServletRequest();
    request.setRequestURI("/api/auth/login");

    AtomicReference<HttpServletRequest> downstream = new AtomicReference<>();
    FilterChain chain = (req, res) -> downstream.set((HttpServletRequest) req);

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(downstream.get()).isSameAs(request);
  }
}
