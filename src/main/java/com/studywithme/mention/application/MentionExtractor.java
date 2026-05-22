package com.studywithme.mention.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MentionExtractor {

	private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![A-Za-z0-9._%+-])@([A-Za-z0-9가-힣_-]{1,50})");

	public List<String> extract(String content) {
		if (content == null || content.isBlank()) {
			return List.of();
		}
		Matcher matcher = MENTION_PATTERN.matcher(content);
		Set<String> nicknames = new LinkedHashSet<>();
		while (matcher.find()) {
			nicknames.add(matcher.group(1));
		}
		return List.copyOf(nicknames);
	}
}
