package com.jessegrabowski.blog.content;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blog.content")
public class ContentProperties {

  private ContentHeadersProperties headers;

  @Data
  public static class ContentHeadersProperties {
    private String reportUri;
    private String reportTo;
  }
}
