package org.lucas.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(ServerHttpSecurity serverHttpSecurity){
    serverHttpSecurity.csrf(ServerHttpSecurity.CsrfSpec::disable)
      .authorizeExchange(exchange ->
        exchange.pathMatchers("/eureka/**")
          .permitAll()
          .anyExchange()
          .authenticated())
      .oauth2ResourceServer(oAuth2ResourceServerSpec -> oAuth2ResourceServerSpec.jwt(Customizer.withDefaults()));
    return serverHttpSecurity.build();
  }
}
