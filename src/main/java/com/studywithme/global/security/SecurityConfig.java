package com.studywithme.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithme.auth.oauth.CustomOAuth2UserService;
import com.studywithme.auth.oauth.OAuth2AuthenticationSuccessHandler;
import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.global.config.AppCorsProperties;
import com.studywithme.member.repository.MemberRepository;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
	private final MemberRepository memberRepository;

	public SecurityConfig(
		JwtTokenProvider jwtTokenProvider,
		ObjectMapper objectMapper,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		CustomOAuth2UserService customOAuth2UserService,
		OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler,
		MemberRepository memberRepository
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.customOAuth2UserService = customOAuth2UserService;
		this.oauth2AuthenticationSuccessHandler = oauth2AuthenticationSuccessHandler;
		this.memberRepository = memberRepository;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.securityContext(securityContext -> securityContext.securityContextRepository(new NullSecurityContextRepository()))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
				.requestMatchers("/api/v1/auth/me").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/v1/auth/me/nickname").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/v1/auth/me/signup").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/auth/me").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/studies/me").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/studies/*/join-requests").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/studies", "/api/v1/studies/*").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/*").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/posts/*/comments").permitAll()
				.requestMatchers(
					HttpMethod.POST,
					"/api/v1/studies",
					"/api/v1/studies/*/join",
					"/api/v1/studies/*/leave",
					"/api/v1/studies/*/close",
					"/api/v1/studies/*/end",
					"/api/v1/studies/*/join-requests/*/approve",
					"/api/v1/studies/*/join-requests/*/reject",
					"/api/v1/studies/*/join-requests/cancel",
					"/api/v1/studies/*/chat-room",
					"/api/v1/posts",
					"/api/v1/posts/*/comments",
					"/api/v1/comments/*/replies",
					"/api/v1/chat/private-rooms",
					"/api/v1/chat/rooms/*/messages"
				).authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/v1/studies/*", "/api/v1/posts/*").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/studies/*", "/api/v1/posts/*").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/v1/comments/*").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/comments/*").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/chat/rooms/*").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/notifications").authenticated()
				.requestMatchers(
					HttpMethod.GET,
					"/api/v1/chat/rooms",
					"/api/v1/chat/rooms/*/messages",
					"/api/v1/chat/rooms/*/members"
				).authenticated()
				.requestMatchers(HttpMethod.POST, "/api/v1/notifications/*/read").authenticated()
				.requestMatchers(
					"/actuator/health",
					"/actuator/info",
					"/ws",
					"/ws/**",
					"/oauth2/**",
					"/login/oauth2/**",
					"/error"
				).permitAll()
				.anyRequest().denyAll()
			)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
			.addFilterAfter(signupRequiredFilter(), JwtAuthenticationFilter.class);

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

	@Bean
	public SignupRequiredFilter signupRequiredFilter() {
		return new SignupRequiredFilter(memberRepository, objectMapper);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(AppCorsProperties corsProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
