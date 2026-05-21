package com.studywithme.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.oauth.CustomOAuth2UserService;
import com.studywithme.auth.oauth.OAuth2AuthenticationSuccessHandler;
import com.studywithme.auth.token.JwtTokenProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

	public SecurityConfig(
		JwtTokenProvider jwtTokenProvider,
		ObjectMapper objectMapper,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		CustomOAuth2UserService customOAuth2UserService,
		OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.customOAuth2UserService = customOAuth2UserService;
		this.oauth2AuthenticationSuccessHandler = oauth2AuthenticationSuccessHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.securityContext(securityContext -> securityContext.securityContextRepository(new NullSecurityContextRepository()))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/v1/auth/me").authenticated()
				.requestMatchers(
					"/actuator/health",
					"/actuator/info",
					"/oauth2/**",
					"/login/oauth2/**",
					"/error"
				).permitAll()
				.anyRequest().denyAll()
			)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		if (clientRegistrationRepository.getIfAvailable() != null) {
			http.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
				.successHandler(oauth2AuthenticationSuccessHandler)
			);
		}

		return http.build();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtTokenProvider, objectMapper);
	}
}
