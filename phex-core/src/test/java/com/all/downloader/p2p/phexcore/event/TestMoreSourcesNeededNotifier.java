package com.all.downloader.p2p.phexcore.event;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mock;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.downloader.p2p.phexcore.BasePhexTestCase;
import com.all.downloader.p2p.phexcore.download.PhexDownload;
import com.all.downloader.p2p.phexcore.download.PhexDownloader;
import com.all.downloader.p2p.phexcore.event.MoreSourcesNeededNotifier;
import com.all.downloader.p2p.phexcore.event.SearcherSourcesListener;
import com.all.downloader.p2p.phexcore.helper.SearchHelper;


public class TestMoreSourcesNeededNotifier extends BasePhexTestCase{
	@Mock
	private PhexDownloader phexDownloader;
	@Mock
	private SearcherSourcesListener listener;
	@Mock
	private PhexDownload phexDownload;
	@Mock
	private SearchHelper searchHelper;
	
	private MoreSourcesNeededNotifier notifier;

	@Test
	public void shouldSendMoreSourcesNeeded() throws Exception {
		notifier = new MoreSourcesNeededNotifier(phexDownloader, listener, phexDownload, searchHelper);
		
		notifier.run();
		
		verify(searchHelper).removeSearcherSourcesLister(listener);
		verify(phexDownload).setState(DownloadState.MoreSourcesNeeded);
		verify(phexDownloader).notifySearchSourcesResult(isA(SearchSourcesEvent.class));
	}
}
