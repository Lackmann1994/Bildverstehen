import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class Launcher {
	
	public static void main(String[]args){
		ImageJ myImageJ = new ImageJ();
		myImageJ.exitWhenQuitting(true);




		//Praktikum 1:
/*		String inputDir = "C:\\Users\\Alex\\IdeaProjects\\Bildverstehen\\img\\";
		String inputIma = "Rubik.jpg";
		ImagePlus image = IJ.openImage(inputDir + inputIma);
		if(image == null){
			IJ.error("cannot open image: " + inputIma);

		}
		IJ.runPlugIn(image, "Sobel_plugin", "");
		IJ.runPlugIn(image, "Canny_plugin", "");
*/


//		Praktikum 2:
		String inputDir = "C:\\Users\\Alex\\IdeaProjects\\Bildverstehen\\img\\";
		String inputIma = "Rubik.jpg";
		ImagePlus image = IJ.openImage(inputDir + inputIma);
		if(image == null){
			IJ.error("cannot open image: " + inputIma);

		}
		IJ.runPlugIn(image, "HT_plugin", "");
//		IJ.runPlugIn(image, "Canny_plugin", "");



	}

}




























