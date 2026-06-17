package br.com.softon.portal.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reescreve o prefixo do hub {@code /api/doc-flow/**} para {@code /api/**} no mesmo processo (uma só porta).
 */
public class HubDocFlowPathRewriteFilter extends OncePerRequestFilter {

  static final String DOC_FLOW_API_PREFIX = "/api/doc-flow";

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
    String uri = request.getRequestURI();
    if (!uri.startsWith(contextPath)) {
      filterChain.doFilter(request, response);
      return;
    }
    String pathWithinContext = uri.substring(contextPath.length());

    if (!pathWithinContext.startsWith(DOC_FLOW_API_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String remainder = pathWithinContext.substring(DOC_FLOW_API_PREFIX.length());
    if (remainder.isEmpty()) {
      remainder = "/";
    } else if (!remainder.startsWith("/")) {
      remainder = "/" + remainder;
    }
    String newPathWithinContext = "/api/v1" + ("/".equals(remainder) ? "" : remainder);
    String newUri = contextPath + newPathWithinContext;

    filterChain.doFilter(new RewrittenRequest(request, newUri, newPathWithinContext), response);
  }

  static final class RewrittenRequest extends HttpServletRequestWrapper {

    private final String requestUri;
    private final String servletPath;

    RewrittenRequest(HttpServletRequest request, String requestUri, String servletPath) {
      super(request);
      this.requestUri = requestUri;
      this.servletPath = servletPath;
    }

    @Override
    public String getRequestURI() {
      return requestUri;
    }

    @Override
    public String getServletPath() {
      return servletPath;
    }

    @Override
    public String getPathInfo() {
      return null;
    }
  }
}
