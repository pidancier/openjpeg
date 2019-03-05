package fr.gael.openjpeg.imageio;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;

public class OpenJpegImageReaderSpi extends ImageReaderSpi
{
   private static final Logger LOGGER =
         Logger.getLogger (OpenJpegImageReaderSpi.class);

   private static final String VENDOR = "GAEL Systems";
   private static final String VERSION = "0.0.1";
   private static final String[] NAMES =
         {"jpeg2000", "jpeg 2000", "JPEG 2000", "JPEG2000"};
   private static final String[] SUFFIXES = {"jp2", "jp2k", "j2k", "j2c"};
   private static final String[] MIME_TYPES =
         {"image/jp2", "image/jp2k", "image/j2k", "image/j2c"};
   private static final Class[] INPUT_TYPES =
         {File.class, byte[].class, ImageInputStream.class};
   private static final String[] WRITER_SPI_NAMES = null;
   private static final boolean SUPPORTS_STREAM_METADATA = false;
   private static final String NAT_STREAM_METADATA_NAME = null;
   private static final String NAT_STREAM_METADATA_CLASS_NAME = null;
   private static final String[] EXT_STREAM_METADATA_FORMAT_NAMES = null;
   private static final String[] EXT_STREAM_METADATA_FORMAT_CLASS_NAMES = null;
   private static final boolean SUPPORTS_IMAGE_METADATA = false;
   private static final String NAT_IMAGE_METADATA_NAME = null;
   private static final String NAT_IMAGE_METADATA_CLASS_NAME = null;
   private static final String[] EXT_IMAGE_METADATA_NAMES = null;
   private static final String[] EXT_IMAGE_METADATA_CLASS_NAMES = null;

   private static final int[] magic = {0x00,0x00,0x00,0x0C,0x6A,0x50,0x20,0x20,0x0D,0x0A,0x87,0x0A,0x00,0x00,0x00,0x14,0x66,0x74,0x79,0x70,0x6A,0x70,0x32};

   public OpenJpegImageReaderSpi ()
   {
      super (VENDOR,
            VERSION,
            NAMES,
            SUFFIXES,
            MIME_TYPES,
            OpenJpegImageReader.class.getName (),
            INPUT_TYPES,
            WRITER_SPI_NAMES,
            SUPPORTS_STREAM_METADATA,
            NAT_STREAM_METADATA_NAME,
            NAT_STREAM_METADATA_CLASS_NAME,
            EXT_STREAM_METADATA_FORMAT_NAMES,
            EXT_STREAM_METADATA_FORMAT_CLASS_NAMES,
            SUPPORTS_IMAGE_METADATA,
            NAT_IMAGE_METADATA_NAME,
            NAT_IMAGE_METADATA_CLASS_NAME,
            EXT_IMAGE_METADATA_NAMES,
            EXT_IMAGE_METADATA_CLASS_NAMES);
   }

   @Override
   public boolean canDecodeInput (Object source) throws IOException
   {
      if (source == null)
      {
         return false;
      }

      boolean isDecodable = false;
      if (source instanceof File)
      {
         File file = (File) source;
         for (String suffix : SUFFIXES)
         {
            if (isDecodable == false)
            {
               isDecodable = file.getName().endsWith(suffix);
            }
         }
      }
      else if (source instanceof byte[])
      {     
         // Checking JP2K magic number         
         byte[] sce = (byte[]) source;
         for (int i = 0; i < 23; i++)
         {
            if ((byte)magic[i] != sce[i])
            {
               return false;
            }
         }
         return true;
      }
      else if (source instanceof ImageInputStream)
      {
         // Checking JP2K magic number
         ImageInputStream sce = (ImageInputStream) source;
         for (int i = 0; i < 23; i++)
         {
            if (magic[i] != sce.read())
            {
               sce.reset ();
               return false;
            }
         }
         sce.reset ();
         return true;
      }

      return isDecodable;
   }

   @Override
   public ImageReader createReaderInstance (Object extension) throws IOException
   {
      return new OpenJpegImageReader (this);
   }

   @Override
   public String getDescription (Locale locale)
   {
      return new StringBuilder ("ImageIO OpenJpeg Image Reader version ")
            .append (VERSION).append (" by ").append (VENDOR).toString ();
   }
}
