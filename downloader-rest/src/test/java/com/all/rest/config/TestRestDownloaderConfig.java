package com.all.rest.config;

import static com.all.rest.config.RestClientConfig.CHUNK_AWAIT_MIN_DELAY;
import static com.all.rest.config.RestClientConfig.CHUNK_AWAIT_TIMEOUT;
import static com.all.rest.config.RestClientConfig.PRIORITY_KEY;
import static com.all.rest.config.RestClientConfig.TIMEOUT_KEY;
import static com.all.rest.config.RestClientConfig.TURN_DOWNLOAD_INIT_DELAY;
import static com.all.rest.config.RestClientConfig.TURN_INITIAL_SHARE_DELAY;
import static com.all.rest.config.RestClientConfig.TURN_SHARE_DELAY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestRestDownloaderConfig {
	@UnderTest
	private RestClientConfig restConfig;

	@Mock
	private ManagedDownloaderConfig downloaderConfig;

	// time settings have seconds as default time unit
	private String initDownloadDelay = "15";
	private String shareDelay = "30";
	private String initShareDelay = "15";
	private String chunkAwaitTimeout = "60";
	private String chunkAwaitDelay = "2";
	private int restPriority = 1;
	private int restTimeout = 10;

	@Before
	public void setup() throws Exception {
		when(downloaderConfig.getProperty(TURN_INITIAL_SHARE_DELAY)).thenReturn(initShareDelay);
		when(downloaderConfig.getProperty(TURN_SHARE_DELAY)).thenReturn(shareDelay);
		when(downloaderConfig.getProperty(TURN_DOWNLOAD_INIT_DELAY)).thenReturn(initDownloadDelay);
		when(downloaderConfig.getProperty(CHUNK_AWAIT_MIN_DELAY)).thenReturn(chunkAwaitDelay);
		when(downloaderConfig.getProperty(CHUNK_AWAIT_TIMEOUT)).thenReturn(chunkAwaitTimeout);
		when(downloaderConfig.getDownloaderPriority(PRIORITY_KEY)).thenReturn(restPriority);
		when(downloaderConfig.getDownloaderSearchTimeout(TIMEOUT_KEY)).thenReturn(restTimeout);
	}

	@Test
	public void shouldGetTurnDownloaderSettingsFromPropsFile() throws Exception {
		assertEquals(Integer.valueOf(restPriority), Integer.valueOf(restConfig.getDownloaderPriority()));
	}

	@Test
	public void shouldGetTimeSettingsInTheExpectedTimeUnit() throws Exception {
		assertEquals(new Long(Long.valueOf(initDownloadDelay) * 1000L), restConfig
				.getInitDownloadDelay(TimeUnit.MILLISECONDS));
		assertEquals(Long.valueOf(initShareDelay), restConfig.getInitialShareDelay(TimeUnit.SECONDS));
		assertEquals(Long.valueOf(shareDelay), restConfig.getShareDelay(TimeUnit.SECONDS));
		assertEquals(new Long(Long.valueOf(chunkAwaitDelay) * 1000), restConfig
				.getMinChunkAwaitDelay(TimeUnit.MILLISECONDS));
		assertEquals(new Long(Long.valueOf(chunkAwaitTimeout) * 1000), restConfig
				.getChunkAwaitTimeout(TimeUnit.MILLISECONDS));

		assertEquals(restTimeout, downloaderConfig.getDownloaderSearchTimeout(TIMEOUT_KEY));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailWhenGettingATimeSettingInAnInvalidTimeUnit() throws Exception {
		restConfig.getChunkAwaitTimeout(TimeUnit.DAYS);
	}

	@Test
	public void shouldForwardOtherRequestsToPeerProviderService() throws Exception {

		restConfig.getUserId();
		restConfig.getIncompleteDownloadsPath();
		restConfig.getCompleteDownloadsPath();

		verify(downloaderConfig).getUserId();
		verify(downloaderConfig).getIncompleteDownloadsPath();
		verify(downloaderConfig).getCompleteDownloadsPath();
	}

}
