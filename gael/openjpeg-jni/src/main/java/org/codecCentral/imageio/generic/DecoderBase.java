/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public abstract class DecoderBase extends DecoderEncoderBase {
	
	public interface IJavaJ2KDecoderLogger {
		public void logDecoderMessage(String message);

		public void logDecoderError(String message);
	}

	private Vector<IJavaJ2KDecoderLogger> loggers = new Vector<IJavaJ2KDecoderLogger>();
	public void logMessage(String message) {
		for (IJavaJ2KDecoderLogger logger : loggers)
			logger.logDecoderMessage(message);
	}
	public void logError(String error) {
		for (IJavaJ2KDecoderLogger logger : loggers)
			logger.logDecoderError(error);
	}
	
	public void addLogger(IJavaJ2KDecoderLogger messagesAndErrorsLogger) {
		loggers.addElement(messagesAndErrorsLogger);
	}

	public void removeLogger(IJavaJ2KDecoderLogger messagesAndErrorsLogger) {
		loggers.removeElement(messagesAndErrorsLogger);
	}
	
	// these fields are accessed from the C code via JNI
	private long[] segmentPositions;
	private long[] segmentLengths;
	public void SetSegmentPositions(long[] positions)
	{
		segmentPositions = positions;
	}
	public void SetSegmentLengths(long[] lengths)
	{
		segmentLengths = lengths;
	}
	
	private static boolean DEBUG_DECOMPRESS_FROM_BUFFER = false;
	protected String[] decoder_arguments = null;
	public void setDecoderArguments(String[] argumentsForTheDecoder) {
		decoder_arguments = argumentsForTheDecoder;
	}

	public void decode(String fname) {
		String[] args = null;
		reset();
		if (fname != null)
		{
			args = new String[1];
			if (DEBUG_DECOMPRESS_FROM_BUFFER) {
				try {
					compressedStream = getBytesFromFile(new File(fname));
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

			} else {
				args[0] = fname;
			}			
		}
		decoder_arguments = args;
		internalDecode(convertArguments(decoder_arguments));
	}

	private static byte[] getBytesFromFile(File file) throws IOException {
		long length = file.length();
		if (length > Integer.MAX_VALUE) {
			throw new IOException("File is too large!");
		}
		byte[] bytes = new byte[(int) length];
		int offset = 0;
		int numRead = 0;
		InputStream is = new FileInputStream(file);
		try {
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
		} finally {
			is.close();
		}
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}
		return bytes;
	}
	void alloc8() {
		if ((image8 == null || (image8 != null && image8.length != width
				* height))
				&& (getDepth() == 1))
      {
			image8 = new byte[width * height];
		}
	}
	void alloc16() {
		if ((image16 == null || (image16 != null && image16.length != width
				* height))
				&& (getDepth() == 2))
      {
			image16 = new short[width * height];
		}
	}
	void alloc24() {
		if ((image24 == null || image24.length != width * height) &&
				getDepth() == 3)
		{
			image24 = new int[width * height];
		}
	}

	public boolean canDecode(String fname)
	{
		String[] args = new String[]{fname};
		return  internalGetFormat(convertArguments(args)) != -1;
	}
	public boolean canDecode(byte[] buffer)
	{
		compressedStream = buffer;
		boolean canDecode =   internalGetFormat(convertArguments(new String[]{})) != -1;
		compressedStream = null;
		return canDecode;
	}
	protected abstract int internalDecode(String[] parameters);
	protected abstract int internalGetFormat(String[] parameters);
}
