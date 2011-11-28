package com.all.downloader.p2p.phexcore.download;

import com.all.downloader.bean.DownloadState;

import phex.download.swarming.SWDownloadConstants;

public class PhexState {

	public static DownloadState convertFrom(int status) {
		switch (status) {
		case SWDownloadConstants.STATUS_FILE_WAITING:
		case SWDownloadConstants.STATUS_FILE_QUEUED:
//			return DownloadState.Searching;

		case SWDownloadConstants.STATUS_FILE_DOWNLOADING:
			return DownloadState.Downloading;

		case SWDownloadConstants.STATUS_FILE_COMPLETED:
		case SWDownloadConstants.STATUS_FILE_COMPLETED_MOVED:
			return DownloadState.Complete;

		case SWDownloadConstants.STATUS_FILE_STOPPED:
			return DownloadState.Paused;

		default:
			return DownloadState.Error;
		}
	}
}
