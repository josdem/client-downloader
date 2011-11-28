package com.all.downloader.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mock;

import com.all.downloader.BaseTestCase;

public class TestCommonDownloader extends BaseTestCase {

	TestableCommonDownloader testableCommonDownloader = new TestableCommonDownloader();
	@Mock
	DownloaderListener downloaderListener;

	@Test
	public void shouldAddAndRemoveDownloaderListeners() throws Exception {
		// increasing code coverage by testing trivial code
		assertTrue(testableCommonDownloader.listeners.isEmpty());

		testableCommonDownloader.addDownloaderListener(downloaderListener);
		testableCommonDownloader.addDownloaderListener(downloaderListener);

		assertFalse(testableCommonDownloader.listeners.isEmpty());
		assertEquals(1, testableCommonDownloader.listeners.size());

		testableCommonDownloader.removeDownloaderListener(downloaderListener);

		assertTrue(testableCommonDownloader.listeners.isEmpty());
	}

	class TestableCommonDownloader extends CommonDownloader {
		
		@Override
		public void download(String downloadId) throws DownloadException {
		}
		
		@Override
		public void delete(String downloadId) throws DownloadException {
		}
		
		@Override
		public void pause(String downloadId) throws DownloadException {
		}
		
		@Override
		public void resume(String downloadId) throws DownloadException {
		}
		
	}
}
