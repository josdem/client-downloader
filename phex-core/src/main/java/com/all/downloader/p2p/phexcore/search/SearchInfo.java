package com.all.downloader.p2p.phexcore.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.annotation.EventTopicSubscriber;

import phex.download.RemoteFile;
import phex.event.PhexEventTopics;
import phex.query.Search;
import phex.query.SearchDataEvent;

import com.all.downloader.p2p.phexcore.helper.FilterHelper;
import com.all.downloader.search.SearchData;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;

public class SearchInfo {
	Log log = LogFactory.getLog(SearchInfo.class);
	List<RemoteFile> remoteFileList = new ArrayList<RemoteFile>();
	Search search;
	String keyword;
	FilterHelper filterHelper;
	Set<SearcherListener> searcherSourcesListeners;

	public SearchInfo() {
	}
	
	public SearchInfo(Search search, String keyword, Set<SearcherListener> searcherSourcesListeners) {
		this.search = search;
		this.keyword = keyword;
		this.searcherSourcesListeners = searcherSourcesListeners;

		this.filterHelper = createFilterHelper();
		this.remoteFileList = new ArrayList<RemoteFile>();
	}

	FilterHelper createFilterHelper() {
		return new FilterHelper();
	}

	@EventTopicSubscriber(topic = PhexEventTopics.Search_Data)
	public void onSearchDataEvent(String topic, SearchDataEvent event) {
		if (isNotOurSearchData(event) || hasNotSearchDataResults(event)) {
			return;
		}

		for (RemoteFile remoteFile : event.getSearchData()) {
			if (filterHelper.isValid(remoteFile)) {
				remoteFileList.add(remoteFile);

				SearchData searchData = createSearchData(remoteFile);

				// TODO set the source as the PhexManager and not this class
				com.all.downloader.search.SearchDataEvent updateSearchEvent = new com.all.downloader.search.SearchDataEvent(
						this, keyword, searchData);
				for (SearcherListener listener : searcherSourcesListeners) {
					listener.updateSearchData(updateSearchEvent);
				}
			}
		}
	}

	SearchData createSearchData(RemoteFile remoteFile) {
		return PhexSearchData.createFrom(remoteFile);
	}

	private boolean isNotOurSearchData(SearchDataEvent event) {
		return search != event.getSource();
	}

	private boolean hasNotSearchDataResults(SearchDataEvent event) {
		return event.getSearchData() == null;
	}

	public String getKeyword() {
		return keyword;
	}

	public void notifyProgress() {
		SearchProgressEvent event = new SearchProgressEvent(this, keyword, search.getProgress());
		for (SearcherListener listener : searcherSourcesListeners) {
			listener.updateProgress(event);
		}
	}

	public void stopSearch() {
		search.stopSearching();
	}

	public List<RemoteFile> getRemoteFileList() {
		//Since Phex started a search, any result is stored at remoteFileList
		return new ArrayList<RemoteFile>(remoteFileList);
	}
	
	public boolean isFinished() {
		return search.isSearchFinished();
	}
	
	public void addRemoteFile(RemoteFile remoteFile) {
		if(remoteFile != null) {
			remoteFileList.add(remoteFile);
		}
	}
}
