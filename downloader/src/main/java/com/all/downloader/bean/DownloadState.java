package com.all.downloader.bean;

public enum DownloadState {
	Queued("downloadState.queued"), 
	Searching("downloadState.sarching"), 
	ReadyToDownload("downloadState.ready"), 
	MoreSourcesNeeded("downloadState.moreResources"), 
	Downloading("downloadState.downloading"), 
	Paused("downloadState.paused"), 
	Complete("downloadState.shared"), 
	Error("downloadState.error"), 
	Canceled("downloadState.canceled"), 
	YoutubeTranscoding("downloadState.youtube.transcoding"), 
	YoutubeDownloading("downloadState.youtube.downloading"), 
	YoutubeComplete("downloadState.youtube.complete"), 
	YoutubeWaiting("downloadState.youtube.waiting"), 
	YoutubePaused("downloadState.youtube.paused"), 
	YoutubeError("downloadState.youtube.error"), 
	YoutubeFNF("downloadState.youtube.fnf");

	private final String key;

	DownloadState(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
