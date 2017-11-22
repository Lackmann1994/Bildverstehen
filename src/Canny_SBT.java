
// Andreas Siebert, Oct 2017
// Canny edge detection

import ij.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;

public class Canny_SBT implements PlugInFilter
{
    public static String INPUT_DIR = "... your directory here ...";
    public static String INPUT_IMA = "Rubik.jpg";

    public static final float SIGMA = 1.0f;

    public static final byte FOREGROUND_BYTE = (byte) 255;

    //**********************************************************************
    public int setup(String arg, ImagePlus imp)
    {
        return DOES_8G + DOES_RGB + NO_UNDO + NO_CHANGES;
    }

    //**********************************************************************
    public void run(ImageProcessor ip)
    {

        ByteProcessor ip_byte;
        if (ip instanceof ColorProcessor)
        {
            ColorProcessor.setWeightingFactors(0.299, 0.587, 0.114);
            ip_byte = (ByteProcessor) ip.convertToByte(false);
        }
        else
            ip_byte = (ByteProcessor) ip;

        ByteProcessor ip_canny = canny(ip_byte, SIGMA);

        ImagePlus imp_grad = new ImagePlus("Canny", ip_canny);
        imp_grad.show();

    }

    //**********************************************************************
    public ByteProcessor canny(ByteProcessor ip, float sigma)
    {
        FloatProcessor[] ips_gradient = gaussianGradientConditional(ip, sigma);

        // ImagePlus imp_grad = new ImagePlus("Gradient_sigma_" + sigma, ips_gradient[0]);
        // imp_grad.show();

        FloatProcessor ip_nms = nonMaximumSuppression4(ips_gradient[0], ips_gradient[3]);
        // ImagePlus imp_nms = new ImagePlus("NMS", ip_nms);
        // imp_nms.show();

        ByteProcessor ip_canny = hysteresis(ip_nms, 0, 0);

        return ip_canny;
    }

    //**********************************************************************
    // return magnitude image, x-gradient, and y-gradient, directional image
    FloatProcessor[] gaussianGradientConditional(ByteProcessor ip, float sigma)
    {
        int filterSize = 2 * (int) Math.ceil(2.0*sigma) + 1;;

        ImageProcessor ip_ext = boundaryTreatment(ip, filterSize);

        float[] gaussDeriv_x = createGaussEdgeFilter_1d(sigma, filterSize, 0);
        float[] gaussDeriv_y = createGaussEdgeFilter_1d(sigma, filterSize, 1);

        FloatProcessor ip_grad_x = convolveSeparated(ip_ext, gaussDeriv_x, gaussDeriv_y);
        FloatProcessor ip_grad_y = convolveSeparated(ip_ext, gaussDeriv_y, gaussDeriv_x);

        FloatProcessor ip_magn   = computeMagnitude(ip_grad_x, ip_grad_y);
        FloatProcessor ip_direct = computeDirectionCondi_4(ip_grad_x, ip_grad_y, 0);  // just four directions

        FloatProcessor[] ip_results = new FloatProcessor[4];
        ip_results[0] = ip_magn;
        ip_results[1] = ip_grad_x;
        ip_results[2] = ip_grad_y;
        ip_results[3] = ip_direct;

        return ip_results;
    }

    //**********************************************************************
    // create _separable_ Gaussian derivative edge detector kernel
    // split G_x(x,y) into separate parts, with extra=-x for direction=0 (vertical)
    float[] createGaussEdgeFilter_1d(float sigma, int filterSize, int direction)
    {
        int center = filterSize/2;
        float sigma_sq = sigma*sigma;
        float two_sigma_sq = 2.0f * sigma_sq;
        float factor = 1.0f / (float)(Math.sqrt(2.0f * Math.PI) * sigma_sq);

        float[] gaussian = new float[filterSize];

        if (direction == 0)  // vertical
            for (int i=0; i<filterSize; i++)
                gaussian[i] = (i-center) * factor * (float) Math.exp(-(i-center)*(i-center)/two_sigma_sq);
        else
            for (int i=0; i<filterSize; i++)
                gaussian[i] =              factor * (float) Math.exp(-(i-center)*(i-center)/two_sigma_sq);

        return gaussian;
    }

    //**********************************************************************
    // convolve image with separable 1d filters filter_x, filter_y
    // in this implementation, filter_x and filter_y have to be of equal size
    // result image is smaller than input image, ie extend image before calling
    FloatProcessor convolveSeparated(ImageProcessor ip, float[] filter_x, float[] filter_y)
    {

        FloatProcessor ip_conv = new FloatProcessor(ip.getWidth()-filter_x.length+1, ip.getHeight()-filter_y.length+1);
        float[] pixels_conv = (float[]) ip_conv.getPixels();
        float[][] intermediate = new float[ip.getWidth()-filter_x.length+1][ip.getHeight()];

        if (ip instanceof ByteProcessor)
        {
            byte[] pixels_ip = (byte[]) ip.getPixels();
            // horizontal filtering
            for (int row=0; row<ip.getHeight(); row++)
                for (int col=0; col<ip_conv.getWidth(); col++)
                {
                    float sum = 0.0f;
                    int basis_ip = row*ip.getWidth()+col;
                    for (int i=0; i<filter_x.length; i++)
                        sum += (pixels_ip[basis_ip++] & 0xff) * filter_x[filter_x.length-i-1];  // NOTE: & 0xff for ByteProcessor
                    intermediate[col][row] = sum;
                }
        }

        else if (ip instanceof FloatProcessor)
        {
            float[] pixels_ip = (float[]) ip.getPixels();
            // horizontal filtering
            for (int row=0; row<ip.getHeight(); row++)
                for (int col=0; col<ip_conv.getWidth(); col++)
                {
                    float sum = 0.0f;
                    int basis_ip = row*ip.getWidth()+col;
                    for (int i=0; i<filter_x.length; i++)
                        sum += (pixels_ip[basis_ip++]) * filter_x[filter_x.length-i-1];
                    intermediate[col][row] = sum;
                }
        }

        // vertical filtering
        int basis_conv=0;
        for (int row=0; row<ip_conv.getHeight(); row++)
            for (int col=0; col<ip_conv.getWidth(); col++)
            {
                float sum = 0.0f;
                for (int j=0; j<filter_y.length; j++)
                    sum += intermediate[col][row+j] * filter_y[filter_y.length-j-1];
                pixels_conv[basis_conv++] = sum;
            }
        return ip_conv;
    }

    //**********************************************************************
    // compute magnitude floating image, given the x- and y-directional values
    static FloatProcessor computeMagnitude(FloatProcessor ip_x, FloatProcessor ip_y)
    {
        FloatProcessor ip_magn = new FloatProcessor(ip_x.getWidth(), ip_x.getHeight());
        float[] pixels_magn = (float[]) ip_magn.getPixels();
        float[] pixels_x    = (float[]) ip_x.getPixels();
        float[] pixels_y    = (float[]) ip_y.getPixels();

        for (int k=0; k<ip_x.getWidth()*ip_x.getHeight(); k++)
        {
            pixels_magn[k] = (float) Math.sqrt(pixels_x[k]*pixels_x[k] + pixels_y[k]*pixels_y[k]);
        }

        return ip_magn;
    }

    //**********************************************************************
    // compute direction floating image, given the x- and y-directional values,
    static FloatProcessor computeDirectionCondi_4(FloatProcessor ip_x, FloatProcessor ip_y, float threshold)
    {
        float thresh_sq = threshold * threshold;

        FloatProcessor ip_direct = new FloatProcessor(ip_x.getWidth(), ip_x.getHeight());
        float[] pixels_direct = (float[]) ip_direct.getPixels();
        float[] pixels_x = (float[]) ip_x.getPixels();
        float[] pixels_y = (float[]) ip_y.getPixels();

        for (int k=0; k<ip_x.getWidth()*ip_x.getHeight(); k++)
        {
            float dist = pixels_x[k]*pixels_x[k] + pixels_y[k]*pixels_y[k];
            if (dist > thresh_sq)
                pixels_direct[k] = quantizeDirection(pixels_x[k], pixels_y[k]);
        }

        return ip_direct;
    }

    //**********************************************************************
    // directions are quantized into only 4+1 bins:
    // 0 = invalid, 1 = horizontal, 2 = diagonal_1, 3 = vertical, 4 = diagonal_2 (left up)
    static float quantizeDirection(float dir_x, float dir_y)
    {
        if (dir_x == 0.0f)
            if (dir_y == 0.0f)
                return 0.0f;
            else
                return 1.0f;  // edge horizontal (i.e. gradient vertical)
        else
        {
            // alpha = Math.atan(pixels_y[k] / pixels_x[k]);  // -Pi/2 < alpha < Pi/2
            float tan_alpha = dir_y / dir_x;  //  -\infty < tan_alpha < \infty
            if (tan_alpha > 2.41421356f)  // 67.5°
                return 1.0f;  // edge horizontal (ie gradient vertical)
            else if (tan_alpha >  0.41421356f)  // 22.5°
                return 2.0f;  // diagonal_1 = edge right up
            else if (tan_alpha > -0.41421356f)  // -22.5°
                return 3.0f;  // edge vertical (ie gradient horizontal
            else if (tan_alpha > -2.41421356f)  // -67.5°
                return 4.0f;  // diagonal_2 = edge left up)
            else
                return 1.0f;  // edge horizontal
        }
    }

    //**********************************************************************
    // return magnitude image with nonmaximum values eliminated
    // the two neighbors to compare with are derived from the direction image
    // NOTE: ip_direct must contain directions quantified to just 4 values (horizontal, diagonal, vertical)
    FloatProcessor nonMaximumSuppression4(FloatProcessor ip_magn, FloatProcessor ip_direct)
    {
        FloatProcessor ip_nms = new FloatProcessor(ip_magn.getWidth(), ip_magn.getHeight());
        float[] pixels_magn   = (float[]) ip_magn.getPixels();
        float[] pixels_direct = (float[]) ip_direct.getPixels();
        float[] pixels_nms    = (float[]) ip_nms.getPixels();

        for (int i=0; i<pixels_magn.length; i++)
            if (pixels_direct[i] > 0)  // zero = direction undefined since gradient too weak
                if (isLocalMaximum4(ip_magn, i, pixels_direct[i]))
                    pixels_nms[i] = pixels_magn[i];

        return ip_nms;
    }

    //**********************************************************************
    // direction = edge direction, ie gradient direction is orthogonal
    // To solve a tie, use >= to the left/top, but > to the right/bottom
    // NOTE: direct must contain a direction quantified to just 4 values (horizontal, diagonal, vertical)
    boolean isLocalMaximum4(FloatProcessor ip_magn, int index, float direct)
    {
        float[] pixels_magn = (float[]) ip_magn.getPixels();

        if (direct == 1.0f) // edge horizontal, gradient vertical
        {
            if (index<ip_magn.getWidth()) // pixel of interest is in first row, ie it has no pixel above
                if (pixels_magn[index] > pixels_magn[index+ip_magn.getWidth()])
                    return true;
                else
                    return false;
            else if (index/ip_magn.getWidth()==(ip_magn.getHeight()-1)) // pixel of interest is in last row, ie it has no pixel below
                if (pixels_magn[index] >= pixels_magn[index-ip_magn.getWidth()])
                    return true;
                else
                    return false;
            else
            if ((pixels_magn[index] >= pixels_magn[index-ip_magn.getWidth()]) &&
                    (pixels_magn[index] >  pixels_magn[index+ip_magn.getWidth()]))
                return true;
            else
                return false;
        }

        else if (direct == 2.0f) // edge diagonal right up
        {
            // small simplification: allow diagonal edges to be kept only if they are not on the image boundary
            if ((index>=ip_magn.getWidth()) && (index<(ip_magn.getHeight()-1)*ip_magn.getWidth()) &&
                    (index%ip_magn.getWidth()>0) && ((index+1)%ip_magn.getWidth()>0))
                if ((pixels_magn[index] >= pixels_magn[index-ip_magn.getWidth()-1]) &&
                        (pixels_magn[index] >  pixels_magn[index+ip_magn.getWidth()+1]))
                    return true;
                else
                    return false;
        }

        else if (direct == 3.0f) // edge vertical, gradient horizontal
        {
            if (index%ip_magn.getWidth()==0)  // pixel of interest is in first column, ie it has no pixel to the left
                if (pixels_magn[index] > pixels_magn[index+1])
                    return true;
                else
                    return false;
            else if ((index+1)%ip_magn.getWidth()==0)  // pixel of interest is in last column, ie it has no pixel to the right
                if (pixels_magn[index] >= pixels_magn[index-1])
                    return true;
                else
                    return false;
            else
            if ((pixels_magn[index] >= pixels_magn[index-1]) &&
                    (pixels_magn[index] > pixels_magn[index+1]))
                return true;
            else
                return false;
        }

        else if (direct == 4.0f) // edge diagonal left up
        {
            // small simplification: allow diagonal edges to be kept only if they are not on the image boundary
            if ((index>=ip_magn.getWidth()) && (index<(ip_magn.getHeight()-1)*ip_magn.getWidth()) &&
                    (index%ip_magn.getWidth()>0) && ((index+1)%ip_magn.getWidth()>0))
                if ((pixels_magn[index] >= pixels_magn[index-ip_magn.getWidth()+1]) &&
                        (pixels_magn[index] >  pixels_magn[index+ip_magn.getWidth()-1]))
                    return true;
                else
                    return false;
        }

        return false;
    }

    //**********************************************************************
    // hysteresis thresholding as suggested by Canny
    // if hyst_high == 0, derive hyst_high from mean of gradients; hyst_low = 0.5* hyst_high
    ByteProcessor hysteresis(FloatProcessor ip_nms, float hyst_low, float hyst_high)
    {
        ByteProcessor ip_canny = new ByteProcessor(ip_nms.getWidth(), ip_nms.getHeight());
        float[] pixels_nms  = (float[]) ip_nms.getPixels();
        byte[] pixels_canny = (byte[]) ip_canny.getPixels();

        if (hyst_high == 0)  // derive hyst_high from mean of gradients
        {
            float meanGrad = 0;
            int gradCount = 0;
            for (int i=0; i<pixels_nms.length; i++)
            {
                if (pixels_nms[i] > 1.0)  // ignore tiny gradients
                {
                    meanGrad += pixels_nms[i];
                    gradCount++;
                }
            }
            hyst_high = meanGrad/gradCount;  // just some heuristic
            hyst_low  = 0.5f*hyst_high;		 // the  next heuristic

            // System.out.println("hysteresis high = " + hyst_high + "   low = " + hyst_low);
        }

        // mark all pixels above hyst_high in result image
        for (int i=0; i<pixels_nms.length; i++)
        {
            if (pixels_nms[i] >= hyst_high)
                pixels_canny[i] = FOREGROUND_BYTE;
        }

        boolean hasChanged = true;
        while (hasChanged == true)
        {
            hasChanged = false;
            int index = 0;
            for (int row=0; row<ip_nms.getHeight(); row++)
                for (int col=0; col<ip_nms.getWidth(); col++)
                {
                    if ((pixels_canny[index] == 0) && (pixels_nms[index] >= hyst_low) && (neighborExists(ip_canny, index, row, col) == true))
                    {
                        pixels_canny[index] = FOREGROUND_BYTE;
                        hasChanged = true;
                    }
                    index++;
                }
        }

        return ip_canny;
    }

    //**********************************************************************
    // check 8-neighborhood of index for set pixel
    boolean neighborExists(ImageProcessor ip, int index, int row, int col)
    {
        byte[] pixels = (byte[]) ip.getPixels();
        if (((col>0) && (pixels[index-1] == FOREGROUND_BYTE)) ||
                ((col<ip.getWidth()-1) && (pixels[index+1] == FOREGROUND_BYTE)) ||
                ((col>0) && (row>0) && (pixels[index-ip.getWidth()-1] == FOREGROUND_BYTE)) ||
                ((row>0) && (pixels[index-ip.getWidth()] == FOREGROUND_BYTE)) ||
                ((row>0) && (col<ip.getWidth()-1) && (pixels[index-ip.getWidth()+1] == FOREGROUND_BYTE)) ||
                ((col>0) && (row<ip.getHeight()-1) && (pixels[index+ip.getWidth()-1] == FOREGROUND_BYTE)) ||
                ((row<ip.getHeight()-1) && (pixels[index+ip.getWidth()] == FOREGROUND_BYTE)) ||
                ((col<ip.getWidth()-1) && (row<ip.getHeight()-1) && (pixels[index+ip.getWidth()+1] == FOREGROUND_BYTE)))
            return true;

        return false;
    }

    //**********************************************************************
    // extend image to all sides by half filtersize, fill  by mirroring
    // return extended image
    ImageProcessor boundaryTreatment(ImageProcessor ip, int filterSize)
    {
        int halfSize = filterSize/2;
        int targetWidth = ip.getWidth()+2*halfSize;
        int targetHeight = ip.getHeight()+2*halfSize;
        ImageProcessor ip_ext;

        if (ip instanceof ByteProcessor)
        {
            ip_ext = new ByteProcessor(targetWidth, targetHeight);
            byte[] pixels = (byte[]) ip.getPixels();
            byte[] pixels_ext = (byte[]) ip_ext.getPixels();

            // copy original pixels into center of extended image
            for (int i=0; i<ip.getHeight(); i++)
            {
                int low = (i+halfSize)*ip_ext.getWidth()+halfSize;
                System.arraycopy(pixels, i*ip.getWidth(), pixels_ext, low, ip.getWidth());
            }

            // mirroring
            for (int i=halfSize; i<ip.getHeight()+halfSize; i++)
            {
                // left
                int ip_ext_pos_lr = i*ip_ext.getWidth()+halfSize;
                int ip_ext_pos_ll = ip_ext_pos_lr-1;
                // right
                int ip_ext_pos_rr = (i+1)*ip_ext.getWidth()-halfSize;
                int ip_ext_pos_rl = ip_ext_pos_rr-1;
                for (int j=0; j<halfSize; j++)
                {
                    pixels_ext[ip_ext_pos_ll--] = pixels_ext[ip_ext_pos_lr++];
                    pixels_ext[ip_ext_pos_rr++] = pixels_ext[ip_ext_pos_rl--];
                }
            }

            // top/bottom side
            for (int i=0; i<halfSize; i++)
            {
                System.arraycopy(pixels_ext, (halfSize+i)*ip_ext.getWidth(), pixels_ext, (halfSize-i-1)*ip_ext.getWidth(), ip_ext.getWidth());
                System.arraycopy(pixels_ext, (ip_ext.getHeight()-halfSize-i-1)*ip_ext.getWidth(), pixels_ext, (ip_ext.getHeight()-halfSize+i)*ip_ext.getWidth(), ip_ext.getWidth());
            }
        }

        else if (ip instanceof FloatProcessor)
        {
            ip_ext = new FloatProcessor(targetWidth, targetHeight);
            float[] pixels = (float[]) ip.getPixels();
            float[] pixels_ext = (float[]) ip_ext.getPixels();

            // copy original pixels into center of extended image
            for (int i=0; i<ip.getHeight(); i++)
            {
                int low = (i+halfSize)*ip_ext.getWidth()+halfSize;
                System.arraycopy(pixels, i*ip.getWidth(), pixels_ext, low, ip.getWidth());
            }

            // mirroring
            for (int i=halfSize; i<ip.getHeight()+halfSize; i++)
            {
                // left
                int ip_ext_pos_lr = i*ip_ext.getWidth()+halfSize;
                int ip_ext_pos_ll = ip_ext_pos_lr-1;
                // right
                int ip_ext_pos_rr = (i+1)*ip_ext.getWidth()-halfSize;
                int ip_ext_pos_rl = ip_ext_pos_rr-1;
                for (int j=0; j<halfSize; j++)
                {
                    pixels_ext[ip_ext_pos_ll--] = pixels_ext[ip_ext_pos_lr++];
                    pixels_ext[ip_ext_pos_rr++] = pixels_ext[ip_ext_pos_rl--];
                }
            }

            // top/bottom side
            for (int i=0; i<halfSize; i++)
            {
                System.arraycopy(pixels_ext, (halfSize+i)*ip_ext.getWidth(), pixels_ext, (halfSize-i-1)*ip_ext.getWidth(), ip_ext.getWidth());
                System.arraycopy(pixels_ext, (ip_ext.getHeight()-halfSize-i-1)*ip_ext.getWidth(), pixels_ext, (ip_ext.getHeight()-halfSize+i)*ip_ext.getWidth(), ip_ext.getWidth());
            }
        }

        else if (ip instanceof ColorProcessor) // ColorProcessor
        {
            ip_ext = new ColorProcessor(targetWidth, targetHeight);
            int[] pixels = (int[]) ip.getPixels();
            int[] pixels_ext = (int[]) ip_ext.getPixels();

            // copy original pixels into center of extended image
            for (int i=0; i<ip.getHeight(); i++)
            {
                int low = (i+halfSize)*ip_ext.getWidth()+halfSize;
                System.arraycopy(pixels, i*ip.getWidth(), pixels_ext, low, ip.getWidth());
            }

            // mirroring
            for (int i=halfSize; i<ip.getHeight()+halfSize; i++)
            {
                // left
                int ip_ext_pos_lr = i*ip_ext.getWidth()+halfSize;
                int ip_ext_pos_ll = ip_ext_pos_lr-1;
                // right
                int ip_ext_pos_rr = (i+1)*ip_ext.getWidth()-halfSize;
                int ip_ext_pos_rl = ip_ext_pos_rr-1;
                for (int j=0; j<halfSize; j++)
                {
                    pixels_ext[ip_ext_pos_ll--] = pixels_ext[ip_ext_pos_lr++];
                    pixels_ext[ip_ext_pos_rr++] = pixels_ext[ip_ext_pos_rl--];
                }
            }

            // top/bottom side
            for (int i=0; i<halfSize; i++)
            {
                System.arraycopy(pixels_ext, (halfSize+i)*ip_ext.getWidth(), pixels_ext, (halfSize-i-1)*ip_ext.getWidth(), ip_ext.getWidth());
                System.arraycopy(pixels_ext, (ip_ext.getHeight()-halfSize-i-1)*ip_ext.getWidth(), pixels_ext, (ip_ext.getHeight()-halfSize+i)*ip_ext.getWidth(), ip_ext.getWidth());
            }
        }
        else  // here, add code for ShortProcessor if you need it
        {
            return null;
        }

        return ip_ext;
    }

    //**********************************************************************
    public static void main(String[] args)
    {
        ImageJ myImageJ = new ImageJ();
        myImageJ.exitWhenQuitting(true);

        ImagePlus image = IJ.openImage(INPUT_DIR + INPUT_IMA);
        if (image == null)
            IJ.error("Couldn't open image " + INPUT_IMA);

        image.show();
        IJ.runPlugIn(image, "Canny_SBT", "");
    }
}

