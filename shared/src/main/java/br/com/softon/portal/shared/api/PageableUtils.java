package br.com.softon.portal.shared.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {
  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_SIZE = 10;
  private static final int MAX_SIZE = 1000;

  private PageableUtils() {
  }

  public static Pageable of(Integer page, Integer size, Sort sort) {
    int safePage = page == null ? DEFAULT_PAGE : Math.max(DEFAULT_PAGE, page);
    int safeSize = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));
    return PageRequest.of(safePage - 1, safeSize, sort);
  }
}
