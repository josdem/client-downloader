package com.all.downloader.p2p.phexcore.download;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.p2p.phexcore.PhexCoreImpl;

public class DownloadClient {

	private JButton downloadButton = new JButton("Start");
	private PhexDownloader phexManager;
	private JFrame frame = new JFrame();
	private JPanel panel = new JPanel();
	private List<DownloadBean> downloadBeanList;

	private DownloadBean bean;
	private int completeDownloads = 0;
	private static Log log = LogFactory.getLog(DownloadClient.class);

	private void initialize() throws Exception {
		doLayout();

		PhexCoreImpl phexCore = new PhexCoreImpl();

		phexManager = new PhexDownloader(phexCore);
		phexManager.addDownloaderListener(new DownloaderListener() {

			@Override
			public void onDownloadUpdated(DownloadUpdateEvent downloadUpdateEvent) {
			}

			@Override
			public void onDownloadCompleted(DownloadCompleteEvent completeEvent) {
				File file = completeEvent.getDestinationFile();
				completeDownloads++;
				log.debug(file.getName() + " :: " + "complete");
				int progress = (completeDownloads * 100) / downloadBeanList.size();
				progressBar.setValue(progress);
			}
		});
		downloadBeanList = new ArrayList<DownloadBean>();
		getDownloadsBeans();
		log.debug("size DownloadBeanList: " + downloadBeanList.size());

		downloadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (DownloadBean download : downloadBeanList) {
					try {
						phexManager.download(download.getHash());
					} catch (DownloadException doe) {
						log.debug(doe, doe);
					}
				}
			}
		});
	}

	private void getDownloadsBeans() {
		bean = new DownloadBean();
		bean.setHash("1a11f920740de7012c670692bfa6f8bfdb4b946d");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("2042fc16aa6e44d961985e37d65df731eeddac52");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("26baa3a448d22e7627c5d587f42291c42d623ca0");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("37f10efea54e1350d8991cfbc76616d2b097d9d5");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("6b1685f4a0aa098cb0b555e7c91498bc5c5411c3");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("6d4c4b748dd4732f5027a970013cfde8a4e5f800");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("793b4cff24ac5774b5b5b713bea8c7d7d133abad");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("96beb6cbdce6e3ba3a63d3738b0ec8af48b51e90");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("a4cd551630ad51b1fef3f9ee9a3109ddc4ac04d2");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("bbce686d4b36e51757ead78f7bc4e5d704b49b1b");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("cd7f48aacf7762b1ae3ceec71ff6983e5efeec13");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("d09ae7407e0edc4e9fea971d708082c2a0b76404");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("d51ea017f91251174d7fb2ca4d6528b1c040ef2a");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("de9ae5347528a4f1f482ad92004face74c886251");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("e40179c15c56dc22d6772605e7718ec8a7108de6");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("eff754543b75b0934084128d7da61efe951a30a9");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("f0b1b39e230daa1215ba9c8a2a4c79c2f631e9a2");
		downloadBeanList.add(bean);
		bean = new DownloadBean();
		bean.setHash("f5c61e6eb5d1949656163487bf575692bbfa42a4");
		downloadBeanList.add(bean);
	}

	private void doLayout() {
		frame.setBounds(0, 0, 200, 100);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		progressBar = new JProgressBar();
		panel.add(progressBar);
		panel.add(downloadButton);
		frame.add(panel);
		frame.setVisible(true);
	}

	class DownloadBean {
		private String hash;

		public void setHash(String hash) {
			this.hash = hash;
		}

		public String getHash() {
			return hash;
		}
	}

	public static void main(String[] args) {
		try {
			new DownloadClient().initialize();
		} catch (FileNotFoundException e) {
			log.error(e, e);
		} catch (IOException e) {
			log.error(e, e);
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	private JProgressBar progressBar;

}
