package com.nexus.portal.ai.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/** Sanitização alinhada ao editor DocFlow ({@code PaginaService#sanitizar}). */
@Component
public class AiHtmlSanitizer {

  public String sanitizar(String html) {
    if (html == null || html.isBlank()) {
      return html == null ? "" : html;
    }
    Safelist safelist = Safelist.relaxed()
        .addTags("section", "article", "aside", "figure", "figcaption")
        .addAttributes(":all", "class", "data-codigo-tela")
        .addAttributes("img", "src", "alt", "title", "loading")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https", "data")
        .preserveRelativeLinks(true);
    return Jsoup.clean(html, "https://localhost/", safelist);
  }
}
