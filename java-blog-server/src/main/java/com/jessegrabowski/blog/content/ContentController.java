package com.jessegrabowski.blog.content;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;

@Controller
@RequiredArgsConstructor
public class ContentController {

  private final ContentProperties properties;

  @Value("${spring.web.locale}")
  private final Locale defaultLocale;

  @GetMapping("/")
  public Rendering root(Locale locale) {
    return properties.getSupportedLanguages().contains(locale.getLanguage())
        ? Rendering.redirectTo("/" + locale.getLanguage() + "/index.html")
            .contextRelative(true)
            .build()
        : Rendering.redirectTo("/" + defaultLocale.getLanguage() + "/index.html")
            .contextRelative(true)
            .build();
  }
}
