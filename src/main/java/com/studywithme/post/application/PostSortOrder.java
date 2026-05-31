package com.studywithme.post.application;

import org.springframework.data.domain.Sort;

public enum PostSortOrder {
	LATEST(Sort.Direction.DESC),
	OLDEST(Sort.Direction.ASC);

	private final Sort.Direction direction;

	PostSortOrder(Sort.Direction direction) {
		this.direction = direction;
	}

	public Sort.Direction direction() {
		return direction;
	}
}
