package com.all.downloader.p2p.phexcore.download;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import phex.share.ShareFile;

import com.all.downloader.download.DownloadException;
import com.all.downloader.p2p.phexcore.PhexCoreImpl;
import com.all.downloader.p2p.phexcore.download.PhexDownloader;

public class SeedClient {

	private static Log log = LogFactory.getLog(SeedClient.class);

	private JButton downloadButton = new JButton("Start");
	private PhexDownloader phexManager;
	private JFrame frame = new JFrame();
	private JPanel panel = new JPanel();
	private Timer timer;
	private List<ShareFile> shareFileList;
	private Set<File> filesToShare = null;
	private String path = File.separator + "Users" + File.separator + "user" + File.separator + "Share";

	private void initialize() throws FileNotFoundException, IOException, DownloadException {
		doLayout();

		PhexCoreImpl phexCore = new PhexCoreImpl();
		Properties phexPros = new Properties();
		phexManager = new PhexDownloader(phexCore);
		// phexManager.init();
		filesToShare = new HashSet<File>();
		shareFileList = new ArrayList<ShareFile>();

		downloadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// log.error("I'm listening on: " +
				// phexManager.getsWrapper().getPort());
				// File dir = new File(path);
				// searchFilesToAddList(dir);
				// for (File file : filesToShare) {
				// ShareFile shareFile = new ShareFile(file);
				// phexManager.getsWrapper().getSharedFilesService().queueUrnCalculation(shareFile);
				// shareFileList.add(shareFile);
				// }
				// timer.start();
			}
		});

		timer = new Timer(2000, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				log.debug("-----------------------------------------------");
				for (ShareFile shareFile : shareFileList) {
					log.debug("file :: " + shareFile.getFileName());
					log.debug("urnSha1 :: " + "urn:sha1:" + shareFile.getSha1());
					log.debug("size :: " + shareFile.getSystemFile().length());
				}
				log.debug("-----------------------------------------------");
			}
		});
	}

	public void searchFilesToAddList(File root) {
		String[] list;
		list = root.list();

		for (int i = 0; i < list.length; i++) {
			File file = new File(root.getAbsolutePath() + File.separator + list[i]);
			if (file.isDirectory()) {
				searchFilesToAddList(file);
			} else if (file.getName().toLowerCase().endsWith(".mp3") || file.getName().toLowerCase().endsWith(".mp4")) {
				filesToShare.add(file);
			}
		}
	}

	private void doLayout() {
		frame.setBounds(0, 0, 200, 100);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel.add(downloadButton);
		frame.add(panel);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		try {
			new SeedClient().initialize();
		} catch (FileNotFoundException e) {
			log.error(e, e);
		} catch (IOException e) {
			log.error(e, e);
		} catch (DownloadException e) {
			log.error(e, e);
		}
	}

}
