package com.all.downloader.p2p.phexcore;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class BasePhexTestCase {
	@Before
	public void initMockAnnotations() {
		MockitoAnnotations.initMocks(this);
	}
}
