package com.studywithme.chat.presentation;

import com.studywithme.auth.exception.AuthErrorCode;
import com.studywithme.chat.application.ChatMessageCreateCommand;
import com.studywithme.chat.domain.ChatMessageReportStatus;
import com.studywithme.chat.application.ChatService;
import com.studywithme.global.common.ApiResponse;
import com.studywithme.global.exception.BusinessException;
import com.studywithme.global.security.AuthenticatedMemberPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;

	public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
		this.chatService = chatService;
		this.messagingTemplate = messagingTemplate;
	}

	@PostMapping("/chat/private-rooms")
	public ApiResponse<ChatRoomResponse> createPrivateRoom(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody PrivateChatRoomCreateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ChatRoomResponse.from(
			chatService.createPrivateRoom(authenticatedPrincipal.memberId(), request.targetMemberId())
		));
	}

	@PostMapping("/studies/{studyId}/chat-room")
	public ApiResponse<ChatRoomResponse> createStudyRoom(
		@PathVariable Long studyId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ChatRoomResponse.from(
			chatService.createStudyRoom(studyId, authenticatedPrincipal.memberId())
		));
	}

	@GetMapping("/chat/rooms")
	public ApiResponse<List<ChatRoomResponse>> findMyRooms(
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(chatService.findMyRooms(authenticatedPrincipal.memberId()).stream()
			.map(ChatRoomResponse::from)
			.toList());
	}

	@GetMapping("/chat/rooms/{roomId}/members")
	public ApiResponse<List<ChatRoomMemberResponse>> findRoomMembers(
		@PathVariable Long roomId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(chatService.findRoomMembers(roomId, authenticatedPrincipal.memberId()).stream()
			.map(ChatRoomMemberResponse::from)
			.toList());
	}

	@DeleteMapping("/chat/rooms/{roomId}")
	public ApiResponse<Void> hideRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		chatService.hideRoom(roomId, authenticatedPrincipal.memberId());
		return ApiResponse.success(null);
	}

	@PostMapping("/chat/rooms/{roomId}/messages")
	public ApiResponse<ChatMessageResponse> sendMessage(
		@PathVariable Long roomId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody ChatMessageCreateRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ChatMessageResponse.from(
			chatService.sendMessage(
				roomId,
				authenticatedPrincipal.memberId(),
				new ChatMessageCreateCommand(request.content())
			)
		));
	}

	@GetMapping("/chat/rooms/{roomId}/messages")
	public ApiResponse<List<ChatMessageResponse>> findMessages(
		@PathVariable Long roomId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(chatService.findMessages(roomId, authenticatedPrincipal.memberId()).stream()
			.map(ChatMessageResponse::from)
			.toList());
	}

	@DeleteMapping("/chat/rooms/{roomId}/messages/{messageId}")
	public ApiResponse<ChatMessageResponse> deleteMessage(
		@PathVariable Long roomId,
		@PathVariable Long messageId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		ChatMessageResponse response = ChatMessageResponse.from(
			chatService.deleteMessage(roomId, messageId, authenticatedPrincipal.memberId())
		);
		publishRoomMessage(roomId, authenticatedPrincipal.memberId(), response);
		return ApiResponse.success(response);
	}

	@PostMapping("/chat/rooms/{roomId}/messages/{messageId}/reports")
	public ApiResponse<ChatMessageReportResponse> reportMessage(
		@PathVariable Long roomId,
		@PathVariable Long messageId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody ChatMessageReportRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ChatMessageReportResponse.from(chatService.reportMessage(
			roomId,
			messageId,
			authenticatedPrincipal.memberId(),
			request.reason()
		)));
	}

	@GetMapping("/admin/chat-message-reports")
	public ApiResponse<List<ChatMessageReportResponse>> findMessageReports(
		@RequestParam(required = false) ChatMessageReportStatus status,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(chatService.findMessageReports(authenticatedPrincipal.memberId(), status).stream()
			.map(ChatMessageReportResponse::from)
			.toList());
	}

	@PostMapping("/admin/chat-message-reports/{reportId}/handle")
	public ApiResponse<ChatMessageReportResponse> handleMessageReport(
		@PathVariable Long reportId,
		@AuthenticationPrincipal AuthenticatedMemberPrincipal principal,
		@Valid @RequestBody ChatMessageReportHandleRequest request
	) {
		AuthenticatedMemberPrincipal authenticatedPrincipal = requirePrincipal(principal);
		return ApiResponse.success(ChatMessageReportResponse.from(chatService.handleMessageReport(
			reportId,
			authenticatedPrincipal.memberId(),
			request.status(),
			request.handlingNote()
		)));
	}

	private void publishRoomMessage(Long roomId, Long requesterMemberId, ChatMessageResponse response) {
		chatService.findRoomMembers(roomId, requesterMemberId)
			.forEach(member -> messagingTemplate.convertAndSendToUser(
				member.memberId().toString(),
				"/queue/chat.rooms." + roomId,
				response
			));
	}

	private AuthenticatedMemberPrincipal requirePrincipal(AuthenticatedMemberPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return principal;
	}
}
