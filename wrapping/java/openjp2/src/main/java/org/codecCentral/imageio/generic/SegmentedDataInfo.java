/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;

public class SegmentedDataInfo {
	
	public SegmentedDataInfo(String fileName, long[] segmentOffsets, long[] segmentLengths)
	{
		this.fileName = fileName;
		this.segmentOffsets = segmentOffsets;
		this.segmentLengths = segmentLengths;
		frame = -1;
		numFrames = -1;
	}
	
	public String fileName;
	public long[] segmentOffsets;
	public long[] segmentLengths;
	public long frame;
	public long numFrames;

}
