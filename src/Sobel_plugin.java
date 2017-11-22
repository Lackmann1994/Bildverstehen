import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Sobel_plugin implements PlugInFilter {
	
	@Override
	public void run(ImageProcessor ip) {
		
		ip = ip.convertToByteProcessor();
		ImageProcessor ip2 = ip.duplicate();
		
		ip2.findEdges();
		
		ImagePlus impOverlay = new ImagePlus("Sobel", ip2);
		impOverlay.show();
		
		
	}
		
	@Override
	public int setup(String arg0, ImagePlus arg1) {
		return DOES_ALL + NO_UNDO + NO_CHANGES;
	}
}
