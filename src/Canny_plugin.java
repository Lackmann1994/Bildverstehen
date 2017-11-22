import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Canny_plugin implements PlugInFilter {
	
	@Override
	public void run(ImageProcessor ip) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		ByteProcessor ip_byte = ip.convertToByteProcessor();
		Mat image = OCV2IJ.byteProc2Mat(ip_byte);
		
		Imgproc.Canny(image, image, 100, 200);
		
		
		
		ByteProcessor ip_byte_processed = OCV2IJ.mat2ByteProc(image);
		ImagePlus impOverlay = new ImagePlus("Canny", ip_byte_processed);
		impOverlay.show();
		
		
	}
		
	@Override
	public int setup(String arg0, ImagePlus arg1) {
		return DOES_ALL + NO_UNDO + NO_CHANGES;
	}
}
