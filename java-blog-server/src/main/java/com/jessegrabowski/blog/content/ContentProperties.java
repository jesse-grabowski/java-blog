package com.jessegrabowski.blog.content;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blog.content")
public class ContentProperties {

  private Set<String> supportedLanguages;
}
