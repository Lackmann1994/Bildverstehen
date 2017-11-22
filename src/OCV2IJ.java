// Andreas Siebert
// Sept. 2014
// Some auxiliary methods to convert OpenCV formats into ImageJ formats, and vice versa.
// Only for gray value images (Mat <=> ByteProcessor) and 32-bit float images (Mat <=> FloatProcessor)

import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class OCV2IJ
{

	//************************************************************************************************
	// ImageJ -> OpenCV
	// create Mat of OpenCV data type CV_8UC1 from ImageJ ByteProcessor
	static Mat byteProc2Mat(ByteProcessor ip_byte)
	{
		Mat image = new Mat(ip_byte.getHeight(), ip_byte.getWidth(), CvType.CV_8UC1);
		byte[] pixels = (byte[]) ip_byte.getPixels();
		
		image.put(0, 0, pixels);  // get all the pixels. Note: &0xFF not used -- works well
	
		return image;
	}
	
	//************************************************************************************************
	// OpenCV -> ImageJ
	// create ByteProcessor from Mat of OpenCV data type CV_8UC1
	static ByteProcessor mat2ByteProc(Mat image)
	{
		if (image.type() != CvType.CV_8UC1)
		{
			IJ.log("Error: cannot convert image type " + image.type() + " to ByteProcessor.");
			return null;
		}
	
		byte[] pixels = new byte[image.width()*image.height()];
		image.get(0, 0, pixels);  // get all the pixels

		return new ByteProcessor(image.width(), image.height(), pixels);
	}
	
	
	//************************************************************************************************
	// ImageJ -> OpenCV
	// create Mat of OpenCV data type CV_32F from ImageJ FloatProcessor
	static Mat floatProc2Mat(FloatProcessor ip_float)
	{
		Mat image = new Mat(ip_float.getHeight(), ip_float.getWidth(), CvType.CV_32F);
		float[] pixels = (float[]) ip_float.getPixels();
		
		image.put(0, 0, pixels);  // get all the pixels.
	
		return image;
	}
		
	//************************************************************************************************
	// OpenCV -> ImageJ
	// create FloatProcessor from Mat of OpenCV data type CV_32F
	static FloatProcessor mat2FloatProc(Mat image)
	{
		if (image.type() != CvType.CV_32F)
		{
			IJ.log("Error: cannot convert image type " + image.type() + " to FloatProcessor.");
			return null;
		}
				
		float[] pixels = new float[image.width()*image.height()];
		image.get(0, 0, pixels);  // get all the pixels
		
		return new FloatProcessor(image.width(), image.height(), pixels);	
	}
	

	//************************************************************************************************
	// for historical reasons only: don't do it this way
	Mat convertImageProcessor2MatSlow(ByteProcessor ip_byte)
	{
		Mat image = new Mat(ip_byte.getHeight(), ip_byte.getWidth(), CvType.CV_8UC1);
		byte[] pixels = (byte[]) ip_byte.getPixels();
		
		int index = 0;
		for (int row=0; row<ip_byte.getHeight(); row++)
			for (int col=0; col<ip_byte.getWidth(); col++)
			{
				image.put(row, col, pixels[index] & 0xFF);  // really slow
				index++;
			}	
		return image;
	}
}

