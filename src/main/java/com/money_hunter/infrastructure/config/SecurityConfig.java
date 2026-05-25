package com.money_hunter.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/actuator/health").permitAll()
						.requestMatchers("/api/**").permitAll()
						.anyRequest().permitAll())
				.httpBasic(httpBasic -> {
				})
				.formLogin(formLogin -> {
				})
				.build();
	}
}
