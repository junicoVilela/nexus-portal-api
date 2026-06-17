package br.com.softon.portal.shared.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {
  private SlugUtils() {
  }

  public static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
    return normalized.isBlank() ? null : normalized;
  }
}
