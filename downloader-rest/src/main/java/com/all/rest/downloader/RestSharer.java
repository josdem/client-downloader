package com.all.rest.downloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.share.CommonSharer;
import com.all.downloader.share.ManagedSharer;
import com.all.downloader.share.ShareException;
import com.all.messengine.MessEngine;
import com.all.messengine.MessageListener;
import com.all.rest.config.RestClientConfig;
import com.all.shared.download.TurnSeederInfo;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;

@Service
public class RestSharer extends CommonSharer implements ManagedSharer {

	private final Log log = LogFactory.getLog(this.getClass());

	private final ShareTracksTask shareTask = new ShareTracksTask();

	private final ScheduledExecutorService sharingExecutor = Executors.newSingleThreadScheduledExecutor();
	@Autowired
	private RestClientConfig restConfig;
	@Autowired
	private MessEngine messEngine;

	private final Collection<String> allSharedHashcodes = new HashSet<String>();

	@PostConstruct
	public void init() {
		sharingExecutor.scheduleWithFixedDelay(shareTask, restConfig.getInitialShareDelay(TimeUnit.SECONDS), restConfig
				.getShareDelay(TimeUnit.SECONDS), TimeUnit.SECONDS);
		messEngine.addMessageListener(MessEngineConstants.NEW_ULTRAPEER_SESSION_TYPE,
				new MessageListener<AllMessage<String>>() {
					@Override
					public void onMessage(AllMessage<String> message) {
						log.info("A new session to ultrapeer " + message.getBody()
								+ " was detected. Will republish shared hashcodes...");
						republishSharedHashcodes();
					}
				});
	}

	private void republishSharedHashcodes() {
		shareTask.addAll(allSharedHashcodes);
		sharingExecutor.execute(shareTask);
	}

	@Override
	public void share(AllLink allLink) throws ShareException {
		shareTask.add(allLink.getHashCode());
	}

	private final class ShareTracksTask implements Runnable {

		private final Set<String> unsharedTracks = new HashSet<String>();
		private final Object mutex = new Object();

		@Override
		public void run() {
			if (restConfig.getUserId() != null) {
				TurnSeederInfo seederInfo = new TurnSeederInfo();
				seederInfo.setSeederId(restConfig.getUserId());
				synchronized (mutex) {
					seederInfo.setTracks(new ArrayList<String>(unsharedTracks));
					allSharedHashcodes.addAll(unsharedTracks);
					unsharedTracks.clear();
				}
				if (!seederInfo.getTracks().isEmpty()) {
					messEngine.send(new AllMessage<TurnSeederInfo>(MessEngineConstants.REST_UPDATE_SEEDER_TRACKS, seederInfo));
					log.info("Sent seeder info update: " + seederInfo);
				}
			}
		}

		public void add(String hashcode) {
			synchronized (mutex) {
				unsharedTracks.add(hashcode);
			}
		}
		
		public void addAll(Collection<String> hashcodes){
			synchronized (mutex) {
				unsharedTracks.addAll(hashcodes);
			}
		}

	}

}
