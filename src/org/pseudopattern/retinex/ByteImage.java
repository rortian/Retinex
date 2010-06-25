/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pseudopattern.retinex;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rortian
 */
public class ByteImage {


    int scale,nscales,scales_mode,width,height,channelSize;
    double cvar;
    float weight,mean,vsquared,mini,maxi,range;
    double[] retinexScales;
    boolean hasAlpha;
    byte[] original;
    float[] dest,meanRaw,vsquaredRaw;
    int[] thirds;
    private static final byte maxByte = Byte.MAX_VALUE;

    private class Worker extends Thread {

        final int channel;
        byte[] data;
        GaussCoeff coefs;
        float[] in;



        Worker(int channel){
            super();
            this.channel = channel;

            in = new float[channelSize];
            for(int i=0;i<channelSize;i++){
                in[i] = translate(myByte(i))+1.0f;
            }


            coefs = new GaussCoeff();
        }

        private byte myByte(int i){
            if(hasAlpha){
                return original[1+channel+i*4];
            } else {
                return original[channel+i*3];
            }
        }

        private void addDest(int i,float f){
                dest[channel+i*3] += f;
        }


        @Override
        public void run(){
            for(int currentScale=0;currentScale<nscales;currentScale++){
                compute_coefs(retinexScales[currentScale]);
                for(int row=0;row<height;row++){
                    gaussSmooth(row*width,width,1);
                }
                for(int col=0;col<width;col++){

                    gaussSmooth(col,height,width);
                }
                for(int i=0;i<channelSize;i++){
                    addDest(i,(float)(weight*(Math.log(translate(myByte(i))+1)-Math.log(in[i]))));
                }
            }
        }


        private void gaussSmooth(int start,int num,int stride){
            int buffsize = num + 3;
            float[] w1,w2;
            w1 = new float[buffsize];
            w2 = new float[buffsize];
            int size = num - 1;
            w1[0] = in[start];
            w1[1] = in[start];
            w1[2] = in[start];
            for(int i=0,n=3;i<=size;i++,n++){
                w1[n] = (float)(coefs.B*in[i*stride+start]+
                        ((coefs.b[1]*w1[n-1]+
                        coefs.b[2]*w1[n-2]+
                        coefs.b[3]*w1[n-3])/coefs.b[0]));
            }

            w2[size+1]=w1[size+3];
            w2[size+2]=w1[size+3];
            w2[size+3]=w1[size+3];

            for(int i=size, n = i;i >=0; i--,n--){
                w2[n] = in[i*stride+start] = (float)(coefs.B*w1[n]+
                        ((coefs.b[1]*w2[n+1]+
                        coefs.b[2]*w2[n+2]+
                        coefs.b[3]*w2[n+3])/ coefs.b[0]));
            }
        }

        private void compute_coefs(double sigma){
            double q,q2,q3;
            if(sigma>=2.5){
                q = 0.98711 * sigma - 0.96330;
            } else if((sigma >= 0.5)){
                q = 3.97156 - 4.14554 *  Math.sqrt (1 - 0.26891 * sigma);
            } else {
                q = 0.1147705018520355224609375;
            }
            q2 = q*q;
            q3 = q*q2;
            coefs.b[0] = (1.57825+(2.44413*q)+(1.4281 *q2)+(0.422205*q3));
            coefs.b[1] = (        (2.44413*q)+(2.85619*q2)+(1.26661 *q3));
            coefs.b[2] = (                   -((1.4281*q2)+(1.26661 *q3)));
            coefs.b[3] = (                                 (0.422205*q3));
            coefs.B = 1.0-((coefs.b[1]+coefs.b[2]+coefs.b[3])/coefs.b[0]);
            coefs.sigma = sigma;
            coefs.N = 3;
        }
    }

    private class Summer extends Thread {

        int third;

        public Summer(int i){
            third = i;
        }

        @Override
        public void run(){
            int start = third*width*height;
            int stop = (third+1)*width*height;
            for(int i=start;i<stop;i++){
                meanRaw[third] +=  dest[i];
                vsquaredRaw[third] += dest[i]*dest[i];
            }
        }

    }

    private class Gainer extends Thread {

        int third;

        public Gainer(int i){
            third = i;
        }

        @Override
        public void run() {
            int start = 0, stop = 0;
            int three = width*height/3;
            switch (third) {
                case 0:
                    stop = three;
                    break;
                case 1:
                    start = three;
                    stop = 2 * three;
                    break;
                case 2:
                    start = 2 * three;
                    stop = width*height;
                    break;
            }
            float alpha = 128;
            float gain = 1;
            float offset = 0;
            for (int i = start; i < stop; i++) {

                int first = (hasAlpha) ? i * 4 + 1 : i * 3;

                int destFirst = i * 3;

                float logl = (float) Math.log(translate(original[first]) + translate(original[first + 1])
                        + translate(original[first + 2]) + 3.0);

                dest[destFirst] = gain * (float) (Math.log(alpha * (translate(original[first]) + 1) - logl) * dest[destFirst]) + offset;
                dest[destFirst + 1] = gain * (float) (Math.log(alpha * (translate(original[first + 1]) + 1) - logl) * dest[destFirst + 1]) + offset;
                dest[destFirst + 2] = gain * (float) (Math.log(alpha * (translate(original[first + 2]) + 1) - logl) * dest[destFirst + 2]) + offset;
            }
        }
    }

    private class Pixeler extends Thread {

        int third;

        public Pixeler(int i){
            third = i;
        }

        @Override
        public void run(){
            int start = 0, stop = 0;
            int three = width*height / 3;
            switch (third) {
                case 0:
                    stop = three;
                    break;
                case 1:
                    start = three;
                    stop = 2 * three;
                    break;
                case 2:
                    start = 2 * three;
                    stop = width*height;
                    break;
            }
            for(int i=start;i<stop;i++){
                int first = (hasAlpha) ? i * 4 + 1 : i * 3;
                int destFirst = i * 3;

                for(int j=0;j<3;j++){
                    float c = 255 *(dest[destFirst+j]-mini)/range;
                    original[first+j] = translate(c);
                }
            }
        }
    }


    public ByteImage(BufferedImage inputImage, HashMap<String, Number> options) {
        scale = options.get("scale").intValue();
        nscales = options.get("nscales").intValue();
        scales_mode = options.get("scales_mode").intValue();
        cvar = options.get("cvar").doubleValue();

        retinex_scales_distribution();
        width = inputImage.getWidth();
        height = inputImage.getHeight();
        channelSize = width*height;
        dest = new float[width*height*3];
        for(int i=0;i<dest.length;i++)
            dest[i] = 0;

        //System.out.println("Width\t"+width+"\tHeight\t"+height);
        
        //WritableRaster inRaster = inputImage.getRaster();
        Raster inRaster = inputImage.getRaster();
        
        DataBufferByte dbIn = (DataBufferByte) inRaster.getDataBuffer();

        original = dbIn.getData();

        /*/for(int i = 0;i<100;i++)
            System.out.print(original[i]+"\t");
        System.out.println();*/

        hasAlpha = (width*height*4 == original.length);
        //System.out.println(original.length+"\t"+hasAlpha);

        weight = (float) 1.0 / nscales;
        Worker[] workers = new Worker[3];
        for(int channel = 0; channel < 3; channel++){
            workers[channel] = new Worker(channel);
            workers[channel].start();
        }
        for(int i=0;i<3;i++)
            try {
            workers[i].join();
        } catch (InterruptedException ex) {
            Logger.getLogger(ByteImage.class.getName()).log(Level.SEVERE, null, ex);
        }

        Thread[] gainers = new Thread[3];

        for(int i=0;i<3;i++){
            gainers[i] = new Gainer(i);
            gainers[i].start();
        }


        try {
            for (int i = 0; i < 3; i++) {
                gainers[i].join();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(ByteImage.class.getName()).log(Level.SEVERE, null, ex);
        }




        mean = 0;
        vsquared = 0;

        meanRaw = new float[3];
        vsquaredRaw = new float[3];
        meanRaw[0] = vsquaredRaw[0] = 0;
        meanRaw[1] = vsquaredRaw[1] = 0;
        meanRaw[2] = vsquaredRaw[2] = 0;
        Thread[] summers = new Thread[3];
        for(int i=0;i<3;i++){
            summers[i] = new Summer(i);
            summers[i].start();
        }


        try {
            for (int i = 0; i < 3; i++) {
                summers[i].join();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(ByteImage.class.getName()).log(Level.SEVERE, null, ex);
        }


        mean = meanRaw[0] +  meanRaw[1] + meanRaw[2];
        vsquared = vsquaredRaw[0] +  vsquaredRaw[1] + vsquaredRaw[2];

        mean /= dest.length;
        vsquared /= dest.length;

        float var = (vsquared - (mean*mean));
        var = (float) Math.sqrt(var);

        mini = (float) (mean - cvar * var);
        maxi = (float) (mean + cvar * var);
        range = maxi - mini;

        /*System.out.println(range);
        System.out.println(var);
        System.out.println(mean);
        System.out.println(vsquared);
        System.out.println(cvar);*/

        Thread[] last = new Thread[3];
        for(int i=0;i<3;i++){
            last[i] = new Pixeler(i);
            last[i].start();
        }
        
        System.out.println("before");

        try {
            for(int i=0;i<3;i++)
                last[i].join();
        } catch (InterruptedException ex) {
            Logger.getLogger(ByteImage.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*for(int i = 0;i<100;i++)
            System.out.print(original[i]+"\t");
        System.out.println();*/
    }




    public static float translate(byte b) {
        if (b >= 0) {
            return b;
        }
        float toReturn = 128;
        return toReturn + (b & maxByte);
    }

    public static byte translate(float f){
        int i = Math.round(f);
        if(i<128){
            return (byte) i;
        }
        byte b = Byte.MIN_VALUE;
        return (byte) (b | ((byte) (i - 128)));
    }

    private void retinex_scales_distribution() {
        switch(nscales){
            case 1:
                retinexScales = new double[1];
                retinexScales[0] = (scale/2);
                break;
            case 2:
                retinexScales = new double[2];
                retinexScales[0] = (scale/2);
                retinexScales[1] = scale;
                break;
            default:
                double size_step = ((double)scale) / (double) nscales;
                retinexScales = new double[nscales];
                switch(scales_mode){
                    case Retinex.UNIFORM:
                        for(int i=0;i<nscales;i++)
                            retinexScales[i] = 2.0 + i*size_step;
                        break;
                    case Retinex.LOW:
                        size_step = Math.log(scale-2.0) / nscales;
                        for(int i=0;i<nscales;i++)
                            retinexScales[i] = 2.0 + Math.pow(10, (i * size_step)/ Math.log(10));
                        break;
                    case Retinex.HIGH:
                        size_step = Math.log(scale-2.0) / nscales;
                        for(int i=0;i<nscales;i++)
                            retinexScales[i] = nscales - Math.pow(10, (i * size_step)/ Math.log(10));
                        break;
                }
                break;
        }
    }

}
