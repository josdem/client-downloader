package com.all.rest.mc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.mc.manager.uploads.UploaderListener;
import com.all.messengine.MessEngine;
import com.all.messengine.impl.StubMessEngine;
import com.all.rest.beans.RestTrackStatus;
import com.all.rest.beans.RestUploadStatus;
import com.all.rest.config.RestClientConfig;
import com.all.rest.web.RestService;
import com.all.shared.download.RestUploadRequest;
import com.all.shared.download.TrackProvider;
import com.all.shared.mc.TrackStatus.Status;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;
import com.all.shared.model.RemoteTrack;
import com.all.testing.MockInyectRunner;
import com.all.testing.Stub;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestRestMediaManager {

	@UnderTest
	private RestMediaManager restUploader;
	@Mock
	private RestService restService;
	@Mock
	private TrackProvider trackProvider;
	@Stub
	private MessEngine messEngine = new StubMessEngine();
	@Mock
	private RestClientConfig restConfig;
	@Stub
	private RemoteTrack track = new RemoteTrack();

	private String trackId = "12345678";

	private String requester = "leecher@all.com";

	private RestUploadRequest request = new RestUploadRequest(requester, trackId);

	private File file = new File("src/test/resources/requestedFile.mp3");

	private RestTrackStatus trackStatus;

	@Mock
	private UploaderListener listener;

	@Before
	public void setup() {
		when(restConfig.getUserId()).thenReturn("user@all.com");
		restUploader.init();

		assertTrue(((StubMessEngine) messEngine).getRegisteredTypes().contains(
				MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE));

		trackStatus = new RestTrackStatus();
		trackStatus.setTrackStatus(Status.NOT_AVAILABLE);
		trackStatus.setTrackId(trackId);

		when(restService.getStatus(trackId)).thenReturn(trackStatus);

		when(trackProvider.getTrack(trackId)).thenReturn(track);
		when(trackProvider.getFile(trackId)).thenReturn(file);
		assertTrue(file.exists());
		track.setHashcode(trackId);
	}

	@Test
	public void shouldUploadTrackWhenRequestReceived() throws Exception {
		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		while (restUploader.isUploading(trackId)) {
			Thread.sleep(100);
		}
		verify(restService, times(100)).uploadChunk(eq(trackId), anyInt(), any(byte[].class));
		verify(restService, timeout(1000)).uploadMetadata(track);
	}

	@Test
	public void shouldCalculateInitialUploadRateOnlyOnce() throws Exception {
		int uploadRate = restUploader.getUploadRate();
		assertEquals(uploadRate, restUploader.getUploadRate());
		assertEquals(uploadRate, restUploader.getUploadRate());
		assertEquals(uploadRate, restUploader.getUploadRate());

		verify(restService).getUploadRate();
	}

	@Test
	public void shouldReturnZertoIfCannotCalculateUploadRate() throws Exception {
		doThrow(new RuntimeException("Server may be down")).when(restService).getUploadRate();

		assertEquals(0, restUploader.getUploadRate());
	}

	@Test
	public void shouldNotUploadTwice() throws Exception {
		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		while (restUploader.isUploading(trackId)) {
			Thread.sleep(100);
		}
		verify(restService, times(100)).uploadChunk(eq(trackId), anyInt(), any(byte[].class));
	}

	@Test
	public void shouldStartUploadFromRequestedChunk() throws Exception {
		trackStatus.setTrackStatus(Status.INCOMPLETE);
		trackStatus.setLastChunkNumber(50);
		when(restService.getStatus(trackId)).thenReturn(trackStatus);

		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		while (restUploader.isUploading(trackId)) {
			Thread.sleep(100);
		}
		verify(restService, times(50)).uploadChunk(eq(trackId), anyInt(), any(byte[].class));
	}

	@Test
	public void shouldNotUploadTrackIfAlreadyInServer() throws Exception {
		trackStatus.setTrackStatus(Status.UPLOADED);
		trackStatus.setLastChunkNumber(100);
		when(restService.getStatus(trackId)).thenReturn(trackStatus);
		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		Thread.sleep(100);
		verify(restService, never()).uploadChunk(eq(trackId), anyInt(), any(byte[].class));
		verify(restService, never()).uploadMetadata(track);
	}

	@Test
	public void shouldAddAndRemoveUploaderListeners() throws Exception {
		restUploader.addUploaderListener(listener);
		restUploader.upload(trackId);
		verify(listener, timeout(1000).times(101)).onUploadUpdated(isA(RestUploadStatus.class));
		while (restUploader.isUploading(trackId)) {
			Thread.sleep(100);
		}
		reset(listener);
		restUploader.removeUploaderListener(listener);
		restUploader.upload(trackId);
		while (restUploader.isUploading(trackId)) {
			Thread.sleep(100);
		}
		verify(listener, never()).onUploadUpdated(isA(RestUploadStatus.class));

	}

	@Test
	public void shouldCancelUploadsWhenShuttingDown() throws Exception {
		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		restUploader.shutdown();
		Thread.sleep(1000);
		verify(restService, never()).uploadMetadata(track);
	}

	@Test
	public void shouldHandleAnUnexpectedExceptionDuringDownloadAndCancelIt() throws Exception {
		doThrow(new RuntimeException("Some server error")).when(restService).uploadChunk(eq(trackId), anyInt(),
				any(byte[].class));
		messEngine.send(new AllMessage<RestUploadRequest>(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE, request));
		Thread.sleep(1000);
		verify(restService).uploadChunk(eq(trackId), anyInt(), any(byte[].class));
		assertFalse(restUploader.isUploading(trackId));
		verify(restService, never()).uploadMetadata(track);
	}

	@Test
	public void shouldUploadTrackMetadataWhenUploadingTrack() throws Exception {
		restUploader.upload(trackId);
		verify(restService, timeout(1000)).uploadMetadata(track);
	}
}
