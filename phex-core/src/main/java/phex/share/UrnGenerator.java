package phex.share;

public class UrnGenerator {
	
	SharedFilesService sharedFilesService;

	public UrnGenerator(SharedFilesService sharedFilesService) {
		this.sharedFilesService = sharedFilesService;
	}
	
	public boolean generate(ShareFile shareFile) {
		UrnCalculationWorker worker = new UrnCalculationWorker(shareFile, sharedFilesService);
		worker.run();
		return shareFile.getURN() != null;
	}
}
