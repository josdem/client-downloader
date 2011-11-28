package com.all.download.manager;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class UnitTestCase {
	@Before
	public void initMockAnnotations() {
		MockitoAnnotations.initMocks(this);
	}
}
