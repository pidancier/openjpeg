package org.openJpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.codecCentral.imageio.generic.NativeUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class OpenJPEGJavaDecoderTest
{
   private NativeUtilities _utilities = new NativeUtilities();
   private static final int  TEMP_BUFFER_SIZE = 1024*1024;
   String filesource = "test.jp2";
   String working_file;
   
   @BeforeClass
   public void init() throws IOException
   {
      InputStream is = ClassLoader.getSystemResourceAsStream(filesource);
      File w_file = File.createTempFile("jp2k", ".jp2");
      FileOutputStream fos = new FileOutputStream(w_file);
      try
      {
         byte[] tempBuffer = new byte[TEMP_BUFFER_SIZE];
         int bytesRead;
         while ((bytesRead=is.read(tempBuffer, 0, TEMP_BUFFER_SIZE))!=-1)
            fos.write(tempBuffer,0, bytesRead);
      }
      finally
      {
         try
         {
            if (is!=null) is.close();
            if (fos!=null) fos.close();
         }
         catch (IOException e)
         {
         }
      }
      working_file = w_file.getPath();
      
      _utilities.loadLibraries(Arrays.asList("openjp2"));
   }
   
   @AfterClass
   public void exit()
   {
      new File(working_file).delete();
   }
   
   @Test
   public void internalGetHeader()
   {
      OpenJPEGJavaDecoder decoder = new OpenJPEGJavaDecoder();
      decoder.header(working_file);
      Assert.assertNotEquals(decoder.getWidth(), -1);
      Assert.assertNotEquals(decoder.getHeight(), -1);
      Assert.assertNotEquals(decoder.getDepth(), -1);
      
      System.out.println("Widh   =" + decoder.getWidth());
      System.out.println("Height =" + decoder.getHeight());
      System.out.println("Depth  =" + decoder.getDepth());
   }
}
