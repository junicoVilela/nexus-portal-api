package br.com.softon.portal.shared.api;

import java.util.List;
import org.springframework.data.domain.Sort;

public final class SortUtils {
  private SortUtils() {
  }

  public static Sort of(String property, SortDirection direction, List<String> allowedProperties, Sort fallback) {
    if (property == null || property.isBlank()) {
      return fallback;
    }
    String normalized = property.trim();
    if (allowedProperties.stream().noneMatch(prop -> prop.equals(normalized))) {
      return fallback;
    }
    Sort.Direction sortDirection = direction == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
    return Sort.by(sortDirection, normalized);
  }
}
