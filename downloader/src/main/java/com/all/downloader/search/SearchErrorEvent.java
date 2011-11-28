package com.all.downloader.search;

import java.util.EventObject;

public class SearchErrorEvent extends EventObject {

	private static final long serialVersionUID = 9100212628840727109L;

	private final String keyword;

	public SearchErrorEvent(Object source, String keyword) {
		super(source);
		this.keyword = keyword;
	}

	public String getKeyword() {
		return keyword;
	}
}
