package com.all.rest.beans;

import com.all.shared.mc.TrackStatus;


public class RestTrackStatus implements TrackStatus{

	private String trackId;
	
	private int lastChunk;
	
	private Status status;

	
	@Override
	public String getTrackId(){
		return trackId;
	}
	
	@Override
	public Status getTrackStatus(){
		return status;
	}

	@Override
	public int getLastChunkNumber(){
		return lastChunk;
	}

	public void setTrackId(String trackId) {
		this.trackId = trackId;
	}

	public void setLastChunkNumber(int lastChunk) {
		this.lastChunk = lastChunk;
	}

	public void setTrackStatus(Status status) {
		this.status = status;
	}
}
