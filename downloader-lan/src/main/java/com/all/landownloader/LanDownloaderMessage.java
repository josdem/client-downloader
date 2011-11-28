package com.all.landownloader;

public class LanDownloaderMessage {

	private String sourceAddress;

	private LanDownloadMessageType type;

	private String downloadId;

	private String body;

	@Deprecated
	public LanDownloaderMessage() {
	}

	public LanDownloaderMessage(String sourceAddress, LanDownloadMessageType type, String downloadId) {
		this.sourceAddress = sourceAddress;
		this.type = type;
		this.downloadId = downloadId;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public void setSourceAddress(String sourceAddress) {
		this.sourceAddress = sourceAddress;
	}

	public LanDownloadMessageType getType() {
		return type;
	}

	public void setType(LanDownloadMessageType type) {
		this.type = type;
	}

	public String getDownloadId() {
		return downloadId;
	}

	public void setDownloadId(String downloadId) {
		this.downloadId = downloadId;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}
