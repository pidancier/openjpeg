/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeUtilities {

    private static final Logger LOGGER = Logger.getLogger("org.codecCentral.imageio.generic");
    
    private  boolean available;
    private  boolean initialized;
    
 
    public NativeUtilities()
    {
    	
    }
    public synchronized boolean areLibrariesAvailable(List<String> libraries) {
        loadLibraries(libraries);
        return available;
    }
    public synchronized void loadLibraries(List<String> libraries) {
        if (initialized)
            return;
        initialized = true;
        try {
        	for (String library : libraries)
        	{
        		if (library != null)
        			System.loadLibrary(library);
        		
        	}
            available = true;
        } catch (UnsatisfiedLinkError e) {
            if (LOGGER.isLoggable(Level.WARNING)){
            	 LOGGER.warning("Failed to load the native libs. Plugin disabled. " + e.toString());
            }
        }
    }
 }
