package com.all.downloader.search;

public interface Searcher {
	
	void search(String keyword) throws SearchException;

	void addSearcherListener(SearcherListener searcherlistener);

	void removeSearcherListener(SearcherListener searcherlistener);
	
}
