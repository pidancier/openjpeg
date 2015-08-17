/* 
 * Copyright (c) 2014, Aaron Boxer
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 * 
 */

package org.codecCentral.imageio.generic;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import org.codecCentral.imageio.generic.DecoderBase;

public abstract class GenericImageReaderSpi extends ImageReaderSpi {

	public GenericImageReaderSpi(String vendorname, String version,
			String[] formatnames, String[] suffixes, String[] mimetypes,
			String readercn, Class[] classes, String[] wsn,
			boolean supportsstandardstreammetadataformat,
			String nativestreammetadataformatname,
			String nativestreammetadataformatclassname,
			String[] extrastreammetadataformatnames,
			String[] extrastreammetadataformatclassnames,
			boolean supportsstandardimagemetadataformat,
			String nativeimagemetadataformatname,
			String nativeimagemetadataformatclassname,
			String[] extraimagemetadataformatnames,
			String[] extraimagemetadataformatclassnames) {

		super(vendorname, version,
		 formatnames,  suffixes, mimetypes,
		readercn,  classes,  wsn,
		supportsstandardstreammetadataformat,
		nativestreammetadataformatname,
		nativestreammetadataformatclassname,
		 extrastreammetadataformatnames,
		 extrastreammetadataformatclassnames,
		 supportsstandardimagemetadataformat,
		 nativeimagemetadataformatname,
		 nativeimagemetadataformatclassname,
		extraimagemetadataformatnames,
		extraimagemetadataformatclassnames);
	}
	
    /**
     * This method checks if the provided input can be decoded from this SPI
     */
    public boolean canDecodeInput(Object input) throws IOException {
    	if (input == null)
    		return false;
    	
        boolean isDecodable = true;
    	DecoderBase decoder = CreateDecoder();

        // Retrieving the File source
        if (input instanceof File) 
        {
            isDecodable =  decoder.canDecode(((File) input).getAbsolutePath());
          
        } else if (input instanceof byte[]) 
        {
        	isDecodable =  decoder.canDecode((byte[])input);
        } else if (input instanceof URL)
        {
            final URL tempURL = (URL) input;
            if (tempURL.getProtocol().equalsIgnoreCase("file")) 
            {
            	isDecodable =  decoder.canDecode(Utils.urlToFile(tempURL).getAbsolutePath());
            }
        } else
            return false;

        return isDecodable;
    }
    /**
     * Upon registration, this method ensures that this SPI is listed at the top
     * of the ImageReaderSpi items, so that it will be invoked before the
     * default ImageReaderSpi
     * 
     * @param registry
     *                ServiceRegistry where this object has been registered.
     * @param category
     *                a Class object indicating the registry category under
     *                which this object has been registered.
     */
    @SuppressWarnings("unchecked")
	public synchronized void onRegistration(ServiceRegistry registry, Class category) {
        super.onRegistration(registry, category);
        if (registered) {
            return;
        }

        registered = true;
        if (!_utilities.areLibrariesAvailable(libraries)) {
            final IIORegistry iioRegistry = (IIORegistry) registry;
            final Class<ImageReaderSpi> spiClass = ImageReaderSpi.class;
            final Iterator<ImageReaderSpi> iter = iioRegistry.getServiceProviders(spiClass,true);
            while (iter.hasNext()) {
                final ImageReaderSpi provider = (ImageReaderSpi) iter.next();
                if (provider.getClass().isAssignableFrom(getClass())) {
                    registry.deregisterServiceProvider(provider);
                }
            }
            return;
        }
        
        final List<ImageReaderWriterSpi> readers = Utils.getJDKImageReaderWriterSPI(registry,format, true);
        for (ImageReaderWriterSpi elem:readers) {
        	if (elem instanceof ImageReaderSpi){
	            final ImageReaderSpi spi = (ImageReaderSpi) elem;;
	            if (spi == this)
	                continue;
	            registry.deregisterServiceProvider(spi);
	            registry.setOrdering(category, this, spi);
        	}

        }
    }

	
	abstract protected DecoderBase CreateDecoder();
	
    protected boolean registered = false;
	
	protected NativeUtilities _utilities = new NativeUtilities();
	protected List<String> libraries;
	protected String format;

}
