package com.all.downloader.p2p.phexcore.share;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.helper.ShareHelper;
import com.all.downloader.share.CommonSharer;
import com.all.downloader.share.FileSharedEvent;
import com.all.downloader.share.ManagedSharer;
import com.all.downloader.share.ShareException;
import com.all.downloader.share.SharerListener;
import com.all.shared.download.TrackProvider;

@Service
public class PhexSharer extends CommonSharer implements ManagedSharer {

	@Autowired
	private PhexCore phexCore;
	@Autowired
	private TrackProvider trackProvider;
	@Autowired
	private ShareHelper shareHelper;

	@Override
	public void share(AllLink allLink) throws ShareException {
		if (phexCore.isSeedingGnutella()) {
			File fileToSeed = trackProvider.getFile(allLink.getHashCode());

			if (fileToSeed == null || !fileToSeed.exists()) {
				throw new ShareException("Could not share a file that doesn't exists: " + fileToSeed);
			}

			boolean containsUrnSha = allLink.containsUrnSha();
			if (!containsUrnSha) {
				String urnsha = shareHelper.getUrnsha(fileToSeed);
				AllLink updatedAllLink = new AllLink(allLink.getHashCode(), urnsha);
				notifyFileShared(updatedAllLink);
			} else if (!shareHelper.isURNShared(allLink.getUrnSha())) {
				shareHelper.getUrnsha(fileToSeed);
			}
		}
	}

	void notifyFileShared(AllLink allLink) {
		FileSharedEvent fileSharedEvent = new FileSharedEvent(this, allLink);

		for (SharerListener sharerListener : listeners) {
			sharerListener.onFileShared(fileSharedEvent);
		}
	}

}
