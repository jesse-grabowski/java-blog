package com.jessegrabowski.blog;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            authorize -> authorize.antMatchers(HttpMethod.GET, "/**").permitAll())
        .csrf(csrf -> {})
        .build();
  }
}
