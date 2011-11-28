package com.all.rest.beans;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.all.mc.manager.uploads.UploadStatus;
import com.all.rest.beans.RestUploadStatus;

public class TestRestUploadStatus {

	@Test
	public void shouldImplementUploadStatus() throws Exception {
		String trackId = "trackId";
		UploadStatus status = new RestUploadStatus(trackId);
		assertEquals(trackId, status.getTrackId());
		assertEquals(0, status.getProgress());
		assertEquals(0, status.getUploadRate());
		assertEquals(UploadStatus.UploadState.UPLOADING, status.getState());
	}
}
