/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;


import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageWriteParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class Utils {
	
	  
    public static List<ImageReaderWriterSpi> getJDKImageReaderWriterSPI(
            ServiceRegistry registry, String formatName, boolean isReader) {

        if (registry == null || !(registry instanceof IIORegistry))
            throw new IllegalArgumentException("Illegal registry provided");

        IIORegistry iioRegistry = (IIORegistry) registry;
        Class<? extends ImageReaderWriterSpi> spiClass;
        if (isReader)
            spiClass = ImageReaderSpi.class;
        else
            spiClass = ImageWriterSpi.class;

        final Iterator<? extends ImageReaderWriterSpi> iter = iioRegistry
                .getServiceProviders(spiClass, true); // useOrdering
        final ArrayList<ImageReaderWriterSpi> list = new ArrayList<ImageReaderWriterSpi>();
        while (iter.hasNext()) {
            final ImageReaderWriterSpi provider = (ImageReaderWriterSpi) iter.next();
            final String[] formatNames = provider.getFormatNames();
            for (int i = 0; i < formatNames.length; i++) {
                if (formatNames[i].equalsIgnoreCase(formatName)) {
                    list.add(provider);
                    break;
                }
            }
        }
        return list;
    }


    public static final double DOUBLE_TOLERANCE = 1E-6;
    public static void computeRegions(final Rectangle sourceBounds,
            Dimension destSize, ImageWriteParam param) {
        int periodX = 1;
        int periodY = 1;
        if (param != null) {
            final int[] sourceBands = param.getSourceBands();
            if (sourceBands != null && (sourceBands.length != 1 || sourceBands[0] != 0)) {
                throw new IllegalArgumentException("Cannot sub-band image!");
            }
            Rectangle sourceRegion = param.getSourceRegion();
            if (sourceRegion != null) {
                sourceRegion = sourceRegion.intersection(sourceBounds);
                sourceBounds.setBounds(sourceRegion);
            }
            periodX = param.getSourceXSubsampling();
            periodY = param.getSourceYSubsampling();

            int gridX = param.getSubsamplingXOffset();
            int gridY = param.getSubsamplingYOffset();
            sourceBounds.x += gridX;
            sourceBounds.y += gridY;
            sourceBounds.width -= gridX;
            sourceBounds.height -= gridY;
        }
        destSize.setSize((sourceBounds.width + periodX - 1) / periodX,  (sourceBounds.height + periodY - 1) / periodY);
        if (destSize.width <= 0 || destSize.height <= 0) {
            throw new IllegalArgumentException("Empty source region");
        }
    }
	
    public static boolean notEqual(double value, double reference) {
        return (Math.abs(value - reference) > DOUBLE_TOLERANCE); 
    }
	

    public static File urlToFile(URL url) {
        if (!"file".equals(url.getProtocol())) {
            return null; // not a File URL
        }
        String string = url.toExternalForm();
        if (string.contains("+")) {
            string = string.replace("+", "%2B");
        }
        try {
            string = URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not decode the URL to UTF-8 format", e);
        }
        String path3;
        String simplePrefix = "file:/";
        String standardPrefix = "file://";
        String os = System.getProperty("os.name");

        if (os.toUpperCase().contains("WINDOWS") && string.startsWith(standardPrefix)) {
            path3 = string.substring(standardPrefix.length() - 2);
        } else if (string.startsWith(standardPrefix)) {
            path3 = string.substring(standardPrefix.length());
        } else if (string.startsWith(simplePrefix)) {
            path3 = string.substring(simplePrefix.length() - 1);
        } else {
            String auth = url.getAuthority();
            String path2 = url.getPath().replace("%20", " ");
            if (auth != null && !auth.equals("")) {
                path3 = "//" + auth + path2;
            } else {
                path3 = path2;
            }
        }
        return new File(path3);
    }
}
