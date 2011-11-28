package com.all.downloader.download;

public class DownloadException extends Exception {

	private static final long serialVersionUID = -1313607640518637749L;

	public DownloadException(String message) {
		super(message);
	}
	
	public DownloadException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
