/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;

public abstract class EncoderBase extends DecoderEncoderBase {
	

	 // Holds the compressed stream length, which may be smaller than
	 // compressedStream.length if this byte[] is pre-allocated

	protected long compressedStreamLength = -1;
	
	protected abstract long internalEncode(String[] parameters);
	
	public abstract void encode();
	
	public long getCompressedStreamLength() {
		return compressedStreamLength;
	}
	
}
