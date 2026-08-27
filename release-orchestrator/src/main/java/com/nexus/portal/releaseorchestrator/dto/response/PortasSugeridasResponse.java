package com.nexus.portal.releaseorchestrator.dto.response;

import java.util.List;

public record PortasSugeridasResponse(
    List<Integer> backend,
    List<Integer> frontend,
    List<Integer> emUsoNoHost,
    boolean verificouHost) {

  public PortasSugeridasResponse(List<Integer> backend, List<Integer> frontend) {
    this(backend, frontend, List.of(), false);
  }
}
