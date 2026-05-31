package com.studywithme.post.application;

public enum PostSearchScope {
	ALL(true, true, true),
	TITLE(true, false, false),
	TITLE_CONTENT(true, true, false),
	AUTHOR(false, false, true);

	private final boolean titleIncluded;
	private final boolean contentIncluded;
	private final boolean authorIncluded;

	PostSearchScope(boolean titleIncluded, boolean contentIncluded, boolean authorIncluded) {
		this.titleIncluded = titleIncluded;
		this.contentIncluded = contentIncluded;
		this.authorIncluded = authorIncluded;
	}

	public boolean includesTitle() {
		return titleIncluded;
	}

	public boolean includesContent() {
		return contentIncluded;
	}

	public boolean includesAuthor() {
		return authorIncluded;
	}
}
