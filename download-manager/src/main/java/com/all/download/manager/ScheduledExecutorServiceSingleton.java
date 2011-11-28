package com.all.download.manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ScheduledExecutorServiceSingleton {
	final int THREAD_POOL_SIZE = 15;
	Log log = LogFactory.getLog(this.getClass());
	List<ScheduledExecutorService> scheduledExecutorServiceList = new ArrayList<ScheduledExecutorService>();

	public ScheduledExecutorServiceSingleton() {
	}

	public void destroy() {
		Iterator<ScheduledExecutorService> iterator = scheduledExecutorServiceList.iterator();
		
		while(iterator.hasNext()) {
			ScheduledExecutorService scheduledExecutorService = iterator.next();

			try {
				// close the thread pool, should only be closed here but no harm if
				// it was closed somewhere else
				scheduledExecutorService.shutdown();
				if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
					log.warn("Thread pool did not terminate correctly");
				}
			} catch (InterruptedException ie) {
				scheduledExecutorService.shutdownNow();
			}
			
			iterator.remove();
		}
		
	}

	public ScheduledExecutorService getInstance() {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE,
				new DownloaderThreadFactory());
		
		scheduledExecutorServiceList.add(scheduledExecutorService);
		
		return scheduledExecutorService;
	}

}

class DownloaderThreadFactory implements ThreadFactory {
	AtomicInteger threadNumber = new AtomicInteger(-1);

	@Override
	public Thread newThread(Runnable runnable) {
		StringBuilder sb = new StringBuilder();
		sb.append("download-thread-");
		sb.append(threadNumber.incrementAndGet());

		Thread thread = new Thread(runnable, sb.toString());
		thread.setDaemon(true);

		return thread;
	}

}
