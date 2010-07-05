/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pseudopattern.retinex;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 *
 * @author rortian
 */
public class Tester {
    public interface MLibrary extends Library {
        MLibrary INSTANCE = (MLibrary) Native.loadLibrary("m", MLibrary.class);

        double log(double x);
    }



    public static void main(String[] args){
        System.out.println(MLibrary.INSTANCE.log(1.0));
        for(int i=0;i<100;i++){
            System.out.println(i*0.01+":\t"+(Math.log(1+i*0.01)-MLibrary.INSTANCE.log(1+i*0.01)));
        }
        /*byte b = Byte.MAX_VALUE;
        byte all = -1;
        System.out.println(ByteImage.translate((byte)0));
        System.out.println(ByteImage.translate(255f));
        System.out.println(ByteImage.translate(128f));
        System.out.println(ByteImage.translate(129f));
        for(float f = -7;f<260;f+=1)
            System.out.println(ByteImage.translate(f));*/
    }

}
