/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;

public abstract class DecoderEncoderBase {

	public DecoderEncoderBase()
			throws ExceptionInInitializerError {
		
	}

	protected byte[] image8 = null;
	protected short[] image16 = null;
	protected int[] image24 = null;

	protected byte compressedStream[] = null;

	protected int width = -1;
	protected int height = -1;
	protected int bitsPerSample = -1;
	protected int samplesPerPixel = -1;
	
	public void setCompressedStream(byte[] compressedStream) {
		this.compressedStream = compressedStream;
	}
	public long getCodestreamLength() {
		if (compressedStream == null)
			return -1;
		else
			return compressedStream.length;
	}

	/**
	 * Contains the decompressed version of the image, if the depth in is [9,16]
	 * bpp. Returns NULL otherwise.
	 */
	public short[] getImage16() {
		return image16;
	}

	/**
	 * Contains the decompressed version of the image, if the depth in is
	 * [17,24] bpp and the image is in color. Returns NULL otherwise.
	 */
	public int[] getImage24() {
		return image24;
	}

	/**
	 * This method must be called in depth in [9,16].
	 * 
	 * @param an array of shorts, containing width*height values
	 */
	public void setImage16(short[] image16) {
		this.image16 = image16;
	}

	/**
	 * This method must be called in depth in [17,24] for RGB images.
	 * 
	 * @param an   array of int, containing width*height values
	 */
	public void setImage24(int[] image24) {
		this.image24 = image24;
	}

	/**
	 * This method must be called in depth in [1,8].
	 * 
	 * @param an
	 *            array of bytes, containing width*height values
	 */
	public void setImage8(byte[] image8) {
		this.image8 = image8;
	}

	/**
	 * Contains the decompressed version of the image, if the depth in is [1,8]
	 * bpp. Returns NULL otherwise.
	 */
	public byte[] getImage8() {
		return image8;
	}
	
	public byte[] getCompressedStream() {
		return compressedStream;
	}

	/** Image depth in bpp */
	public int getDepth() {
		return ((bitsPerSample + 7)/8)*samplesPerPixel;
	}

	/** Image depth in bpp */
	public void setBitsPerSample(int bits) {
		this.bitsPerSample = bits;
	}

	/** Image height in pixels */
	public int getHeight() {
		return height;
	}

	/** Image height in pixels */
	public void setHeight(int height) {
		this.height = height;
	}

	/** Width of the image in pixels */
	public int getWidth() {
		return width;
	}

	/** Width of the image in pixels */
	public void setWidth(int width) {
		this.width = width;
	}

	public void reset() {
		image8 = null;
		image16 = null;
		image24 = null;
		width = -1;
		height = -1;
		bitsPerSample = -1;
	}
	

	protected String[] convertArguments(String[] input)
	{
		String[] arguments = new String[0 + (input != null ? input.length : 0)];
		int offset = 0;
		if (input != null)
		{
			for (int i = 0; i < input.length; i++)
				arguments[i + offset] = input[i];
		}
		return arguments;
	}

}
