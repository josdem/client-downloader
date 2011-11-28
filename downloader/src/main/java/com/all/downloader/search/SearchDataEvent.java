package com.all.downloader.search;

import java.util.EventObject;

public class SearchDataEvent extends EventObject {
	private static final long serialVersionUID = -5364996806945219763L;
	private final String keywordSearch;
	private final SearchData searchData;

	public SearchDataEvent(Object source, String keywordSearch, SearchData searchData) {
		super(source);
		this.keywordSearch = keywordSearch;
		this.searchData = searchData;
	}

	public String getKeywordSearch() {
		return keywordSearch;
	}

	public SearchData getSearchData() {
		return searchData;
	}

}
