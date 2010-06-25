/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pseudopattern.retinex;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 *
 * @author rortian
 */
public class Retinex {

    public static final  int UNIFORM = 0;
    public static final int LOW = 1;
    public static final int HIGH = 2;

    public static HashMap<String,Number> defaults;

    static {
        defaults = new HashMap<String,Number>();
        defaults.put("scale", 240);
        defaults.put("nscales", 3);
        defaults.put("scales_mode",UNIFORM);
        defaults.put("cvar", 1.2);
    }

    public static void retinex(File input,File output) throws IOException{
        retinex(input,output,defaults);
    }

    public static void retinex(File input,File output,String json){
        //Will put JSON option modifying stuff
    }

    public static void retinex(File input,File output,HashMap<String,Number> options) throws IOException {
        BufferedImage inputImage = ImageIO.read(input);

        Raster inRaster = inputImage.getData();
        DataBuffer buffer = inRaster.getDataBuffer();
        //System.out.println(buffer.getNumBanks()+"banks");
        DataBuffer writing = null;
        BufferedImage outie = null;
        switch (buffer.getDataType()){
            case DataBuffer.TYPE_BYTE:
                //System.out.println("byte");
                ByteImage bi = new ByteImage(inputImage,options);
                break;
            case DataBuffer.TYPE_DOUBLE:
                System.out.println("double");
                break;
            case DataBuffer.TYPE_FLOAT:
                System.out.println("float");
                break;
            case DataBuffer.TYPE_INT:
                System.out.println("int");
                break;
            case DataBuffer.TYPE_SHORT:
                System.out.println("short");
                break;
            case DataBuffer.TYPE_USHORT:
                System.out.println("ushort");
                break;
        }
        //inputImage.flush();
        ImageIO.write(inputImage, "png", output);
    }


    public static void main(String[] args) throws IOException {
        File in = new File("/mnt/storage/bigimage2/images/1277343811.png");
        File in2 = new File("/home/rortian/Pictures/jcexamstatus.png");
        File in3 = new File("/home/rortian/Pictures/1276502848-invert-retinex.png");
        File out = new File("/home/rortian/whoa.png");
        //System.out.println("1277343811.png");
        //retinex(in,null);
        System.out.println("jcexamstatus.png");
        retinex(in,out);
        //System.out.println("1276502848-invert-retinex.png");
        //retinex(in3,null);
    }

}
