package com.studywithme.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigRouteContractTest.UnlistedApiController.class)
class SecurityConfigRouteContractTest {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@MethodSource("authenticatedRoutes")
	@DisplayName("사이트 기능 API는 인증 없이 접근하면 AUTH-003 응답을 반환한다")
	void rejectAuthenticatedRoutesWithoutAuthentication(HttpMethod method, String path) throws Exception {
		mockMvc.perform(request(method, path))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@ParameterizedTest
	@MethodSource("unlistedRoutes")
	@DisplayName("명시되지 않은 API는 기본 공개 경로로 취급하지 않고 인증을 요구한다")
	void denyUnlistedRoutes(String path) throws Exception {
		mockMvc.perform(get(path))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	private static Arguments[] authenticatedRoutes() {
		return new Arguments[] {
			Arguments.of(HttpMethod.GET, "/api/v1/auth/me"),
			Arguments.of(HttpMethod.GET, "/api/v1/studies/me"),
			Arguments.of(HttpMethod.GET, "/api/v1/studies"),
			Arguments.of(HttpMethod.GET, "/api/v1/studies/1"),
			Arguments.of(HttpMethod.GET, "/api/v1/studies/1/join-requests"),
			Arguments.of(HttpMethod.GET, "/api/v1/posts"),
			Arguments.of(HttpMethod.GET, "/api/v1/posts/1"),
			Arguments.of(HttpMethod.GET, "/api/v1/posts/1/comments"),
			Arguments.of(HttpMethod.GET, "/api/v1/notifications"),
			Arguments.of(HttpMethod.GET, "/api/v1/chat/rooms"),
			Arguments.of(HttpMethod.GET, "/api/v1/admin/chat-message-reports"),
			Arguments.of(HttpMethod.GET, "/api/v1/admin/content-reports"),
			Arguments.of(HttpMethod.GET, "/api/v1/admin/member-sanctions"),
			Arguments.of(HttpMethod.POST, "/api/v1/studies"),
			Arguments.of(HttpMethod.POST, "/api/v1/posts"),
			Arguments.of(HttpMethod.POST, "/api/v1/posts/1/comments"),
			Arguments.of(HttpMethod.POST, "/api/v1/posts/1/reports"),
			Arguments.of(HttpMethod.POST, "/api/v1/comments/1/reports"),
			Arguments.of(HttpMethod.POST, "/api/v1/notifications/read-all"),
			Arguments.of(HttpMethod.POST, "/api/v1/chat/rooms/1/messages/1/reports"),
			Arguments.of(HttpMethod.POST, "/api/v1/admin/chat-message-reports/1/assign"),
			Arguments.of(HttpMethod.POST, "/api/v1/admin/chat-message-reports/1/handle"),
			Arguments.of(HttpMethod.POST, "/api/v1/admin/content-reports/1/assign"),
			Arguments.of(HttpMethod.POST, "/api/v1/admin/content-reports/1/handle"),
			Arguments.of(HttpMethod.POST, "/api/v1/admin/member-sanctions"),
			Arguments.of(HttpMethod.PUT, "/api/v1/posts/1"),
			Arguments.of(HttpMethod.DELETE, "/api/v1/posts/1"),
			Arguments.of(HttpMethod.DELETE, "/api/v1/chat/rooms/1"),
			Arguments.of(HttpMethod.DELETE, "/api/v1/chat/rooms/1/messages/1"),
		};
	}

	private static Arguments[] unlistedRoutes() {
		return new Arguments[] {
			Arguments.of("/api/v1/future-public-route"),
			Arguments.of("/api/v1/studies/1/unknown-public-view"),
			Arguments.of("/api/v1/posts/1/unknown-public-view"),
		};
	}

	private static RequestBuilder request(HttpMethod method, String path) {
		MockHttpServletRequestBuilder builder = switch (method.name()) {
			case "GET" -> get(path);
			case "POST" -> post(path);
			case "PUT" -> put(path);
			case "DELETE" -> delete(path);
			default -> throw new IllegalArgumentException("Unsupported method: " + method);
		};
		return builder;
	}

	@RestController
	static class UnlistedApiController {

		@GetMapping({
			"/api/v1/future-public-route",
			"/api/v1/studies/1/unknown-public-view",
			"/api/v1/posts/1/unknown-public-view"
		})
		String ok() {
			return "ok";
		}
	}
}
