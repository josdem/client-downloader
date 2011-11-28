package com.all.rest.downloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.all.downloader.alllink.AllLink;
import com.all.messengine.Message;
import com.all.messengine.impl.DefaultMessEngine;
import com.all.rest.config.RestClientConfig;
import com.all.shared.download.TurnSeederInfo;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;

public class TestRestSharer {

	@InjectMocks
	private RestSharer restPublisher = new RestSharer();

	@Mock
	private RestClientConfig restConfig;
	@Mock
	private ScheduledExecutorService executor;
	@Spy
	private DefaultMessEngine messEngine = new DefaultMessEngine();
	@Captor
	private ArgumentCaptor<Runnable> taskCaptor;
	@Captor
	private ArgumentCaptor<AllMessage<?>> messageCaptor;
	@Mock
	private AllLink allLink;

	private String trackId = "00a9ae41a50cfece357f26e786db6fa014af765b";

	private String email = "seeder@all.com";

	private long shareDelay = 30L;

	private long initialDelay = 15L;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		messEngine.init();

		when(allLink.getHashCode()).thenReturn(trackId);
		when(restConfig.getUserId()).thenReturn(email);
		when(restConfig.getInitialShareDelay(TimeUnit.SECONDS)).thenReturn(initialDelay);
		when(restConfig.getShareDelay(TimeUnit.SECONDS)).thenReturn(shareDelay);

		restPublisher.init();

		verify(executor).scheduleWithFixedDelay(taskCaptor.capture(), eq(initialDelay), eq(shareDelay),
				eq(TimeUnit.SECONDS));
	}

	@Test
	public void shouldShareATrackAsynchronouslyOnlyOnce() throws Exception {
		Runnable shareTask = taskCaptor.getValue();

		restPublisher.share(allLink);
		shareTask.run();

		verify(messEngine).send(messageCaptor.capture());
		AllMessage<?> message = messageCaptor.getValue();
		assertNotNull(message);
		TurnSeederInfo seederInfo = (TurnSeederInfo) message.getBody();
		assertEquals(email, seederInfo.getSeederId());
		List<String> tracks = seederInfo.getTracks();
		assertEquals(1, tracks.size());
		assertEquals(trackId, tracks.get(0));
		reset(messEngine);

		String anotherTrackId = "aaa9ae41a5000ece357f26e786db6fa014af7ccc";
		when(allLink.getHashCode()).thenReturn(anotherTrackId);

		restPublisher.share(allLink);
		shareTask.run();

		verify(messEngine).send(messageCaptor.capture());
		message = messageCaptor.getValue();
		assertNotNull(message);
		seederInfo = (TurnSeederInfo) message.getBody();
		assertEquals(email, seederInfo.getSeederId());
		tracks = seederInfo.getTracks();
		assertEquals(1, tracks.size());
		assertEquals(anotherTrackId, tracks.get(0));
		reset(messEngine);

		shareTask.run();

		verify(messEngine, never()).send(any(AllMessage.class));
	}

	@Test
	public void shouldRepublishAllSharedTracksWhenNewUltrapeerSessionDetected() throws Exception {
		Runnable shareTask = taskCaptor.getValue();

		restPublisher.share(allLink);
		shareTask.run();

		verify(messEngine).send(messageCaptor.capture());
		AllMessage<?> message = messageCaptor.getValue();
		assertNotNull(message);
		TurnSeederInfo seederInfo = (TurnSeederInfo) message.getBody();
		assertEquals(email, seederInfo.getSeederId());
		List<String> tracks = seederInfo.getTracks();
		assertEquals(1, tracks.size());
		assertEquals(trackId, tracks.get(0));
		reset(messEngine);
		
		messEngine.send(new AllMessage<String>(MessEngineConstants.NEW_ULTRAPEER_SESSION_TYPE, "ultrapeerIp"));
		verify(executor, timeout(100)).execute(shareTask);
		reset(messEngine);
		shareTask.run();

		verify(messEngine).send(messageCaptor.capture());
		Message<?> republishMessage = messageCaptor.getValue();
		assertNotSame(message, republishMessage);
		assertEquals(MessEngineConstants.REST_UPDATE_SEEDER_TRACKS, republishMessage.getType());
		seederInfo = (TurnSeederInfo) republishMessage.getBody();
		assertEquals(email, seederInfo.getSeederId());
		tracks = seederInfo.getTracks();
		assertEquals(1, tracks.size());
		assertEquals(trackId, tracks.get(0));
		
		reset(messEngine);
		shareTask.run();
		verify(messEngine, never()).send(messageCaptor.capture());

	}
	
}