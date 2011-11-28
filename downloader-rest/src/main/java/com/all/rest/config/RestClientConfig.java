package com.all.rest.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.downloader.download.ManagedDownloaderConfig;

@Service
public class RestClientConfig {

	public static final String TIMEOUT_KEY = "timeout.rest";

	public static final String PRIORITY_KEY = "priority.rest";

	public static final String CHUNK_AWAIT_MIN_DELAY = "turnChunkAwaitMinDelay";

	public static final String CHUNK_AWAIT_TIMEOUT = "turnChunkAwaitTimeout";

	public static final String TURN_INITIAL_SHARE_DELAY = "turnInitialShareDelay";

	public static final String TURN_SHARE_DELAY = "turnShareDelay";

	public static final String TURN_DOWNLOAD_INIT_DELAY = "turnDownloadInitDelay";

	@Autowired
	private ManagedDownloaderConfig downloaderConfig;

	public String getUserId() {
		return downloaderConfig.getUserId();
	}

	public String getIncompleteDownloadsPath() {
		return downloaderConfig.getIncompleteDownloadsPath();
	}

	public String getCompleteDownloadsPath() {
		return downloaderConfig.getCompleteDownloadsPath();
	}

	public String getProperty(String prop){
		return downloaderConfig.getProperty(prop);
	}

	public Long getChunkAwaitTimeout(TimeUnit unit) {
		return toTimeUnit(Long.valueOf(downloaderConfig.getProperty(CHUNK_AWAIT_TIMEOUT)), unit);
	}

	public Long getMinChunkAwaitDelay(TimeUnit unit) {
		return toTimeUnit(Long.valueOf(downloaderConfig.getProperty(CHUNK_AWAIT_MIN_DELAY)), unit);
	}

	public Long getInitialShareDelay(TimeUnit unit) {
		return toTimeUnit(Long.valueOf(downloaderConfig.getProperty(TURN_INITIAL_SHARE_DELAY)), unit);
	}

	public Long getShareDelay(TimeUnit unit) {
		return toTimeUnit(Long.valueOf(downloaderConfig.getProperty(TURN_SHARE_DELAY)), unit);
	}

	public Long getInitDownloadDelay(TimeUnit unit) {
		return toTimeUnit(Long.valueOf(downloaderConfig.getProperty(TURN_DOWNLOAD_INIT_DELAY)), unit);
	}

	private Long toTimeUnit(Long value, TimeUnit unit) {
		switch (unit) {
		case SECONDS:
			return value;
		case MILLISECONDS:
			return TimeUnit.SECONDS.toMillis(value);
		default:
			throw new IllegalArgumentException("Invalid time unit. Valid time units are: SECONDS, MILLISECONDS");
		}
	}

	public int getDownloaderPriority() {
		return downloaderConfig.getDownloaderPriority(PRIORITY_KEY);
	}

	public long getDownloaderSearchTimeout() {
		return downloaderConfig.getDownloaderSearchTimeout(TIMEOUT_KEY);
	}

}
