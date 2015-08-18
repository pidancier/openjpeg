package org.openJpeg;


import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.codecCentral.imageio.generic.GenericImageWriter;
import org.codecCentral.imageio.generic.Utils;
import org.openJpeg.JP2KOpenJpegImageWriteParam.Compression;
import org.openJpeg.JP2KOpenJpegImageWriteParam.ProgressionOrder;

public class JP2KOpenJpegImageWriter extends GenericImageWriter {


    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger.getLogger("org.openJpeg");

    /** The System Property key used to define the maximum buffer size */
    public final static String MAX_BUFFER_SIZE_KEY = "org.openJpeg.maxBufferSize";

    /** The System Property key used to define the temp buffer size */
    public final static String TEMP_BUFFER_SIZE_KEY = "org.openJpeg.tempBufferSize";

    /** The System Property key used to customize the Comment Marker presence */
    public final static String ADD_COMMENT_MARKER_KEY = "org.openJpeg.addCommentMarker";

    /** The Default Maximum Buffer Size */
    private final static int DEFAULT_MAX_BUFFER_SIZE = 32 * 1024 * 1024;

    /** The Default Temp Buffer Size */
    private final static int DEFAULT_TEMP_BUFFER_SIZE = 64 * 1024;

    /**
     * Set at once to indicate whether to write the comment marker generated by
     * rate allocation
     */
    private final static boolean ADD_COMMENT_MARKER;

    /**
     * A temporary buffer size where to store data when directly writing
     * on an output stream
     */
    private static int TEMP_BUFFER_SIZE = DEFAULT_TEMP_BUFFER_SIZE;

    /**
     * The max size in bytes of the buffer to be used by stripe compressor.
     */
    private final static int MAX_BUFFER_SIZE;

    private final static int MIN_BUFFER_SIZE = 1024 * 1024;



    /**
     * Static initialization
     */
    static {
	int size = DEFAULT_MAX_BUFFER_SIZE;
	int buffer = DEFAULT_TEMP_BUFFER_SIZE;
	final Integer maxSize = Integer.getInteger(MAX_BUFFER_SIZE_KEY);
	final Integer bufferSize = Integer.getInteger(TEMP_BUFFER_SIZE_KEY);
	String marker = System.getProperty(ADD_COMMENT_MARKER_KEY);
	if (marker != null) {
	    final Boolean addComment = Boolean.parseBoolean(ADD_COMMENT_MARKER_KEY);
	    if (addComment != null){
		ADD_COMMENT_MARKER = addComment.booleanValue();
	    } else {
		ADD_COMMENT_MARKER = true;
	    }
	} else {
	    ADD_COMMENT_MARKER = true;
	}

	// //
	//
	// Setting MAX BUFFER SIZE
	//
	// //
	if (maxSize != null) {
	    size = maxSize.intValue();
	} else {
	    final String maxSizes = System.getProperty(MAX_BUFFER_SIZE_KEY);
	    if (maxSizes != null) {
		size = parseSize(maxSizes);
	    }
	}

	// //
	//
	// Setting TEMP BUFFER SIZE
	//
	// //
	if (bufferSize != null) {
	    buffer = bufferSize.intValue();
	} else {
	    final String bufferSizes = System.getProperty(TEMP_BUFFER_SIZE_KEY);
	    if (bufferSizes != null) {
		buffer = parseSize(bufferSizes);
	    }
	}
	TEMP_BUFFER_SIZE = buffer;
	MAX_BUFFER_SIZE = size;
    }

    /**
     * Get a default {@link ImageWriteParam} instance.
     */
    public ImageWriteParam getDefaultWriteParam() {
	return new JP2KOpenJpegImageWriteParam();
    }

    /**
     *
     * @param sizeValue
     * @return
     */
    private static int parseSize(final String sizeValue) {
	// //
	//
	// Checking for a properly formatted string value.
	// Valid values should end with one of M,m,K,k
	//
	// //

	int size = 0;
	final int length = sizeValue.length();
	final String value = sizeValue.substring(0, length - 1);
	final String suffix = sizeValue.substring(length - 1, length);

	// //
	//
	// Checking for valid multiplier suffix
	//
	// //
	if (suffix.equalsIgnoreCase("M")
		|| suffix.equalsIgnoreCase("K")) {
	    int val;
	    try {
		val = Integer.parseInt(value);
		if (suffix.equalsIgnoreCase("M"))
		    val *= (1024 * 1024); // Size in MegaBytes
		else
		    val *= 1024; // Size in KiloBytes
		size = val;
	    } catch (NumberFormatException nfe) {
		// not a valid value
	    }
	}
	return size;
    }

    /**
     * In case the ratio between the stripe_height and the image height is
     * greater than this value, set the stripe_height to the image height in
     * order to do a single push
     */
    private static final double SINGLE_PUSH_THRESHOLD_RATIO = 0.95;

    /**
     * The file to be written
     */
    private File outputFile;

    public JP2KOpenJpegImageWriter(ImageWriterSpi originatingProvider) {
	super(originatingProvider);
	encoder = new OpenJPEGJavaEncoder();
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData,
	    ImageTypeSpecifier imageType, ImageWriteParam param) {
	return null;
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData,
	    ImageWriteParam param) {
	return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
	    ImageWriteParam param) {
	return null;
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
	return null;
    }

    /**
     * Sets the destination to the given <code>Object</code>, usually a
     * <code>File</code> or a {@link FileImageOutputStreamExt}.
     *
     * @param output
     *                the <code>Object</code> to use for future writing.
     */
    public void setOutput(Object output) {
	super.setOutput(output); // validates output
	if (output instanceof File)
	    outputFile = (File) output;
	else if (output instanceof byte[])
	{

	}
	else if (output instanceof URL) {
	    final URL tempURL = (URL) output;
	    if (tempURL.getProtocol().equalsIgnoreCase("file")) {
		outputFile = Utils.urlToFile(tempURL);
	    }
	} else if (output instanceof ImageOutputStream) {
	    try {
		outputStream = (ImageOutputStream) output;
		outputFile = File.createTempFile("buffer", ".j2c");
	    } catch (IOException e) {
		throw new RuntimeException("Unable to create a temp file", e);
	    }
	}
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image,
	    ImageWriteParam param) throws IOException {


	// ////////////////////////////////////////////////////////////////////
	//
	// Variables initialization
	//
	// ////////////////////////////////////////////////////////////////////
	final String fileName = outputFile.getAbsolutePath();
	JP2KOpenJpegImageWriteParam jp2Kparam;
	final boolean writeCodeStreamOnly;
	final double quality;
	int cLayers = 1;
	int cLevels;
	final boolean cycc;
	final boolean orgGen_plt;
	int orgGen_tlm = JP2KOpenJpegImageWriteParam.UNSPECIFIED_ORG_GEN_TLM;
	int qGuard = -1;
	String orgT_parts = null;
	String cPrecincts = null;
	boolean setTiling = false;
	int tileW = Integer.MIN_VALUE;
	int tileH = Integer.MIN_VALUE;
	ProgressionOrder cOrder = null;
	double[] bitRates = null;
	boolean addCommentMarker = ADD_COMMENT_MARKER;
	int sProfile = JP2KOpenJpegImageWriteParam.DEFAULT_SPROFILE;
	Compression compression = Compression.UNDEFINED;

	// //
	//
	// Write parameters parsing
	//
	// //
	if (param == null) {
	    param = getDefaultWriteParam();
	}
	if (param instanceof JP2KOpenJpegImageWriteParam) {
	    jp2Kparam = (JP2KOpenJpegImageWriteParam) param;
	    writeCodeStreamOnly = jp2Kparam.isWriteCodeStreamOnly();

	    bitRates = jp2Kparam.getQualityLayersBitRates();
	    double q = jp2Kparam.getQuality();
	    if (q < 0.01) {
		q = 0.01;
		if (LOGGER.isLoggable(Level.FINE))
		    LOGGER.fine("Quality level should be in the range 0.01 - 1. /n Reassigning it to 0.01");
	    }
	    quality = q;

	    setTiling = jp2Kparam.getTilingMode() == ImageWriteParam.MODE_EXPLICIT;

	    if (setTiling) {
		tileH = jp2Kparam.getTileHeight();
		tileW = jp2Kparam.getTileWidth();
	    }

	    // COD PARAMS
	    cOrder = jp2Kparam.getcOrder();
	    cPrecincts = jp2Kparam.getcPrecincts();
	    cLevels = jp2Kparam.getCLevels();
	    cLayers = jp2Kparam.getQualityLayers();

	    // ORG PARAMS
	    orgGen_plt = jp2Kparam.isOrgGen_plt();
	    orgGen_tlm = jp2Kparam.getOrgGen_tlm();
	    orgT_parts = jp2Kparam.getOrgT_parts();
	    qGuard = jp2Kparam.getqGuard();

	    addCommentMarker &= jp2Kparam.isAddCommentMarker();
	    sProfile = jp2Kparam.getsProfile();
	    compression = jp2Kparam.getCompression();

	    if (bitRates != null && bitRates.length != cLayers) {
		throw new IllegalArgumentException(" Specified bitRates parameter's length "
			+ bitRates.length + " should match the quality layers parameter "
			+ "(cLayers): " + cLayers);
	    }

	    if (compression != null){
		switch (compression){
		case LOSSY:
		    if (bitRates != null){
			if (LOGGER.isLoggable(Level.FINE)){
			    LOGGER.fine("Applying lossy compression leveraging on provided quality bit rates");
			}
		    } else {
			if (LOGGER.isLoggable(Level.FINE)){
			    LOGGER.fine("Applying lossy compression leveraging on quality factor");
			}
		    }
		    break;
		case NUMERICALLY_LOSSLESS:
		    if (bitRates != null){
			if (LOGGER.isLoggable(Level.FINE)){
			    LOGGER.fine("Applying numerically lossless compression leveraging on " +
					"provided quality bit rates");
			}
			if (Utils.notEqual(bitRates[bitRates.length - 1] ,0)){
			    throw new IllegalArgumentException("Asking for a Numerically Lossless "
					+"but the last quality layer's bit rate should be 0 "
					+ " instead of " + bitRates[bitRates.length - 1]);
			}
		    } else {
			if (LOGGER.isLoggable(Level.FINE)){
			    LOGGER.fine("Applying numerically lossless compression");
			}
		    }
		    break;
		}
	    } else {
		compression = Compression.UNDEFINED;
	    }
	} else {
	    orgGen_plt = false;
	    writeCodeStreamOnly = true;
	    quality = JP2KOpenJpegImageWriteParam.DEFAULT_QUALITY;
	    cLevels = JP2KOpenJpegImageWriteParam.DEFAULT_C_LEVELS;
	}

	// ////////////////////////////////////////////////////////////////////
	//
	// Image properties initialization
	//
	// ////////////////////////////////////////////////////////////////////
	final RenderedImage inputRenderedImage = image.getRenderedImage();
	final int sourceWidth = inputRenderedImage.getWidth();
	final int sourceHeight = inputRenderedImage.getHeight();
	final int sourceMinX = inputRenderedImage.getMinX();
	final int sourceMinY = inputRenderedImage.getMinY();
	final SampleModel sm = inputRenderedImage.getSampleModel();
	final int dataType = sm.getDataType();
	final boolean isDataSigned = (dataType != DataBuffer.TYPE_USHORT && dataType != DataBuffer.TYPE_BYTE);

	final ColorModel colorModel = inputRenderedImage.getColorModel();
	final boolean hasPalette = colorModel instanceof IndexColorModel ? true : false;
	final int[] numberOfBits = colorModel.getComponentSize();

	// The number of bytes used by header, markers, boxes
	int bytesOverHead = 0;

	// We suppose all bands share the same bitDepth
	final int bits = numberOfBits[0];
	int nComponents = sm.getNumBands();

	// Array to store optional look up table entries
	byte[] reds = null;
	byte[] greens = null;
	byte[] blues = null;

	// //
	//
	// Handling paletted Image
	//
	// //
	if (hasPalette) {
	    cycc = false;
	    cLevels = 1;
	    IndexColorModel icm = (IndexColorModel) colorModel;
	    final int lutSize = icm.getMapSize();
	    final int numColorComponents = colorModel.getNumColorComponents();
	    // Updating the number of components to write as RGB (3 bands)
	    if (writeCodeStreamOnly) {
		nComponents = numColorComponents;

		// //
		//
		// Caching look up table for future accesses.
		//
		// //
		reds = new byte[lutSize];
		blues = new byte[lutSize];
		greens = new byte[lutSize];
		icm.getReds(reds);
		icm.getGreens(greens);
		icm.getBlues(blues);
	    } else {
		// adding pclr and cmap boxes overhead bytes
		bytesOverHead += (4 + 2 + numColorComponents + 1); // NE + NC + Bi
		bytesOverHead += lutSize * numColorComponents + 4; // pclr LUT
		bytesOverHead += 20; // cmap
	    }
	} else if (quality == 1) {
	    cycc = false;
	} else {
	    cycc = true;
	}

	// //
	//
	// Setting regions and sizes and retrieving parameters
	//
	// //
	final int xSubsamplingFactor = param.getSourceXSubsampling();
	final int ySubsamplingFactor = param.getSourceYSubsampling();
	final Rectangle originalBounds = new Rectangle(sourceMinX, sourceMinY, sourceWidth, sourceHeight);
	final Rectangle imageBounds = (Rectangle) originalBounds.clone();
	final Dimension destSize = new Dimension();
	Utils.computeRegions(imageBounds, destSize, param);

	boolean resampleInputImage = false;
	if (xSubsamplingFactor != 1 || ySubsamplingFactor != 1 || !imageBounds.equals(originalBounds)) {
	    resampleInputImage = true;
	}

	// Destination sizes
	final int destinationWidth = destSize.width;
	final int destinationHeight = destSize.height;
	final int rowSize = (destinationWidth * nComponents);
	final int bandSize = destinationHeight * destinationWidth;
	final int imageSize = bandSize * nComponents;



	DataBuffer buff =  	inputRenderedImage.getData().getDataBuffer();
	if (buff instanceof DataBufferUShort)
	{
		encoder.setImage16(((DataBufferUShort)buff).getData());
	}

	encoder.setBitsPerSample(image.getRenderedImage().getColorModel().getPixelSize());
		encoder.setWidth( sourceWidth);
		encoder.setHeight(sourceHeight);
		((OpenJPEGJavaEncoder)encoder).setNbResolutions(6);
	    encoder.encode();


	 writeOnStream();
    }




}
