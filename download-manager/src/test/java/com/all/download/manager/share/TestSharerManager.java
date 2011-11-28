package com.all.download.manager.share;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import com.all.downloader.share.ManagedSharer;
import com.all.testing.MockInyectRunner;
import com.all.testing.Stub;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestSharerManager {
	@UnderTest
	private SharerManager sharerManager;
	@Stub
	private Collection<ManagedSharer> sharerCollection = new ArrayList<ManagedSharer>();
	@Mock
	ManagedSharer managedSharer;

	@Before
	public void setup() {
		sharerCollection.add(managedSharer);
	}

	@Test
	public void shouldInitialize() throws Exception {
		sharerManager.initialize();
		
		verify(managedSharer).addSharerListener(sharerManager);
	}

}
