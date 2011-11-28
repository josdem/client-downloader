package com.all.downloader.p2p.phexcore;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import phex.common.Environment;
import phex.common.Phex;
import phex.common.ThreadTracking;
import phex.common.URN;
import phex.common.log.LogUtils;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.gui.prefs.PhexGuiPrefs;
import phex.host.Host;
import phex.prefs.core.DownloadPrefs;
import phex.prefs.core.NetworkPrefs;
import phex.prefs.core.PhexCorePrefs;
import phex.query.Search;
import phex.query.SearchContainer;
import phex.servent.Servent;
import phex.share.SharedFilesService;
import phex.utils.Localizer;
import phex.utils.SystemProperties;

import com.all.commons.IncrementalNamedThreadFactory;
import com.all.core.common.spring.InitializeService;
import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.messengine.MessEngine;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;
import com.all.shared.stats.usage.UserActions;

/**
 * understands a wrapper for simplified calls to Phex
 */
@Service
public class PhexCoreImpl implements PhexCore {

	public static final String SEEDING_GNUTELLA_KEY = "seedingGnutella";
	public static final String PRIORITY_GNUTELLA = "priority.gnutella";
	public static final String TIMEOUT_GNUTELLA = "timeout.gnutella";

	private final Log log = LogFactory.getLog(this.getClass());

	private final ScheduledExecutorService scheduler = Executors
			.newSingleThreadScheduledExecutor(new IncrementalNamedThreadFactory("ultraPeerConnectionTask"));

	@Autowired
	private ManagedDownloaderConfig downloaderConfig;

	private Servent servent;

	boolean seedingGnutella = true;

	// TODO simplify initialize process
	@InitializeService
	public void initialize() throws Exception {
		// Since Phex store downloads information on cfg path we have to set it
		// on a ALL path
		String phexConfigPath = new File(downloaderConfig.getUserConfigPath()).getAbsolutePath();

		System.setProperty(SystemProperties.PHEX_CONFIG_PATH_SYSPROP, phexConfigPath);

		initPhexInternal();

		DownloadPrefs.DestinationDirectory.set(downloaderConfig.getCompleteDownloadsPath());
		DownloadPrefs.IncompleteDirectory.set(downloaderConfig.getIncompleteDownloadsPath());
		DownloadPrefs.save(true);

		servent = Servent.getInstance();
		NetworkPrefs.ListeningPort.set(PORT);
		if (!servent.isRunning()) {
			servent.start();
		}

		seedingGnutella = Boolean.parseBoolean(downloaderConfig.getProperty(SEEDING_GNUTELLA_KEY));
		scheduler.schedule(new UltraPeerConnectionTask(), 1, TimeUnit.MINUTES);
	}

	void initPhexInternal() throws Exception {
		try {
			LogUtils.initializeLogging();
			PhexGuiPrefs.init();
			Localizer.initialize(null);

			ThreadTracking.initialize();
			PhexCorePrefs.init();
			Phex.initialize();
		} catch (Exception e) {
			log.error(e, e);
			throw e;
		}
	}

	@PreDestroy
	public void shutdown() {
		if (servent.isRunning()) {
			try {
				servent.stop();
			} catch (Exception e) {
				log.error(e, e);
			}
			;
		}

		log.info("Shutting down timer service...");
		Environment.getInstance().getTimerService().cancel();

		log.info("Shutting down phex pool thread...");
		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Environment.getInstance().getThreadPool();
		threadPool.shutdownNow();
		scheduler.shutdownNow();
	}

	@Override
	public void processPhexAnotation(Object object) {
		Phex.getEventService().processAnnotations(object);
	}

	@Override
	public Search createSearchTerm(String serchTerm) {
		SearchContainer searchContainer = servent.getQueryService().getSearchContainer();
		return searchContainer.createSearch(serchTerm);
	}

	@Override
	public SWDownloadFile getDownloadFileByURN(URN urn) {
		return servent.getDownloadService().getDownloadFileByURN(urn);
	}

	@Override
	public SWDownloadFile addFileToDownload(RemoteFile remoteFile, String filename, String searchTerm) {
		return servent.getDownloadService().addFileToDownload(remoteFile, filename, searchTerm);
	}

	@Override
	public void removeSWDownloadFile(SWDownloadFile swDownloadFile) {
		SwarmingManager downloadService = getDownloadService();
		downloadService.removeDownloadFile(swDownloadFile);
	}

	@Override
	public boolean isConnected() {
		return servent.getHostService().getUltrapeerConnections().length > 0;
	}

	@Override
	public boolean isSeedingGnutella() {
		return seedingGnutella;
	}

	@Override
	public boolean isAnyUltraPeerConnection() {
		Host[] ultrapeerConnections = servent.getHostService().getUltrapeerConnections();

		if (ultrapeerConnections.length == 0) {
			log.info("Not connected to ultrapeers");
			return false;
		}

		log.info("Ultra peers connected: ");
		for (Host ultrapeer : ultrapeerConnections) {
			log.info(ultrapeer);
		}

		return true;
	}

	public SwarmingManager getDownloadService() {
		return Servent.getInstance().getDownloadService();
	}

	public SharedFilesService getSharedFilesService() {
		return servent.getSharedFilesService();
	}

	public String createNaturalSearchTerm(String keyword) {
		return phex.utils.StringUtils.createNaturalSearchTerm(keyword);
	}

	@Override
	public int getDownloaderPriority() {
		return downloaderConfig.getDownloaderPriority(PRIORITY_GNUTELLA);
	}

	public int getDownloaderTimeout() {
		return downloaderConfig.getDownloaderSearchTimeout(TIMEOUT_GNUTELLA);
	}

	private class UltraPeerConnectionTask implements Runnable {
		@Autowired
		private MessEngine messEngine;

		@Override
		public void run() {
			boolean connected = isAnyUltraPeerConnection();
			sendUltrapeerConnectionStat(connected);
		}

		private void sendUltrapeerConnectionStat(boolean conected) {
			log.info("Sending ultrapeer connection stat connected? " + conected);
			if (conected) {
				messEngine.send(new AllMessage<Integer>(MessEngineConstants.REPORT_USER_ACTION,
						UserActions.Downloads.GNUTELLA_ULTRAPEER_CONNECTION));
			}
		}
	}

}
