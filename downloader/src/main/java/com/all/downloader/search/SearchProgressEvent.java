package com.all.downloader.search;

import java.util.EventObject;

public class SearchProgressEvent extends EventObject {
	private static final long serialVersionUID = 4734154282751016500L;
	private final String keywordSearch;
	private final int progress;

	public SearchProgressEvent(Object source, String keywordSearch, int progress) {
		super(source);
		this.keywordSearch = keywordSearch;
		this.progress = progress;
	}

	public String getKeywordSearch() {
		return keywordSearch;
	}
	
	public int getProgress() {
		return progress;
	}
}
