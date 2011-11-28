package com.all.landownloader.discovery;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.landownloader.discovery.LanDiscoveryService.Announcer;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestLanDiscoveryService {
	@UnderTest
	private LanDiscoveryService discoveryService;
	@Mock
	private Announcer announcer;
	@Mock
	private LanDiscoverySocket discoverySocket;
	@Mock
	private Thread listenerThread;

	@Test
	public void shouldNotSendAnnounceIfIsWithinWindow() throws Exception {
		createAnnouncerInstance();

		// simulate time transcurred
		setValueToPrivateField(announcer, "previousSentMessage", System.currentTimeMillis());

		announcer.run();

		verify(discoverySocket, never()).announceNow();
	}

	private void createAnnouncerInstance() {
		announcer = discoveryService.new Announcer();
	}

	@Test
	public void shouldNotSendAnnounceJustOnceIfItIsWithinWindow() throws Exception {
		createAnnouncerInstance();

		announcer.run();

		announcer.run();

		verify(discoverySocket, times(1)).announceNow();
	}

	@Test
	public void shouldSendAgainAMessageAfterAPeriod() throws Exception {
		createAnnouncerInstance();
		announcer.run();

		long nextTime = System.currentTimeMillis() - LanDiscoveryService.MESSAGE_SENT_WINDOW -1000;

		setValueToPrivateField(announcer, "previousSentMessage", nextTime);

		announcer.run();

		verify(discoverySocket, times(2)).announceNow();
	}

	@Test
	public void shouldSendMessageAccordingToTime() throws Exception {
		createAnnouncerInstance();

		// check only called once
		announcer.run();
		announcer.run();
		verify(discoverySocket, times(1)).announceNow();

		// simulate time and should call a second time
		long nextTime = System.currentTimeMillis() - LanDiscoveryService.MESSAGE_SENT_WINDOW - 1000;
		setValueToPrivateField(announcer, "previousSentMessage", nextTime);
		announcer.run();
		verify(discoverySocket, times(2)).announceNow();

		// verify a call within the window doesn't make a new call
		announcer.run();
		verify(discoverySocket, times(2)).announceNow();

		announcer.run();
		verify(discoverySocket, times(2)).announceNow();

		// simulate time and should call a third time
		nextTime = System.currentTimeMillis() - LanDiscoveryService.MESSAGE_SENT_WINDOW - 1000;
		setValueToPrivateField(announcer, "previousSentMessage", nextTime);
		announcer.run();
		verify(discoverySocket, times(3)).announceNow();

		// verify a call within the window doesn't make a new call
		announcer.run();
		verify(discoverySocket, times(3)).announceNow();
	}

	@Test
	public void shouldStartListeningAndDie() throws Exception {
		discoveryService.startListening();
		Thread.sleep(100);
		verify(listenerThread).start();

		discoveryService.die();
		verify(listenerThread).interrupt();
		verify(discoverySocket).announceDeath();
	}

	public void setValueToPrivateField(Object object, String fieldName, Object value) throws Exception {
		Field privateField = object.getClass().getDeclaredField(fieldName);
		privateField.setAccessible(true);
		privateField.set(object, value);
	}

}
