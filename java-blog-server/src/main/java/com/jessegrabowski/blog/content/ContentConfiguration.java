package com.jessegrabowski.blog.content;

import com.jessegrabowski.TemplateRouterFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
@EnableConfigurationProperties(ContentProperties.class)
public class ContentConfiguration {

  @Bean
  public RouterFunction<ServerResponse> templateRouterFunction(ContentProperties properties) {
    return new TemplateRouterFactory(
            properties.getHeaders().getReportUri(), properties.getHeaders().getReportTo())
        .create();
  }
}
