package com.all.downloader.search;

public interface SearcherListener {

	void updateSearchData(SearchDataEvent updateSearchEvent);

	void updateProgress(SearchProgressEvent updateProgressEvent);
	
	void onError(SearchErrorEvent searchErrorEvent);
	
}
