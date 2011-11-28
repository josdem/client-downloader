package com.all.landownloader;

import static com.all.landownloader.LanDownloadMessageType.CANCEL_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.CHUNK_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.PAUSE_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.RESUME_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.START_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.TRACK_REQUEST;
import static com.all.landownloader.LanDownloadMessageType.TRACK_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.landownloader.LanDownloader.LanDownload;
import com.all.landownloader.LanDownloader.LanDownloadFactory;
import com.all.landownloader.LanDownloader.LanTransfer;
import com.all.landownloader.LanDownloader.LanTransferFactory;
import com.all.shared.download.TrackProvider;
import com.all.shared.model.Track;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestLanDownloader {

	@UnderTest
	private LanDownloader lanDownloader;
	@Mock
	private ManagedDownloaderConfig config;
	@Mock
	private TrackProvider trackProvider;
	@Mock
	private LanDownloadFactory downloadFactory;
	@Mock
	private LanTransferFactory transferFactory;
	@Mock
	private LanNetworkingService networkingService;

	// Aux mocks
	@Mock
	private LanDownload download;
	@Mock
	private LanTransfer transfer;
	@Mock
	private Track track;
	@Mock
	private File trackFile;

	private String downloadId = "00a9ae41a50cfece357f26e786db6fa014af765b";

	private String email = "seeder@all.com";

	private String address = "192.168.1.27";

	@Before
	public void setup() {
		List<String> hashcodes = new ArrayList<String>();
		hashcodes.add("1234567890");
		hashcodes.add("0987654321");

		when(config.getUserId()).thenReturn(email);

		lanDownloader.init();

		when(trackProvider.getTrack(downloadId)).thenReturn(track);
		when(trackProvider.getFile(downloadId)).thenReturn(trackFile);
		when(track.getHashcode()).thenReturn(downloadId);
		when(downloadFactory.createDownload(eq(track), anyBoolean())).thenReturn(download);
		when(transferFactory.createTransfer(eq(downloadId), eq(trackFile), anyString())).thenReturn(transfer);
	}
	
	@After
	public void shudown(){
		lanDownloader.shutdown();
	}

	@Test
	public void shouldCancelADownload() throws Exception {
		lanDownloader.download(downloadId);

		lanDownloader.delete(downloadId);

		verify(download).cancel();
	}

	@Test
	public void shouldReturnDownloadStatus() throws Exception {

		assertNull(lanDownloader.getStatus(downloadId));

		lanDownloader.download(downloadId);

		lanDownloader.getStatus(downloadId);

		verify(download).getStatus();
	}

	@Test
	public void shouldPauseDownload() throws Exception {
		lanDownloader.download(downloadId);

		lanDownloader.pause(downloadId);

		verify(download).pause();
	}

	@Test
	public void shouldResumeDownload() throws Exception {
		lanDownloader.download(downloadId);

		lanDownloader.resume(downloadId);

		verify(download).resume();
	}

	@Test
	public void shouldAddSeederToCurrentDownloadOnTrackResponse() throws Exception {
		lanDownloader.download(downloadId);
		LanDownloaderMessage request = new LanDownloaderMessage(address, TRACK_RESPONSE, downloadId);
		request.setBody(Boolean.toString(true));

		lanDownloader.onMessage(request);

		verify(download).addSeederResponse(address, true);
	}

	@Test
	public void shouldAddChunkToCurrentDownloadOnChunkTransferred() throws Exception {
		lanDownloader.download(downloadId);
		LanDownloaderMessage request = new LanDownloaderMessage(address, CHUNK_TRANSFER, downloadId);
		String chunk = "encodedChunk";
		request.setBody(chunk);

		lanDownloader.onMessage(request);

		verify(download).addChunk(chunk);
	}

	@Test
	public void shouldStartTransferAsLeecherRequest() throws Exception {
		startTransfer();

		verify(transferFactory).createTransfer(downloadId, trackFile, address);
	}

	private void startTransfer() {
		LanDownloaderMessage request = new LanDownloaderMessage(address, START_TRANSFER, downloadId);

		lanDownloader.onMessage(request);
	}

	@Test
	public void shouldPauseCurrentTransferOnRequest() throws Exception {
		startTransfer();
		LanDownloaderMessage request = new LanDownloaderMessage(address, PAUSE_TRANSFER, downloadId);

		lanDownloader.onMessage(request);

		verify(transfer).pause();
	}

	@Test
	public void shouldResumeCurrentTransferOnRequest() throws Exception {
		startTransfer();
		LanDownloaderMessage request = new LanDownloaderMessage(address, RESUME_TRANSFER, downloadId);

		lanDownloader.onMessage(request);

		verify(transfer).resume();
	}

	@Test
	public void shouldCancelCurrentTransferOnRequest() throws Exception {
		startTransfer();
		LanDownloaderMessage request = new LanDownloaderMessage(address, CANCEL_TRANSFER, downloadId);

		lanDownloader.onMessage(request);

		verify(transfer).cancel();
	}

	@Test
	public void shouldGetDownloaderPriority() throws Exception {
		Integer priority = 0;
		when(config.getDownloaderPriority(LanDownloader.PRIORITY_KEY)).thenReturn(priority);

		assertEquals(priority, Integer.valueOf(lanDownloader.getDownloaderPriority()));
	}

	@Test
	public void shouldFindSourcesAndThenStart() throws Exception {
		lanDownloader.findSources(downloadId);
		lanDownloader.download(downloadId);

		verify(downloadFactory).createDownload(track, false);
		verify(download).start();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldNotFindSourcesIfDownloadInProgress() throws Exception {
		lanDownloader.download(downloadId);
		lanDownloader.findSources(downloadId);
	}

	@Test
	public void shouldRespondToTrackRequestIfHasTrack() throws Exception {
		LanDownloaderMessage request = new LanDownloaderMessage(address, TRACK_REQUEST, downloadId);

		lanDownloader.onMessage(request);

		verify(networkingService).sendTo(any(LanDownloaderMessage.class), eq(address));
	}

	@Test
	public void shouldRespondTrackRequestNegativelyIfResponseIsNotOptional() throws Exception {
		LanDownloaderMessage request = new LanDownloaderMessage(address, TRACK_REQUEST, downloadId);
		request.setBody(Boolean.toString(true));

		lanDownloader.onMessage(request);

		verify(networkingService).sendTo(any(LanDownloaderMessage.class), eq(address));
	}

}
