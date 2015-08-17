/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public abstract class GenericImageReader extends ImageReader {

	/** Size of the Temp Buffer, used when reading from an image input stream */
	private final static int TEMP_BUFFER_SIZE = 64 * 1024;

	/** The dataset input source */
	private File inputFile = null;
	
	/** The data input source name */
	private String fileName = null;

	private int numImages = 1;

	protected DecoderBase decoder;

	protected GenericImageReader(ImageReaderSpi originatingProvider) {
		super(originatingProvider);
		
	}

	protected void checkImageIndex(final int imageIndex) {
		if (imageIndex < 0 || imageIndex > numImages) {
			final StringBuffer sb = new StringBuffer(
					"Illegal imageIndex specified = ").append(imageIndex)
					.append(", while the valid imageIndex");
			if (numImages > 1)
				// There are N Images.
				sb.append(" range should be [0,").append(numImages - 1)
						.append("]!");
			else
				// Only the imageIndex 0 is valid.
				sb.append(" should be 0!");
			throw new IndexOutOfBoundsException(sb.toString());
		}
	}

	public int getNumImages(boolean allowSearch) throws IOException {
		return numImages;
	}
	public BufferedImage read(int imageIndex, ImageReadParam param)
			throws IOException {
		checkImageIndex(imageIndex);
		if (decoder == null)
			return null;

		decoder.decode(fileName);
		decoder.setCompressedStream(null);

		int width = decoder.getWidth();
		int height = decoder.getHeight();

		BufferedImage bufimg=null;

		byte[] buf8;
		short[] buf16;
		int[] buf24;
		if ((buf24 = decoder.getImage24()) != null) {
			int[] bitMasks = new int[] { 0xFF0000, 0xFF00, 0xFF, 0xFF000000 };
			SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
					DataBuffer.TYPE_INT, width, height, bitMasks);
			DataBufferInt db = new DataBufferInt(buf24, buf24.length);
			WritableRaster wr = Raster
					.createWritableRaster(sm, db, new Point());
			bufimg = new BufferedImage(ColorModel.getRGBdefault(), wr, false,
					null);
		} else if ((buf16 = decoder.getImage16()) != null) {
			int[] bits = { 16 };
			ColorModel cm = new ComponentColorModel(
					ColorSpace.getInstance(ColorSpace.CS_GRAY), bits, false,
					false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);

			SampleModel sm = cm.createCompatibleSampleModel(width, height);

			DataBufferUShort db = new DataBufferUShort(buf16, width * height
					* 2);

			WritableRaster ras = Raster.createWritableRaster(sm, db, null);

			bufimg = new BufferedImage(cm, ras, false, null);

		} else if ((buf8 = decoder.getImage8()) != null) {
			int[] bits = { 8 };
			ColorModel cm = new ComponentColorModel(
					ColorSpace.getInstance(ColorSpace.CS_GRAY), bits, false,
					false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

			SampleModel sm = cm.createCompatibleSampleModel(width, height);

			DataBufferByte db = new DataBufferByte(buf8, width * height);

			WritableRaster ras = Raster.createWritableRaster(sm, db, null);

			bufimg = new BufferedImage(cm, ras, false, null);
		}
		return bufimg;
	}

	public void setInput(Object input, boolean seekForwardOnly,
			boolean ignoreMetadata) {
		reset();
		if (input == null)
			throw new NullPointerException("The provided input is null!");
		if (input instanceof File)
		{
			inputFile = (File) input;
		} else if (input instanceof byte[]) 
		{
			decoder.setCompressedStream((byte[])input);
		} else if (input instanceof URL)
		{
			final URL tempURL = (URL) input;
			if (tempURL.getProtocol().equalsIgnoreCase("file")) {
				inputFile = Utils.urlToFile(tempURL);
			}
		} else if (input instanceof ImageInputStream) {
			try {
				
				ImageInputStream iis = (ImageInputStream)input;
				byte[] compressedBytes = new byte[(int)iis.length()];
				int bytesRead = 0;
				int offset = 0;
				while ((bytesRead = iis.read(compressedBytes,offset, TEMP_BUFFER_SIZE)) != -1)
					offset +=bytesRead;
				decoder.setCompressedStream(compressedBytes);
			} catch (IOException ioe) {
				throw new RuntimeException("Unable to read data from ImageInputStream", ioe);
			}
		}
		else if (input instanceof SegmentedDataInfo)
		{
			SegmentedDataInfo info = (SegmentedDataInfo)input;
			inputFile = new File(info.fileName);
			decoder.SetSegmentPositions(info.segmentOffsets);
			decoder.SetSegmentLengths(info.segmentLengths);
	}
		else
		{
			throw new IllegalArgumentException("Incorrect input type!");
		}

		if (this.inputFile != null)
    		fileName = inputFile.getAbsolutePath();
		
		numImages = 1;
		super.setInput(input, seekForwardOnly, ignoreMetadata);
	}

	public void dispose() {
		super.dispose();
		numImages = 1;
	}

	public void reset() {
		super.setInput(null, false, false);
		dispose();
		numImages = -1;
	}

	File getInputFile() {
		return inputFile;
	}

	String getFileName() {
		return fileName;
	}
}