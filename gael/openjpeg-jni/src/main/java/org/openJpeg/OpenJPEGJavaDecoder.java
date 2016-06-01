/*
 * Copyright (c) 2014, Aaron Boxer
 * Copyright (c) 2002-2007, Communications and Remote Sensing Laboratory, Universite catholique de Louvain (UCL), Belgium
 * Copyright (c) 2002-2007, Professor Benoit Macq
 * Copyright (c) 2002-2007, Patrick Piscaglia, Telemis s.a.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS `AS IS'
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.openJpeg;

import org.codecCentral.imageio.generic.DecoderBase;

/**
 * This class decodes one  codestream into an image (width + height + depth +
 * pixels[].. To be able to log messages, the
 * called must register a IJavaJ2KDecoderLogger object.
 */
public class OpenJPEGJavaDecoder extends DecoderBase {

	/** the quality layers */
	private int[] layers = null;

	private int reductionIn = 0;
	private int tileIn = -1;
	private int areaX0 = 0;
	private int areaY0 = 0;
	private int areaX1 = 0;
	private int areaY1 = 0;

	private int userChangedTile = 0;
	private int userChangedReduction = 0;
	private int userChangedArea = 0;

	private int maxTiles = 0;
	private int maxReduction = 0;

	private static boolean DEBUG_COMPRESS_FROM_BUFFER = false;


	@Override
    protected int internalDecode(String[] parameters)
	{
	   int rc =  	internalDecodeJ2KtoImage(parameters);
		if (DEBUG_COMPRESS_FROM_BUFFER)
		{

			OpenJPEGJavaEncoder encoder = new OpenJPEGJavaEncoder();
			if (image8 != null)
			{
				encoder.setImage8(image8);
			}
			else if (image16 != null)
			{
				encoder.setImage16(image16);


			}
			else if (image24 != null)
			{
				encoder.setImage24(image24);
			}
			 encoder.setBitsPerSample(bitsPerSample);
			encoder.setWidth( width);
			encoder.setHeight(height);
			encoder.setNbResolutions(6);
		    encoder.encode();
		}
		return rc;
	}

	@Override
	protected int internalGetFormat(String[] parameters) {
		// TODO Auto-generated method stub
		return internalGetDecodeFormat(parameters);
	}


	//NATIVE METHODS

	/**
	 * Decode the j2k stream given in the codestream byte[] and fills the
	 * image8, image16 or image24 array, according to the bit depth.
	 */
	/* ================================================================== */
	private native int internalDecodeJ2KtoImage(String[] parameters);
	private native int internalGetDecodeFormat(String[] parameters);

	/* ================================================================== */

	public void setTileIn(int t) {
		this.tileIn = t;
	}

	public void setReductionIn(int r) {
		this.reductionIn = r;
	}

	public void setAreaIn(int x0, int y0, int x1, int y1) {
		this.areaX0 = x0;
		this.areaY0 = y0;
		this.areaX1 = x1;
		this.areaY1 = y1;
	}

	public void setUserChangedTile(int v) {
		this.userChangedTile = v;
	}

	public void setUserChangedReduction(int v) {
		this.userChangedReduction = v;
	}

	public void setUserChangedArea(int v) {
		this.userChangedArea = v;
	}

	public int getMaxTiles() {
		return maxTiles;
	}

	public int getMaxReduction() {
		return maxReduction;
	}

	public void setMaxTiles(int v) {
		this.maxTiles = v;
	}

	public void setMaxReduction(int v) {
		this.maxReduction = v;
	}

	public void reset() {
		layers = null;
		super.reset();
	}


}
