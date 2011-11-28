package com.all.rest.beans;

import com.all.mc.manager.uploads.UploadStatus;

public class RestUploadStatus implements UploadStatus {

	private final String trackId;

	private UploadState state = UploadState.UPLOADING;

	private int progress;

	private int uploadRate;

	public RestUploadStatus(String trackId) {
		this.trackId = trackId;
	}

	@Override
	public int getProgress() {
		return progress;
	}

	@Override
	public UploadState getState() {
		return state;
	}

	@Override
	public String getTrackId() {
		return trackId;
	}

	@Override
	public int getUploadRate() {
		return uploadRate;
	}

	public void setState(UploadState state) {
		this.state = state;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setUploadRate(int uploadRate) {
		this.uploadRate = uploadRate;
	}

}
