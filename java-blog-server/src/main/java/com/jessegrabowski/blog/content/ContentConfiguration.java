package com.jessegrabowski.blog.content;

import com.jessegrabowski.TemplateRouterConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TemplateRouterConfiguration.class)
@EnableConfigurationProperties(ContentProperties.class)
public class ContentConfiguration {}
