package com.all.downloader.p2p.phexcore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestPhexCoreImpl {
	static final String PRIORITY_GNUTELLA = "priority.gnutella";
	static final String TIMEOUT_GNUTELLA = "timeout.gnutella";

	@UnderTest
	private PhexCoreImpl phexCore;
	@Mock
	private ManagedDownloaderConfig downloaderConfig;

	@SuppressWarnings("static-access")
	@Test
	public void shouldVerifyPropertiesPriorityName() throws Exception {
		assertEquals(PRIORITY_GNUTELLA, phexCore.PRIORITY_GNUTELLA);
		assertEquals(TIMEOUT_GNUTELLA, phexCore.TIMEOUT_GNUTELLA);
	}

	@Test
	public void shouldGetDownloaderPriority() throws Exception {
		int expectedPriority = 2;
		when(downloaderConfig.getDownloaderPriority(PRIORITY_GNUTELLA)).thenReturn(expectedPriority);
		assertEquals(expectedPriority, phexCore.getDownloaderPriority());
	}

	@Test
	public void shouldGetTimeOut() throws Exception {
		int expectedTimeout = 10;
		when(downloaderConfig.getDownloaderSearchTimeout(TIMEOUT_GNUTELLA)).thenReturn(expectedTimeout);
		assertEquals(expectedTimeout, phexCore.getDownloaderTimeout());
	}
}
