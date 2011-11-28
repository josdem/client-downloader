package com.all.downloader.p2p.phexcore.helper;

import java.io.File;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import phex.common.URN;
import phex.prefs.api.Setting;
import phex.prefs.core.LibraryPrefs;
import phex.share.ShareFile;
import phex.share.SharedFilesService;
import phex.share.UrnGenerator;
import phex.utils.DirectoryOnlyFileFilter;

import com.all.core.common.spring.InitializeService;
import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.downloader.p2p.phexcore.PhexCoreImpl;

@Component
public class ShareHelper {
	private Log log = LogFactory.getLog(ShareHelper.class);

	private final FileHelper fileHelper = new FileHelper();
	@Autowired
	private PhexCoreImpl phexCore;
	@Autowired
	private ManagedDownloaderConfig downloaderConfig;

	private UrnGenerator urnGenerator;

	@InitializeService
	public void initialize() {
		urnGenerator = new UrnGenerator(phexCore.getSharedFilesService());
	}

	@InitializeService
	public void initializeService() {
		shareDirRecursive(new File(this.downloaderConfig.getProperty("savePath")));
	}

	public String getUrnsha(File fileToSeed) {
		SharedFilesService shareFileService = phexCore.getSharedFilesService();
		ShareFile existingShareFile = shareFileService.getFileByName(fileToSeed.getAbsolutePath());
		if (existingShareFile != null) {
			String urnsha1 = existingShareFile.getSha1();
			log.debug("Phex already generated urnsha for track: " + fileToSeed + " and is: " + urnsha1);
			return urnsha1;
		}

		String urnSha = null;
		ShareFile shareFile = createShareFile(fileToSeed);
		if (urnGenerator.generate(shareFile)) {
			// Add the directory of this file to the SharedDirectories list
			addFileDirectory(shareFile);
			if (shareFile.getURN() == null) {
				log.error("Its important that the file owns a valid urn when being added");
			} else {
				shareFileService.addSharedFile(shareFile);
				urnSha = shareFile.getSha1();
			}
		}
		log.debug("Sharing: " + fileToSeed + " with URN = " + urnSha);
		return urnSha;
	}

	public boolean isURNShared(String urnSha) {
		SharedFilesService shareFileService = phexCore.getSharedFilesService();
		try {
			URN fileURN = new URN(urnSha);
			return shareFileService.isURNShared(fileURN);
		} catch (IllegalArgumentException iae) {
			return false;
		}
	}

	private ShareFile createShareFile(File fileToSeed) {
		return fileHelper.createShareFile(fileToSeed);
	}

	void addFileDirectory(ShareFile shareFile) {
		File systemFile = shareFile.getSystemFile();
		String directoryName = systemFile.getParent();

		Setting<Set<String>> sharedDirectories = getSharedDirectories();
		Set<String> sharedDirs = sharedDirectories.get();

		if (sharedDirs.contains(directoryName)) {
			log.debug("The directory for file: " + shareFile.getFileName() + " is already set on : " + directoryName);
			return;
		}

		File directory = createDirectoryToShare(directoryName);
		boolean success = shareDirRecursive(directory);
		if (success) {
			LibraryPrefs.save(true);
		}
	}

	private File createDirectoryToShare(String directoryName) {
		return fileHelper.createDirectoryToShare(directoryName);
	}

	public boolean shareDirRecursive(File file) {
		boolean success = true;
		if (!file.isDirectory()) {
			log.debug(file + " to share is not directory");
			return false;
		}
		Setting<Set<String>> sharedDirectories = getSharedDirectories();
		sharedDirectories.get().add(file.getAbsolutePath());
		sharedDirectories.changed();
		File[] dirs = file.listFiles(new DirectoryOnlyFileFilter());
		if (dirs == null) {// not a valid path or an IO error.
			return false;
		}
		for (int i = 0; i < dirs.length; i++) {
			shareDirRecursive(dirs[i]);
		}
		return success;
	}

	private Setting<Set<String>> getSharedDirectories() {
		return fileHelper.getSharedDirectories();
	}

	class FileHelper {

		public Setting<Set<String>> getSharedDirectories() {
			return LibraryPrefs.SharedDirectoriesSet;
		}

		public File createDirectoryToShare(String directoryName) {
			return new File(directoryName);
		}

		public ShareFile createShareFile(File fileToSeed) {
			return new ShareFile(fileToSeed);
		}

	}
}
