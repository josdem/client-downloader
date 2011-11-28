package com.all.downloader.p2p.phexcore.download;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import phex.common.TransferDataProvider;
import phex.download.swarming.SWDownloadFile;
import phex.gui.comparator.ETAComparator;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;

public class PhexDownload implements DownloadStatus {
	static final int PROGRESS_FOR_UNKOWN_SIZE = 0;
	static final int UNKOWN_SIZE = -1; // so phex will not fail

	private String downloadId;
	private DownloadState state = DownloadState.Downloading;
	private int progress;
	private int freeNodes;
	private int busyNodes;
	private String urnSha;
	private SWDownloadFile downloadFile;
	private String fileName;
	private long size = UNKOWN_SIZE;
	private String fileExtension;
	private long downloadRate;
	private int timeRemaining;
	private AllLink allLink;
	ETAComparator eta = new ETAComparator();

	private long startTime;

	public PhexDownload() {
	}

	public PhexDownload(String allLinkString) {
		allLink = AllLink.parse(allLinkString);
		this.urnSha = allLink.getUrnSha();
	}

	public void setFileExtension(String fileExtension) {
		if (!fileExtension.startsWith(".")) {
			fileExtension = "." + fileExtension;
		}
		this.fileExtension = fileExtension;
	}

	public void setState(DownloadState state) {
		this.state = state;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public SWDownloadFile getDownloadFile() {
		return downloadFile;
	}

	public String getHashcode() {
		return downloadId;
	}

	public void setDownloadFile(SWDownloadFile downloadFile) {
		this.downloadFile = downloadFile;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size == 0 ? UNKOWN_SIZE : size;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String trackName) {
		this.fileName = trackName;
	}

	public String getFileHashcode() {
		return urnSha;
	}

	public File getFile() {
		return downloadFile != null ? downloadFile.getDestinationFile() : null;
	}

	@Override
	public long getDownloadRate() {
		return downloadRate;
	}

	@Override
	public int getProgress() {
		return progress;
	}

	@Override
	public int getRemainingSeconds() {
		return timeRemaining;
	}

	@Override
	public DownloadState getState() {
		return state;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setDownloadId(String downloadId) {
		this.downloadId = downloadId;
	}

	public String getFileNameAndExtension() {
		return this.fileName + this.fileExtension;
	}

	public void updateStatus() {
		if (downloadFile != null) {
			int goodCandidates = downloadFile.getGoodCandidateCount();
			int badCandidates = downloadFile.getBadCandidateCount();
			int progress = downloadFile.getProgress();
			this.downloadRate = downloadFile.getTransferSpeed();

			progress = verifyDownloadProgress(progress);

			this.progress = progress == UNKOWN_SIZE ? PROGRESS_FOR_UNKOWN_SIZE : progress;
			this.state = PhexState.convertFrom(downloadFile.getStatus());
			this.freeNodes = goodCandidates;
			this.busyNodes = badCandidates;
			int calcTimeRemaining = (int) eta.calcTimeRemaining(downloadFile);
			if (calcTimeRemaining == TransferDataProvider.INFINITY_ETA_INT) {
				this.timeRemaining = 0;
			} else {
				this.timeRemaining = calcTimeRemaining;
			}
		}
	}

	int verifyDownloadProgress(int progress) {
		// Related to Bug 4477. Avoid confusion sometimes you can see 100% but
		// is still downloading
		if (progress == 100 && downloadFile.getTransferSpeed() != 0) {
			progress = 98;
		}
		return progress;
	}

	public String getAllLinkAsString() {
		return allLink.toString();
	}

	public void setAllLink(String allLinkString) {
		if (!StringUtils.isEmpty(allLinkString)) {
			allLink = AllLink.parse(allLinkString);
			this.urnSha = allLink.getUrnSha();
			this.downloadId = allLink.getHashCode();
		}
	}

	@Override
	public String getDownloadId() {
		return downloadId;
	}

	@Override
	public int getFreeNodes() {
		return freeNodes;
	}

	@Override
	public int getBusyNodes() {
		return busyNodes;
	}

	public void setStartTimeStamp() {
		this.startTime = System.currentTimeMillis();
	}

	public long getStartTimeStamp() {
		return startTime;
	}

	public File getFile(String parentPath) {
		return new File(parentPath, getFileNameAndExtension());
	}
}
