package fr.gael.openjpeg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

public class OpenJpegDecoder
{

   private static boolean loaded = false;
   /**
    * Performs library loading only once, and avoid
    * concurrency calls.
    */
   private static synchronized void loadLibraries ()
   {
      // Library already loaded.
      if (loaded) return;

      try
      {
         System.loadLibrary ("openjp2");
      }
      catch (Exception e)
      {
         throw new IllegalStateException (
               "Cannot load OpenJpeg libraries", e);
      }
      finally
      {
         loaded=true;
      }
   }

   private static byte[] getBytesFromFile (File file) throws IOException
   {
      if (file == null || !file.exists () || !file.isFile ())
      {
         throw new IllegalArgumentException ("Invalid file.");
      }

      if (file.length () > Integer.MAX_VALUE)
      {
         throw new IllegalArgumentException ("file too long : " + file);
      }

      byte[] buffer = new byte[(int) file.length ()];
      FileInputStream fis = new FileInputStream (file);
      try
      {
         fis.read (buffer, 0, buffer.length);
      }
      finally
      {
         fis.close ();
      }

      return buffer;
   }

   // Logger
   private final Logger logger = Logger.getLogger (OpenJpegDecoder.class);

   // Encoded image
   private byte[] byteInputStream = null;

   // Decoded image
   private byte[] image8 = null;
   private short[] image16 = null;
   private int[] image24 = null;

   // Decoded image properties
   private int width = -1;
   private int height = -1;
   private int precision = -1;
   private int componentsNumber = -1;

   public OpenJpegDecoder ()
   {
      loadLibraries ();
   }

   public byte[] getImage8 ()
   {
      return image8;
   }

   public void setImage8 (byte[] image8)
   {
      this.image8 = image8;
   }

   public short[] getImage16 ()
   {
      return image16;
   }

   public void setImage16 (short[] image16)
   {
      this.image16 = image16;
   }

   public int[] getImage24 ()
   {
      return image24;
   }

   public void setImage24 (int[] image24)
   {
      this.image24 = image24;
   }

   public byte[] getByteInputStream ()
   {
      return byteInputStream;
   }

   public void setByteInputStream (byte[] compressedStream)
   {
      this.byteInputStream = compressedStream;
   }

   public long getByteInputStreamLength ()
   {
      long length;
      if (byteInputStream == null)
      {
         length = -1;
      }
      else
      {
         length = byteInputStream.length;
      }
      return length;
   }

   public int getWidth ()
   {
      return width;
   }

   public void setWidth (int width)
   {
      this.width = width;
   }

   public int getHeight ()
   {
      return height;
   }

   public void setHeight (int height)
   {
      this.height = height;
   }

   public int getPrecision ()
   {
      return precision;
   }

   public void setPrecision (int precision)
   {
      this.precision = precision;
   }

   public int getComponentsNumber ()
   {
      return componentsNumber;
   }

   public void setComponentsNumber (int componentsNumber)
   {
      this.componentsNumber = componentsNumber;
   }

   /**
    * Reset decoder
    */
   public void reset ()
   {
      image8 = null;
      image16 = null;
      image24 = null;
      width = -1;
      height = -1;
      precision = -1;
   }

   public int getDepth ()
   {
      return ((precision + 7) / 8) * componentsNumber;
   }

   /**
    * Represents result of decoded image.
    * Called by the native code.
    */
   protected void alloc8 ()
   {
      int length = width * height;

      if ((image8 == null || image8.length != length) && getDepth () == 1)
      {
         image8 = new byte[length];
      }
   }

   /**
    *
    */
   protected void alloc16 ()
   {
      int length = width * height;

      if ((image16 == null || image16.length != length) && getDepth () == 2)
      {
         image16 = new short[length];
      }
   }

   /**
    *
    */
   protected void alloc24 ()
   {
      int length = width * height;

      if ((image24 == null || image24.length != length) && getDepth () == 3)
      {
         image24 = new int[length];
      }
   }

   public boolean decode (String filename)
   {
      reset ();

      if (filename != null)
      {
         try
         {
            setByteInputStream (getBytesFromFile (new File (filename)));
         }
         catch (IOException e)
         {
            logger.error ("Cannot convert to byte array the file: " + filename,
                  e);
         }
      }

      return internalOpenJpegDecode ();
   }

   public boolean decodeHeader (String filename)
   {
      reset ();
      if (filename != null)
      {
         try
         {
            setByteInputStream (getBytesFromFile (new File (filename)));
         }
         catch (IOException e)
         {
            logger.error ("Cannot convert to byte array the file: " + filename,
                  e);
         }
      }
      return internalOpenJpegDecodeHeader ();
   }

   /**
    * Decodes only the header of Jpeg2000 image.
    *
    * @return true
    */
   private native boolean internalOpenJpegDecodeHeader ();

   /**
    * Decodes completly the Jpeg2000 image.
    *
    * @return true if inputImage is decoded properly, otherwise false.
    */
   private native boolean internalOpenJpegDecode ();

}
