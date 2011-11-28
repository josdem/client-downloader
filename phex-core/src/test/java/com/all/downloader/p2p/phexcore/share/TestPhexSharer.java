package com.all.downloader.p2p.phexcore.share;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.helper.ShareHelper;
import com.all.downloader.share.FileSharedEvent;
import com.all.downloader.share.ShareException;
import com.all.downloader.share.SharerListener;
import com.all.shared.download.TrackProvider;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestPhexSharer {

	@UnderTest
	private PhexSharer phexSharer;
	@Mock
	private PhexCore phexCore;
	@Mock
	private TrackProvider trackProvider;
	@Mock
	private ShareHelper shareHelper;
	@Mock
	private AllLink allLink;
	@Mock
	private File file;
	@Mock
	private SharerListener sharerListener;

	private String hashcode = "hashCode";
	private String urnSha = "urnSha";

	@Before
	public void setup() {
		phexSharer.addSharerListener(sharerListener);
		when(phexCore.isSeedingGnutella()).thenReturn(true);
	}

	@Test
	public void shouldCreateUrnShaFromUnsharedFile() throws Exception {
		when(allLink.getHashCode()).thenReturn(hashcode);
		when(trackProvider.getFile(allLink.getHashCode())).thenReturn(file);
		when(file.exists()).thenReturn(true);
		when(allLink.containsUrnSha()).thenReturn(false);
		when(shareHelper.getUrnsha(file)).thenReturn(urnSha);

		phexSharer.share(allLink);

		verify(shareHelper).getUrnsha(file);
		verify(sharerListener).onFileShared(any(FileSharedEvent.class));
	}

	@Test(expected = ShareException.class)
	public void shouldFailOnNullFile() throws Exception {
		phexSharer.share(allLink);
	}

	@Test(expected = ShareException.class)
	public void shouldFailOnMissingFile() throws Exception {
		when(allLink.getHashCode()).thenReturn(hashcode);
		when(trackProvider.getFile(allLink.getHashCode())).thenReturn(file);
		when(file.exists()).thenReturn(false);
		phexSharer.share(allLink);

	}

	@Test
	public void shouldShareFileWithUrnSha() throws Exception {
		when(allLink.getHashCode()).thenReturn(hashcode);
		when(trackProvider.getFile(allLink.getHashCode())).thenReturn(file);
		when(file.exists()).thenReturn(true);
		when(allLink.containsUrnSha()).thenReturn(true);
		when(shareHelper.isURNShared(urnSha)).thenReturn(false);
		when(shareHelper.getUrnsha(file)).thenReturn(urnSha);

		phexSharer.share(allLink);

		verify(shareHelper).getUrnsha(file);
		verify(sharerListener, never()).onFileShared(any(FileSharedEvent.class));
	}

}
