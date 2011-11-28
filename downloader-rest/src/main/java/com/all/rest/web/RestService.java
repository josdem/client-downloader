package com.all.rest.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.all.rest.beans.RestTrackStatus;
import com.all.rest.beans.TopHundredCategory;
import com.all.rest.beans.TopHundredPlaylist;
import com.all.rest.beans.TopHundredRestTrackSource;
import com.all.rest.beans.readers.TopHundredCategoryJsonReader;
import com.all.rest.beans.readers.TopHundredPlaylistJsonReader;
import com.all.rest.config.RestClientConfig;
import com.all.shared.json.JsonConverter;
import com.all.shared.mc.TrackSearchResult;
import com.all.shared.mc.TrackStatus;
import com.all.shared.model.Category;
import com.all.shared.model.Playlist;
import com.all.shared.model.RemoteTrack;
import com.all.shared.model.Track;

@Service
public class RestService {

	private static final String TRACK_SEARCH_URL = "/search";
	private static final String TRACK_METADATA_URL = "/{trackId}/metadata";
	private static final String TRACKS_AVAILABILITY_URL = "/availability";
	private static final String UPLOAD_RATE_URL = "/upload/rate";
	private static final String TRACK_UPLOAD_URL = "/{trackId}/upload";
	private static final String TRACK_STATUS_URL = "/{trackId}/status";
	private static final String TRACK_CHUNK_URL = "/{trackId}/{chunkId}";
	private static final String REST_PEER_HEALTH_URL = "/health";
	private static final String REST_PEERS_URL = "peers/rest";
	private static final String REST_PEER_URL_KEY = "restServerUrl";
	private static final String TRACKER_SERVER_URL_KEY = "trackerUrl";

	private static final String TOP_HUNDRED_CATEGORY_LIST = "/top/categories";
	private static final String TOP_HUNDRED_CATEGORY_PLAYLIST = "/top/{categoryId}";
	private static final String TOP_HUNDRED_PLAYLIST_TRACKS = "/top/playlist/{playlistId}";
	private static final String TOP_HUNDRED_RANDOM_PLAYLIST = "/top/random";

	private static final byte[] SPEED_TEST_CHUNK = new byte[1024 * 20];

	private final Log log = LogFactory.getLog(this.getClass());
	@Autowired
	private RestClientConfig restConfig;
	@Autowired
	private RestTemplate restTemplate;

	private String restPeerUri;

	private String trackerServerUri;

	public RestService() {
		JsonConverter.addJsonReader(TopHundredPlaylist.class, new TopHundredPlaylistJsonReader());
		JsonConverter.addJsonReader(TopHundredCategory.class, new TopHundredCategoryJsonReader());
	}

	@PostConstruct
	public void initialize() {
		restPeerUri = restConfig.getProperty(REST_PEER_URL_KEY);
		trackerServerUri = restConfig.getProperty(TRACKER_SERVER_URL_KEY);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				verifyRestPeerHealth();
			}
		});
	}

	private String createUrl(String urlExt) {
		return restPeerUri + urlExt;
	}

	public byte[] getChunk(final String trackId, final int chunk) {
		return execute(new RestCall<byte[]>() {
			@Override
			public byte[] execute() {
				return restTemplate.getForObject(createUrl(TRACK_CHUNK_URL), byte[].class, trackId, chunk);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<String> findTracksById(final List<String> trackIds) {
		return execute(new RestCall<List<String>>() {
			@Override
			public List<String> execute() {
				String json = restTemplate.postForObject(createUrl(TRACKS_AVAILABILITY_URL), JsonConverter.toJson(trackIds), String.class);
				return JsonConverter.toCollection(json, ArrayList.class);
			}
		});
	}

	public int getUploadRate() {
		return execute(new RestCall<Integer>() {
			@Override
			public Integer execute() {
				log.info("Calculating upload rate...");
				// discard first
				int uploadRate = 0;
				calculateUploadRate(new byte[1024]);
				int chunkNumber = 5;
				int partialRate = 0;
				for (int i = 0; i < chunkNumber; i++) {
					partialRate += calculateUploadRate(SPEED_TEST_CHUNK);
				}
				uploadRate = (partialRate / chunkNumber);
				log.info("Upload rate is around " + (uploadRate / 1000.0) + " kb/s");
				return uploadRate;
			}
		});
	}

	public void cancelUpload(final String trackId) {
		execute(new RestCall<Void>() {
			@Override
			public Void execute() {
				restTemplate.delete(createUrl(TRACK_UPLOAD_URL), trackId);
				return null;
			}
		});
	}

	public void uploadMetadata(final Track track) {
		execute(new RestCall<Void>() {
			@Override
			public Void execute() {
				restTemplate.put(createUrl(TRACK_METADATA_URL), JsonConverter.toJson(track), track.getHashcode());
				return null;
			}
		});
	}

	public TrackStatus getStatus(final String trackId) {
		return execute(new RestCall<TrackStatus>() {
			@Override
			public TrackStatus execute() {
				String json = restTemplate.getForObject(createUrl(TRACK_STATUS_URL), String.class, trackId);
				return JsonConverter.toBean(json, RestTrackStatus.class);
			}
		});
	}

	private int calculateUploadRate(byte[] chunk) {
		long startTime = System.currentTimeMillis();
		restTemplate.put(createUrl(UPLOAD_RATE_URL), chunk);
		long transferTime = System.currentTimeMillis() - startTime;
		return (int) (chunk.length * 1000.0 / transferTime);
	}

	public void uploadChunk(final String trackId, final int chunkNum, final byte[] chunk) {
		execute(new RestCall<Void>() {
			@Override
			public Void execute() {
				restTemplate.postForLocation(createUrl(TRACK_CHUNK_URL), chunk, trackId, chunkNum);
				return null;
			}
		});

	}

	@SuppressWarnings("unchecked")
	public List<TrackSearchResult> findTracksByKeyword(final String keyword) {
		return execute(new RestCall<List<TrackSearchResult>>() {
			@Override
			public List<TrackSearchResult> execute() {
				String jsonResult = restTemplate.postForObject(createUrl(TRACK_SEARCH_URL), keyword, String.class);
				return JsonConverter.toTypedCollection(jsonResult, ArrayList.class, TrackSearchResult.class);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Category> findTopCategories() {
		final RestService restService = this;
		return execute(new RestCall<List<Category>>() {
			@Override
			public List<Category> execute() {
				String createUrl = createUrl(TOP_HUNDRED_CATEGORY_LIST);
				String jsonResult = restTemplate.getForObject(createUrl, String.class);
				List<TopHundredCategory> categories = JsonConverter.toTypedCollection(jsonResult, ArrayList.class, TopHundredCategory.class);
				for (TopHundredCategory category : categories) {
					category.setRestService(restService);
				}
				return new ArrayList<Category>(categories);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Playlist> findPlaylistsForTopCategories(final Category category) {
		final RestService restService = this;
		return execute(new RestCall<List<Playlist>>() {
			@Override
			public List<Playlist> execute() {
				try {
					String jsonResult = restTemplate.getForObject(createUrl(TOP_HUNDRED_CATEGORY_PLAYLIST), String.class, category.getId());
					List<TopHundredPlaylist> topPlaylists = JsonConverter.toTypedCollection(jsonResult, ArrayList.class, TopHundredPlaylist.class);
					for (TopHundredPlaylist playlist : topPlaylists) {
						if(!playlist.isTrackSourceSet()){
							playlist.setTrackSource(new TopHundredRestTrackSource(restService, playlist));
						}
					}
					return new ArrayList<Playlist>(topPlaylists);
				} catch (Exception e) {
					return Collections.emptyList();
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Track> getTracksForPlaylist(final Playlist playlist) {
		return execute(new RestCall<List<Track>>() {
			@Override
			public List<Track> execute() {
				String jsonResult = restTemplate.getForObject(createUrl(TOP_HUNDRED_PLAYLIST_TRACKS), String.class, playlist.getHashcode());
				return JsonConverter.toTypedCollection(jsonResult, ArrayList.class, RemoteTrack.class);
			}
		});
	}

	public Playlist getRandomPlaylist() {
		final RestService restService = this;
		return execute(new RestCall<Playlist>() {
			@Override
			public Playlist execute() {
				String jsonResult = restTemplate.getForObject(createUrl(TOP_HUNDRED_RANDOM_PLAYLIST), String.class);
				TopHundredPlaylist playlist = JsonConverter.toBean(jsonResult, TopHundredPlaylist.class);
					if(!playlist.isTrackSourceSet()){
						playlist.setTrackSource(new TopHundredRestTrackSource(restService, playlist));
					}
				return playlist;
			}
		});
	}

	private <T> T execute(RestCall<T> invocation) {
		try {
			return invocation.execute();
		} catch (Exception e) {
			log.error("Could not execute rest call.", e);
			verifyRestPeerHealth();
			throw new RuntimeException("Could not execute rest call.", e);
		}
	}

	private void verifyRestPeerHealth() {
		try {
			log.info("Verifying Rest Peer Availability...");
			restTemplate.getForObject(createUrl(REST_PEER_HEALTH_URL), String.class);
			log.info("Rest Peer at " + restPeerUri + " is up and running.");
		} catch (Exception e) {
			log.error("Rest Server is down.", e);
			switchRestPeer();
		}
	}

	private void switchRestPeer() {
		try {
			log.info("Looking for another Rest Peer Instance...");
			@SuppressWarnings("unchecked")
			List<String> urls = JsonConverter.toCollection(restTemplate.getForObject(trackerServerUri + REST_PEERS_URL, String.class), ArrayList.class);
			urls.remove(restPeerUri);
			if (!urls.isEmpty()) {
				log.info("Switching from " + restPeerUri + " to " + urls.get(0));
				restPeerUri = urls.get(0);
			}
		} catch (Exception e) {
			log.error("Could not update Rest Peer url with Tracker.", e);
		}
	}

	private abstract class RestCall<T> {
		public abstract T execute();
	}

}
