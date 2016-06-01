/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;

import java.io.IOException;

import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

public abstract class GenericImageWriter extends ImageWriter{

    protected ImageOutputStream outputStream = null;
	protected EncoderBase encoder;
	
	protected GenericImageWriter(ImageWriterSpi originatingProvider) {
		super(originatingProvider);
	}
	
    protected void writeOnStream() throws IOException {
    	if (outputStream == null)
    		return;
    	int size = (int)encoder.getCompressedStreamLength()/(encoder.getDepth());
    	outputStream.write(encoder.getCompressedStream(), 0, size);
        
    }



}
