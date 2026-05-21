package com.studywithme.notification.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studywithme.auth.token.JwtTokenProvider;
import com.studywithme.auth.token.RefreshTokenRepository;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.OAuthProvider;
import com.studywithme.member.repository.MemberRepository;
import com.studywithme.notification.domain.Notification;
import com.studywithme.notification.domain.NotificationTargetType;
import com.studywithme.notification.domain.NotificationType;
import com.studywithme.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		notificationRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("인증하지 않고 내 알림 목록을 조회하면 AUTH-003 응답을 반환한다")
	void rejectUnauthenticatedListNotifications() throws Exception {
		mockMvc.perform(get("/api/v1/notifications"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("AUTH-003"));
	}

	@Test
	@DisplayName("인증한 회원은 자신의 알림 목록을 최신순으로 조회한다")
	void listMyNotifications() throws Exception {
		Member receiver = saveMember("receiver");
		Member actor = saveMember("actor");
		notificationRepository.save(Notification.create(
			receiver.getId(),
			actor.getId(),
			NotificationType.COMMENT_ON_POST,
			NotificationTargetType.COMMENT,
			1L,
			"event-1",
			"새 댓글이 달렸습니다."
		));

		mockMvc.perform(get("/api/v1/notifications")
				.header("Authorization", "Bearer " + accessToken(receiver)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].type").value("COMMENT_ON_POST"))
			.andExpect(jsonPath("$.data[0].read").value(false));
	}

	@Test
	@DisplayName("인증한 회원은 자신의 알림을 읽음 처리할 수 있다")
	void readMyNotification() throws Exception {
		Member receiver = saveMember("receiver");
		Member actor = saveMember("actor");
		Notification notification = notificationRepository.save(Notification.create(
			receiver.getId(),
			actor.getId(),
			NotificationType.COMMENT_ON_POST,
			NotificationTargetType.COMMENT,
			1L,
			"event-1",
			"새 댓글이 달렸습니다."
		));

		mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", notification.getId())
				.header("Authorization", "Bearer " + accessToken(receiver)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.read").value(true));
	}

	@Test
	@DisplayName("다른 회원의 알림은 읽음 처리할 수 없다")
	void rejectReadOtherMemberNotification() throws Exception {
		Member receiver = saveMember("receiver");
		Member actor = saveMember("actor");
		Member other = saveMember("other");
		Notification notification = notificationRepository.save(Notification.create(
			receiver.getId(),
			actor.getId(),
			NotificationType.COMMENT_ON_POST,
			NotificationTargetType.COMMENT,
			1L,
			"event-1",
			"새 댓글이 달렸습니다."
		));

		mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", notification.getId())
				.header("Authorization", "Bearer " + accessToken(other)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("NOTIFICATION-002"));
	}

	private Member saveMember(String name) {
		return memberRepository.saveAndFlush(Member.createOAuthMember(
			name + "@example.com",
			name,
			OAuthProvider.GOOGLE,
			"google-" + name,
			null
		));
	}

	private String accessToken(Member member) {
		return jwtTokenProvider.createAccessToken(member).token();
	}
}
