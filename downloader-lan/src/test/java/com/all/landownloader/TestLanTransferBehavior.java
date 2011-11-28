package com.all.landownloader;

import static com.all.landownloader.LanDownloadMessageType.CANCEL_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.CHUNK_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.PAUSE_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.RESUME_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.START_TRANSFER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.all.shared.download.TrackProvider;
import com.all.shared.model.Track;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestLanTransferBehavior {
	@UnderTest
	private LanDownloader lanDownloader;
	@Mock
	private TrackProvider trackProvider;
	@Mock
	private LanNetworkingService networkingService;
	@Mock
	private Track track;

	private String leecherAddr = "192.168.1.xxl";
	private String downloadId = "downloadId";
	private File requestedFile = new File("src/test/resources/requestedFile.mp3");
	private int totalBytes;

	@Before
	public void setup() throws IOException {
		assertTrue(requestedFile.exists());
		FileInputStream fis = new FileInputStream(requestedFile);
		totalBytes = fis.available();
		when(trackProvider.getTrack(downloadId)).thenReturn(track);
		when(track.getHashcode()).thenReturn(downloadId);
		when(trackProvider.getFile(downloadId)).thenReturn(requestedFile);
	}

	@Test
	public void shouldTransferAFile_HappyPath() throws Exception {
		LanDownloaderMessage startTransferRequest = new LanDownloaderMessage(leecherAddr, START_TRANSFER, downloadId);
		ArgumentCaptor<LanDownloaderMessage> messageCaptor = ArgumentCaptor.forClass(LanDownloaderMessage.class);

		lanDownloader.onMessage(startTransferRequest);
		while (lanDownloader.isTransferring()) {
			Thread.sleep(100);
		}

		verify(networkingService, times(100)).sendTo(messageCaptor.capture(), eq(leecherAddr));
		List<LanDownloaderMessage> chunks = messageCaptor.getAllValues();
		int actualBytes = 0;
		for (LanDownloaderMessage chunkMessage : chunks) {
			assertEquals(CHUNK_TRANSFER, chunkMessage.getType());
			actualBytes += Base64.decode(chunkMessage.getBody().getBytes()).length;
		}
		assertEquals(totalBytes, actualBytes);
	}

	@Test(timeout = 15000)
	public void shouldPauseAndResumeATransfer() throws Exception {
		LanDownloaderMessage startTransferRequest = new LanDownloaderMessage(leecherAddr, START_TRANSFER, downloadId);
		LanDownloaderMessage pauseTransferRequest = new LanDownloaderMessage(leecherAddr, PAUSE_TRANSFER, downloadId);
		LanDownloaderMessage resumeTransferRequest = new LanDownloaderMessage(leecherAddr, RESUME_TRANSFER, downloadId);
		ArgumentCaptor<LanDownloaderMessage> messageCaptor = ArgumentCaptor.forClass(LanDownloaderMessage.class);

		lanDownloader.onMessage(startTransferRequest);
		lanDownloader.onMessage(pauseTransferRequest);
		int pauseTime = 3000;
		int timer = 0;
		assertTrue(lanDownloader.isTransferring());
		while (lanDownloader.isTransferring() && timer < pauseTime) {
			Thread.sleep(200);
			timer += 200;
		}
		assertTrue(lanDownloader.isTransferring());
		lanDownloader.onMessage(resumeTransferRequest);
		while (lanDownloader.isTransferring()) {
			Thread.sleep(100);
		}
		verify(networkingService, times(100)).sendTo(messageCaptor.capture(), eq(leecherAddr));
		List<LanDownloaderMessage> chunks = messageCaptor.getAllValues();
		int actualBytes = 0;
		for (LanDownloaderMessage chunkMessage : chunks) {
			assertEquals(CHUNK_TRANSFER, chunkMessage.getType());
			actualBytes += Base64.decode(chunkMessage.getBody().getBytes()).length;
		}
		assertEquals(totalBytes, actualBytes);

	}

	@Test(timeout = 1000)
	public void shouldCancelATransfer() throws Exception {
		LanDownloaderMessage startTransferRequest = new LanDownloaderMessage(leecherAddr, START_TRANSFER, downloadId);
		LanDownloaderMessage cancelTransferRequest = new LanDownloaderMessage(leecherAddr, CANCEL_TRANSFER, downloadId);
		ArgumentCaptor<LanDownloaderMessage> messageCaptor = ArgumentCaptor.forClass(LanDownloaderMessage.class);

		lanDownloader.onMessage(startTransferRequest);
		lanDownloader.onMessage(cancelTransferRequest);
		while (lanDownloader.isTransferring()) {
			Thread.sleep(50);
		}
		verify(networkingService, atMost(80)).sendTo(messageCaptor.capture(), eq(leecherAddr));
	}
}
