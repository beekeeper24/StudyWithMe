package com.studywithme.chat.presentation;

import com.studywithme.chat.exception.ChatErrorCode;
import com.studywithme.global.exception.BusinessException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ChatWebSocketDestination {

	private static final Pattern SUBSCRIBE_DESTINATION = Pattern.compile("^/topic/chat\\.rooms\\.(\\d+)$");
	private static final Pattern SEND_DESTINATION = Pattern.compile("^/app/chat\\.rooms\\.(\\d+)\\.messages$");

	private ChatWebSocketDestination() {
	}

	static Long parseRoomId(String destination) {
		if (destination == null) {
			throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
		}

		Matcher subscribeMatcher = SUBSCRIBE_DESTINATION.matcher(destination);
		if (subscribeMatcher.matches()) {
			return Long.valueOf(subscribeMatcher.group(1));
		}

		Matcher sendMatcher = SEND_DESTINATION.matcher(destination);
		if (sendMatcher.matches()) {
			return Long.valueOf(sendMatcher.group(1));
		}

		throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
	}
}
