package com.jessegrabowski.blog.content;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        ? Rendering.redirectTo("/" + locale.getLanguage() + "/").contextRelative(true).build()
        : Rendering.redirectTo("/" + defaultLocale.getLanguage() + "/")
            .contextRelative(true)
            .build();
  }

  @GetMapping("/{lang:[a-z]{2}}/")
  public Rendering languageRoot(@PathVariable("lang") String lang) {
    return properties.getSupportedLanguages().contains(lang)
        ? Rendering.view("%s/index".formatted(lang)).build()
        : Rendering.redirectTo("/%s/".formatted(defaultLocale.getLanguage()))
            .contextRelative(true)
            .build();
  }
}
