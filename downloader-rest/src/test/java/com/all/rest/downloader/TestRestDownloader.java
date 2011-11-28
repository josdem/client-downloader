package com.all.rest.downloader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.messengine.MessEngine;
import com.all.rest.config.RestClientConfig;
import com.all.rest.downloader.RestDownloader;
import com.all.rest.downloader.RestDownloader.RestDownload;
import com.all.rest.downloader.RestDownloader.RestDownloadFactory;
import com.all.shared.download.TrackProvider;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.Track;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestRestDownloader {

	@UnderTest
	private RestDownloader restDownloader;
	@Mock
	private RestDownload download;
	@Mock
	private RestClientConfig config;
	@Mock
	private MessEngine messEngine;

	private String downloadId = "00a9ae41a50cfece357f26e786db6fa014af765b";
	@Mock
	private RestDownloadFactory downloadFactory;
	@Mock
	private TrackProvider trackProvider;
	@Mock
	private Track track;

	@Before
	public void setup() {
		when(trackProvider.getTrack(downloadId)).thenReturn(track);
		when(downloadFactory.createDownload(eq(track), anyBoolean())).thenReturn(download);
		restDownloader.init();
		verify(messEngine).addMessageListener(eq(MessEngineConstants.TRACK_SEEDERS_RESPONSE_TYPE), eq(restDownloader));
	}

	@Test
	public void shouldGetDownloaderPriority() throws Exception {
		Integer priority = 1;
		when(config.getDownloaderPriority()).thenReturn(priority);
		assertEquals(priority, Integer.valueOf(restDownloader.getDownloaderPriority()));
	}

	@Test
	public void shouldFindSourcesAndThenDownload() throws Exception {
		restDownloader.findSources(downloadId);
		restDownloader.download(downloadId);

		verify(downloadFactory).createDownload(track, false);
		verify(download).start();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldNotFindSourcesIfDownloadInProgress() throws Exception {
		restDownloader.download(downloadId);
		restDownloader.findSources(downloadId);
	}

	@Test
	public void shouldCancelADownload() throws Exception {
		restDownloader.download(downloadId);

		restDownloader.delete(downloadId);

		verify(download).cancel();
	}

	@Test
	public void shouldReturnDownloadStatus() throws Exception {
		restDownloader.download(downloadId);

		restDownloader.getStatus(downloadId);

		verify(download).getStatus();
	}

	@Test
	public void shouldPauseDownload() throws Exception {
		restDownloader.download(downloadId);

		restDownloader.pause(downloadId);

		verify(download).pause();
	}

	@Test
	public void shouldResumeDownload() throws Exception {
		restDownloader.download(downloadId);

		restDownloader.resume(downloadId);

		verify(download).resume();
	}

}
