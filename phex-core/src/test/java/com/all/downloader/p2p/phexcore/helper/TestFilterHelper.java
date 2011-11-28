package com.all.downloader.p2p.phexcore.helper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import phex.download.RemoteFile;

import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestFilterHelper {
	private static final long ONE_MEGA = 1000000;

	@UnderTest
	private FilterHelper filterHelper;
	@Mock
	private RemoteFile remoteFile;

	@Before
	public void setupDefaultValues() {
		when(remoteFile.getDisplayName()).thenReturn("U2 - Beautiful Day.mp3");
		when(remoteFile.getFileSize()).thenReturn(ONE_MEGA);
	}

	@Test
	public void shouldFilterByZipKeyword() throws Exception {
		when(remoteFile.getDisplayName()).thenReturn("Bob Marley - Discography.zip");
		assertFalse(filterHelper.isValid(remoteFile));
	}

	@Test
	public void shouldFilterByMovKeyword() throws Exception {
		when(remoteFile.getDisplayName()).thenReturn("Bob Marley - Discography.mov");
		assertFalse(filterHelper.isValid(remoteFile));
	}

	@Test
	public void shouldFilterByTorrentKeyword() throws Exception {
		when(remoteFile.getDisplayName()).thenReturn("Bob Marley - Discography.torrent");
		assertFalse(filterHelper.isValid(remoteFile));
	}

	@Test
	public void shouldFilterByExeKeyword() throws Exception {
		when(remoteFile.getDisplayName()).thenReturn("Bob Marley - Discography.exe");
		assertFalse(filterHelper.isValid(remoteFile));
	}

	@Test
	public void shouldGetAValidFileByFilteredRule() throws Exception {
		when(remoteFile.getDisplayName()).thenReturn("U2 - Beautiful Day.mp3");
		assertTrue(filterHelper.isValid(remoteFile));
	}

	@Test
	public void shouldReturnFalseIfNull() throws Exception {
		assertFalse(filterHelper.isValid(null));
	}

	@Test
	public void shouldFillterInvalidSize() throws Exception {
		long size = 45000L;
		when(remoteFile.getFileSize()).thenReturn(size);
		assertFalse(filterHelper.isValid(remoteFile));
	}

	@Test
	public void shouldFillterValidSize() throws Exception {
		long size = 1000000L;
		when(remoteFile.getFileSize()).thenReturn(size);
		assertTrue(filterHelper.isValid(remoteFile));
	}

	// Actually the most Gnutella fake files are WMA and even because JavaFX
	// player we're not supporting WMA plays at the moment
	@Test
	public void shouldNotIncludeWmaFormat() throws Exception {
		when(remoteFile.getDisplayName()).thenReturn("U2 - Beautiful Day.wma");
		assertFalse(filterHelper.isValid(remoteFile));
	}
}
