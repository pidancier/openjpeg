package fr.gael.openjpeg.imageio;

import fr.gael.openjpeg.OpenJpegDecoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
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
      OpenJpegDecoder decoder = new OpenJpegDecoder ();
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
         // TODO check can decode byte[]
         return true;
      }
      else if (source instanceof ImageInputStream)
      {
         // TODO check can decode ImageInputStream
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
