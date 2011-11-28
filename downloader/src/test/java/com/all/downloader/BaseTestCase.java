package com.all.downloader;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class BaseTestCase {
	@Before
	public void initMockAnnotations() {
		MockitoAnnotations.initMocks(this);
	}
}
