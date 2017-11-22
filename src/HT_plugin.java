import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;

import java.awt.desktop.SystemSleepEvent;

public class HT_plugin implements PlugInFilter {
	
	@Override
	public void run(ImageProcessor ip) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		ByteProcessor ip_byte = ip.convertToByteProcessor();
		Mat image = OCV2IJ.byteProc2Mat(ip_byte);

        Imgproc.Canny(image, image, 100, 200);
//        image.put(0,0,new double[]{127});


        ByteProcessor ip_byte_processed = OCV2IJ.mat2ByteProc(image);

//        int width = image.width();
//        int height = image.height();
//        int p_h = (int)Math.sqrt(width*width+height*height);
//        int p = p_h*2+1;
//        int a_steps = 180;
//        int pi;
//        int akkumulator[][] = new int[p][a_steps];
//        Mat image2 = Mat.zeros(a_steps, p, CvType.CV_8UC1);
//        Mat image2 = new Mat(a_steps, p, CvType.CV_8UC1);
//
//        for (int row=0; row < a_steps; row++) {
//            for (int col = 0; col < p; col++) {
//                image2.put(row,col, 0  & 0xFF);
//                image2.get(row,col)[0]++;
//
//                System.out.println(image2.get(row,col)[0]);
//            }
//        }
//        for(int row=0; row<height; row++)
//            for(int col=0; col<width; col++){
//                if(image.get(row,col)[0] == 255 ){
//                    System.out.println(image.get(row,col)[0]);
//                }
//            }

//        ByteProcessor ht = OCV2IJ.mat2ByteProc(image2);



        int width = ip_byte_processed.getWidth();
        int height = ip_byte_processed.getHeight();
        int p_h = (int)Math.sqrt(width*width+height*height);
        int p = p_h*2+1;
        int a_steps = 180;
        int akkumulator[][] = new int[a_steps][p];
        int pi;
//
        byte[] pixels_ip = (byte[]) ip_byte_processed.getPixels();
        byte pixels[] = new byte[a_steps*p];
        int i=0;
        for (int row=0; row<height; row++)
            for (int col=0; col<width; col++)
            {
                if (pixels_ip[row*width+col] == -1){
//                    System.out.println("row:" +row+"col: "+col + "val: "+ pixels_ip[col*row+col]);
                    for(int a=0; a<a_steps; a++){
                        pi= (int)(col * Math.cos(a*Math.PI/180)+ row * Math.sin(a*Math.PI/180));
//                        pixels[(pi+p_h)*a+a]++;
                        pixels[(a*p)+(pi+p_h)]++;
                        akkumulator[a][pi+p_h]++;
                    }
                }
                i++;
            }

        byte[] GPL = new byte[a_steps*p];
//        Für alle Akkumulatorzellen Accu[αi][ρj] Wenn Accu[αi][ρj] > θ und Accu[αi][ρj] > Accu[jede Nachbarzelle] Dann Füge (αi,ρj) zu GPL hinzu
        for(int a=1;a<a_steps-1;a++)
            for(pi=1; pi<p-1; pi++){
                if(akkumulator[a][pi]>100 &&
                        akkumulator[a][pi]>akkumulator[a][pi+1] &&
                        akkumulator[a][pi]>akkumulator[a+1][pi+1] &&
                        akkumulator[a][pi]>akkumulator[a+1][pi] &&
                        akkumulator[a][pi]>akkumulator[a-1][pi] &&
                        akkumulator[a][pi]>akkumulator[a][pi-1] &&
                        akkumulator[a][pi]>akkumulator[a-1][pi-1] &&
                        akkumulator[a][pi]>akkumulator[a-1][pi+1] &&
                        akkumulator[a][pi]>akkumulator[a+1][pi-1]){
                    GPL[a*p+pi] = (byte)akkumulator[a][pi];
                }
        }



//        p-x*cos(a)/sin(a)=y


        for (i=0; i<1; i++)
            for (int row=0; row<height; row++)
                for (int col=0; col<width; col++){


	    }

	    ByteProcessor gpl = new ByteProcessor(p, a_steps, GPL);
//
        ByteProcessor ht = new ByteProcessor(p, a_steps, pixels);

		ImagePlus impOverlay = new ImagePlus("Canny", ip_byte_processed);

        ImagePlus impOverlay2 = new ImagePlus("ht", ht);

        ImagePlus impOverlay3 = new ImagePlus("gpl", gpl);




        impOverlay.show();
        impOverlay2.show();
        impOverlay3.show();
		
		
	}
		
	@Override
	public int setup(String arg0, ImagePlus arg1) {
		return DOES_ALL + NO_UNDO + NO_CHANGES;
	}
}
