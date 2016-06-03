/*
 * The copyright in this software is being made available under the 2-clauses 
 * BSD License, included below. This software may be subject to other third 
 * party and contributor rights, including patent rights, and no such rights
 * are granted under this license.
 *
 * Copyright (c) 2010, Mathieu Malaterre, GDCM
 * Copyright (c) 2011-2012, Centre National d'Etudes Spatiales (CNES), France 
 * Copyright (c) 2012, CS Systemes d'Information, France
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
#include "opj_config.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>

#include "opj_includes.h"
#include "openjpeg.h"
#include "opj_malloc.h"

#include "org_openJpeg_OpenJPEGJavaDecoder.h"

#ifdef _WIN32
#include <windows.h>
#else
#define stricmp strcasecmp
#define strnicmp strncasecmp
#endif /* _WIN32 */

#include "format_defs.h"

typedef struct header_info
{
   JNIEnv* env;
   jobjectArray javaParameters;
   int argc;
   const char **argv;
   opj_codec_t *codec;
   opj_stream_t *stream;
   opj_image_t *image;
   jbyteArray   jba;
   jbyte      *jbBody;
   jintArray   jia;
   jint      *jiBody;
   jshortArray jsa;
   jshort      *jsBody;
   jbyteArray   jbaCompressed;
   jbyte      *jbBodyCompressed;
   jlongArray segmentPositions;
   jlong*     bodySegmentPositions;
   OPJ_OFF_T  *opjSegmentPositions;
   jlongArray segmentLengths;
   jlong*     bodySegmentLengths;
   OPJ_SIZE_T *opjSegmentLengths;

} header_info_t;

typedef struct callback_variables
{
   JNIEnv *env;
   /** 'jclass' object used to call a Java method from the C */
   jobject *jobj;
   /** 'jclass' object used to call a Java method from the C */
   jmethodID message_mid;
   jmethodID error_mid;
} callback_variables_t;


extern void error_callback(const char *msg, void *client_data);
extern void warning_callback(const char *msg, void *client_data);
extern void info_callback(const char *msg, void *client_data);
static void print_image(opj_image_t* image);

/* Convert a file into a buffer buffer must be freed by the caller */
static opj_buffer_info_t fileToBuffer(const char* fileName)
{
   opj_buffer_info_t buf_info;
   FILE *reader;
   size_t len;

   memset(&buf_info, 0, sizeof(opj_buffer_info_t));
   reader = fopen(fileName, "rb");
   if (!reader) return buf_info;

   fseek(reader, 0, SEEK_END);
   len = (size_t)ftell(reader);
   fseek(reader, 0, SEEK_SET);

   buf_info.buf = (unsigned char*)opj_malloc(len);
   fread(buf_info.buf, 1, len, reader);

   fclose(reader);
   buf_info.cur = buf_info.buf;
   buf_info.len = len;

   return buf_info;
}

#define JP2_RFC3745_MAGIC "\x00\x00\x00\x0c\x6a\x50\x20\x20\x0d\x0a\x87\x0a"
#define JP2_MAGIC "\x0d\x0a\x87\x0a"
#define J2K_CODESTREAM_MAGIC "\xff\x4f\xff\x51"
/* Identifies the format of the buffer */
static int buffer_format(opj_buffer_info_t* buf_info)
{
   int magic_format;
   if (!buf_info || buf_info->len < 12) return -1;
   if (memcmp(buf_info->buf, JP2_RFC3745_MAGIC, 12) == 0
      || memcmp(buf_info->buf, JP2_MAGIC, 4) == 0)
   {
      magic_format = JP2_CFMT;
   }
   else
   {
      if (memcmp(buf_info->buf, J2K_CODESTREAM_MAGIC, 4) == 0)
      {
         magic_format = J2K_CFMT;
      }
      else
         return -1;
   }
   return magic_format;
}/*  buffer_format() */

/* Build the codec from the format id */
static opj_codec_t *codec_format (int format)
{
   switch(format)
   {
      case J2K_CFMT:   /* JPEG-2000 codestream */
      {
         /* Get a decoder handle */
         return opj_create_decompress(OPJ_CODEC_J2K);
      }
      case JP2_CFMT:   /* JPEG 2000 compressed image data */
      {
         /* Get a decoder handle */
         return opj_create_decompress(OPJ_CODEC_JP2);
      }
      case JPT_CFMT:   /* JPEG 2000, JPIP */
      {
         /* Get a decoder handle */
         return opj_create_decompress(OPJ_CODEC_JPT);
      }
      default:
         fprintf(stderr, "No codec for format no %d.\n", format);
   }
   return 0L;
}

static int ext_file_format(const char *filename)
{
   unsigned int i;

   static const char *extension[] =
   {
      "j2k", "jp2", "jpt", "j2c", "jpc"
   };

   static const int format[] =
   {
      J2K_CFMT, JP2_CFMT, JPT_CFMT, J2K_CFMT, J2K_CFMT
   };

   char *ext = (char*)strrchr(filename, '.');

   if(ext == NULL) return -1;

   ext++;
   if(*ext)
   {
      for(i = 0; i < sizeof(format)/sizeof(*format); i++)
      {
         if(strnicmp(ext, extension[i], 3) == 0)
            return format[i];
      }
   }
   return -1;
}

static int infile_format(FILE *reader, const char *fname)
{
	char const *magic_s;
	int ext_format, magic_format;
	unsigned char buf[12];

	memset(buf, 0, 12);
	fread(buf, 1, 12, reader);
	rewind(reader);

	ext_format = ext_file_format(fname);

	if(ext_format == JPT_CFMT)
		return JPT_CFMT;

	if(memcmp(buf, JP2_RFC3745_MAGIC, 12) == 0
		|| memcmp(buf, JP2_MAGIC, 4) == 0)
	{
		magic_format = JP2_CFMT;
		magic_s = "'.jp2'";
	}
	else
		if(memcmp(buf, J2K_CODESTREAM_MAGIC, 4) == 0)
		{
			magic_format = J2K_CFMT;
			magic_s = "'.j2k' or '.jpc' or '.j2c'";
		}
		else
			return -1;

	if(magic_format == ext_format)
		return ext_format;

	//should we log the fact that the codestream format doesn't match the file extension??

	return magic_format;
}/* infile_format() */


static int getDecodeFormat(const char* fileName, OPJ_OFF_T offsetToData)
{
	FILE *reader = NULL;
	int decod_format;
	if(fileName == NULL || *fileName == 0)
	{
		fprintf(stderr,"%s:%d: input file missing\n",__FILE__,__LINE__);
		return -1;
	}
	if(strlen(fileName) > OPJ_PATH_LEN - 2)
	{
		fprintf(stderr,"%s:%d: input filename too long\n",__FILE__,__LINE__);
		return -1;
	}
	reader = fopen(fileName, "rb");
	if(reader == NULL)
	{
		fprintf(stderr,"%s:%d: failed to open %s for reading\n", __FILE__,__LINE__,fileName);
		return -1;
	}
	/*-------------------------------------------------*/

	//advance to data
	if (offsetToData != 0 && OPJ_FSEEK(reader,offsetToData,SEEK_SET)) {
		return -1;
	}

	decod_format = infile_format(reader, fileName);
	fclose(reader);
	return decod_format;
}


static int get_header (opj_stream_t l_stream, int format, opj_image_t** image)
{
   opj_codec_t* l_codec = codec_format (format);

   /* catch events using our callbacks and give a local context */      
   opj_set_info_handler(l_codec, info_callback,00);
   opj_set_warning_handler(l_codec, warning_callback,00);
   opj_set_error_handler(l_codec, error_callback,00);

   opj_read_header(l_stream, l_codec, image);
   opj_destroy_codec(l_codec);
}

static const char *clr_space(OPJ_COLOR_SPACE i)
{
   if(i == OPJ_CLRSPC_SRGB) return "OPJ_CLRSPC_SRGB";
   if(i == OPJ_CLRSPC_GRAY) return "OPJ_CLRSPC_GRAY";
   if(i == OPJ_CLRSPC_SYCC) return "OPJ_CLRSPC_SYCC";
   if(i == OPJ_CLRSPC_EYCC) return "OPJ_CLRSPC_EYCC";
   if(i == OPJ_CLRSPC_CMYK) return "OPJ_CLRSPC_CMYK";
   if(i == OPJ_CLRSPC_UNKNOWN) return "OPJ_CLRSPC_UNKNOWN";
   return "CLRSPC_UNDEFINED";
}

static void print_image_header (opj_image_comp_t* comp_header, FILE* out_stream)
{
   char tab[3];
   tab[0] = '\t';tab[1] = '\t';tab[2] = '\0';

   fprintf(out_stream, "%s dx=%d, dy=%d\n", tab, comp_header->dx, comp_header->dy);
   fprintf(out_stream, "%s w=%d, h=%d\n", tab, comp_header->w, comp_header->h);
   fprintf(out_stream, "%s x0=%d, y0=%d\n", tab, comp_header->x0, comp_header->y0);
   fprintf(out_stream, "%s prec=%d\n", tab, comp_header->prec);
   fprintf(out_stream, "%s bpp=%d\n", tab, comp_header->bpp);
   fprintf(out_stream, "%s sgnd=%d\n", tab, comp_header->sgnd);
   fprintf(out_stream, "%s resno_decoded=%d\n", tab, comp_header->resno_decoded);
   fprintf(out_stream, "%s factor=%d\n", tab, comp_header->factor);
   fprintf(out_stream, "%s alpha=%d\n", tab, comp_header->alpha);
}

static void print_image(opj_image_t* img_header)
{
   char tab[2];
   FILE*out_stream=stdout; 

   fprintf(out_stream, "Image info\n{\n");
   tab[0] = '\t';tab[1] = '\0';

   fprintf(out_stream, "%s x0=%d, y0=%d\n", tab, img_header->x0, img_header->y0);
   fprintf(out_stream, "%s x1=%d, y1=%d\n", tab, img_header->x1, img_header->y1);
   fprintf(out_stream, "%s color_space=%s\n", tab, clr_space(img_header->color_space));
   fprintf(out_stream, "%s numcomps=%d\n", tab, img_header->numcomps);

   if (img_header->comps)
   {
      OPJ_UINT32 compno;
      for (compno = 0; compno < img_header->numcomps; compno++)
      {
         fprintf(out_stream, "%s\t component %d\n{\n", tab, compno);
         print_image_header(&(img_header->comps[compno]), out_stream);
         fprintf(out_stream,"%s}\n",tab);
      }
   }
   fprintf(out_stream, "}\n");
}


void hide_openjpeg_get_header_from_file (const char *file)
{
   //opj_buffer_info_t buffer=fileToBuffer(file);
   int format = ext_file_format(file);
   opj_stream_t *stream = opj_stream_create_default_file_stream(file,OPJ_TRUE);

   opj_image_t *image;

   get_header (stream, format, &image);

   print_image(image);

   opj_stream_destroy (stream);
   opj_image_destroy (image);
}

static void release(header_info_t *headerInfo)
{
   JNIEnv* env = headerInfo->env;

   /* Release the Java arguments array:*/
   if (headerInfo->argv)
   {
      int i;
      for(i = 0; i < headerInfo->argc; i++)
      {
         if ((headerInfo->argv)[i] != NULL)
         {
            (*env)->ReleaseStringUTFChars(env,
               (*env)->GetObjectArrayElement(env, 
                  headerInfo->javaParameters, i), (headerInfo->argv)[i]);
         }
      }
      opj_free(headerInfo->argv);
      headerInfo->argv=NULL;
   }


   if(headerInfo->codec)
   {
      opj_destroy_codec(headerInfo->codec);
      headerInfo->codec = NULL;
   }

   if(headerInfo->stream)
   {
      opj_stream_destroy(headerInfo->stream);
      headerInfo->stream = NULL;
   }

   if(headerInfo->image)
   {
      opj_image_destroy(headerInfo->image);
      headerInfo->image = NULL;

   }

   if (headerInfo->jba  && headerInfo->jbBody )
   {
      (*env)->ReleaseByteArrayElements(headerInfo->env, headerInfo->jba, headerInfo->jbBody, 0);
      headerInfo->jba =NULL;
      headerInfo->jbBody = NULL;
   }
   if (headerInfo->jsa && headerInfo->jsBody )
   {
      (*env)->ReleaseShortArrayElements(env, headerInfo->jsa, headerInfo->jsBody, 0);
      headerInfo->jsa =NULL;
      headerInfo->jsBody = NULL;
   }
   if (headerInfo->jia && headerInfo->jiBody)
   {
      (*env)->ReleaseIntArrayElements(env, headerInfo->jia, headerInfo->jiBody, 0);
      headerInfo->jia =NULL;
      headerInfo->jiBody = NULL;
   }

   if (headerInfo->jbaCompressed &&  headerInfo->jbBodyCompressed)
   {
      (*env)->ReleaseByteArrayElements(env, headerInfo->jbaCompressed, headerInfo->jbBodyCompressed, 0);
      headerInfo->jbaCompressed = NULL;
      headerInfo->jbBodyCompressed = NULL;
   }

   if (headerInfo->segmentPositions &&  headerInfo->bodySegmentPositions)
   {
      (*env)->ReleaseLongArrayElements(env, headerInfo->segmentPositions, headerInfo->bodySegmentPositions, 0);
      headerInfo->segmentPositions = NULL;
      headerInfo->bodySegmentPositions = NULL;
   }

   if (headerInfo->segmentLengths &&  headerInfo->bodySegmentLengths)
   {
      (*env)->ReleaseLongArrayElements(env, headerInfo->segmentLengths, headerInfo->bodySegmentLengths, 0);
      headerInfo->segmentLengths = NULL;
      headerInfo->bodySegmentLengths = NULL;
   }

   if (headerInfo->opjSegmentPositions)
   {
      opj_free(headerInfo->opjSegmentPositions);
      headerInfo->opjSegmentPositions = NULL;
   }

   if (headerInfo->opjSegmentLengths)
   {
      opj_free(headerInfo->opjSegmentLengths);
      headerInfo->opjSegmentLengths = NULL;
   }
}

static OPJ_BOOL catchAndRelease(header_info_t *headerInfo)
{
   if((*headerInfo->env)->ExceptionOccurred(headerInfo->env))
   {
      release(headerInfo);
      return OPJ_TRUE;
   }
   return OPJ_FALSE;

}

/* -------------------------------
* MAIN METHOD, CALLED BY JAVA
* -----------------------------*/
JNIEXPORT jint JNICALL Java_org_openJpeg_OpenJPEGJavaDecoder_internalDecodeHeader(JNIEnv *env, jobject obj,jobjectArray javaParameters)
{
   opj_dparameters_t parameters;
   OPJ_BOOL hasFile = OPJ_FALSE;

   opj_buffer_info_t buf_info;
   int i, decod_format;
   int width, height;
   OPJ_BOOL hasAlpha, fails = OPJ_FALSE;
   OPJ_CODEC_FORMAT codec_format;
   unsigned char rc, gc, bc, ac;

   /*  ==> Access variables to the Java member variables */
   jsize      arraySize;
   jclass      klass=0;
   jobject      object = NULL;
   jboolean   isCopy = 0;
   jfieldID   fid;
   jbyte       *ptrBBody=NULL;
   jshort  *ptrSBody = NULL;
   jint       *ptrIBody=NULL;
   callback_variables_t msgErrorCallback_vars;
   header_info_t headerInfo;
   opj_file_info_t* p_file_info;

   memset(&headerInfo, 0, sizeof(header_info_t));
   headerInfo.env = env;
   headerInfo.javaParameters = javaParameters;

   memset(&buf_info, 0, sizeof(opj_buffer_info_t));


   /* JNI reference to the calling class
   */
   klass = (*env)->GetObjectClass(headerInfo.env, obj);
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;
   if (klass == 0)
   {
      fprintf(stderr,"GetObjectClass returned zero");
      return -1;

   }

   /* Pointers to be able to call a Java method
   * for all the info and error messages
   */
   msgErrorCallback_vars.env = headerInfo.env;
   msgErrorCallback_vars.jobj = &obj;
   msgErrorCallback_vars.message_mid = (*env)->GetMethodID(headerInfo.env, klass, "logMessage", "(Ljava/lang/String;)V");
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   msgErrorCallback_vars.error_mid = (*env)->GetMethodID(headerInfo.env, klass, "logError", "(Ljava/lang/String;)V");
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;


   /* Preparing the transfer of the codestream from Java to C*/
   /*printf("C: before transfering codestream\n");*/
   fid = (*env)->GetFieldID(headerInfo.env, klass,"compressedStream", "[B");
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   headerInfo.jbaCompressed = (*env)->GetObjectField(headerInfo.env, obj, fid);
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   if (headerInfo.jbaCompressed != NULL)
   {
      buf_info.len = (*env)->GetArrayLength(headerInfo.env, headerInfo.jbaCompressed);
      if ( catchAndRelease(&headerInfo) == -1)
         return -1;

      headerInfo.jbBodyCompressed = (*env)->GetByteArrayElements(headerInfo.env, headerInfo.jbaCompressed, &isCopy);
      if ( catchAndRelease(&headerInfo) == -1)
         return -1;

      buf_info.buf = (unsigned char*)headerInfo.jbBodyCompressed;
      buf_info.cur = buf_info.buf;
   }
   //if we don't have a buffer, then try to get a file name
   if (!buf_info.buf )
   {
      /* Get the String[] containing the parameters,
      *  and converts it into a char** to simulate command line arguments.
      */
      arraySize = (*env)->GetArrayLength(headerInfo.env, headerInfo.javaParameters);
      if ( catchAndRelease(&headerInfo) == -1)
         return -1;

      headerInfo.argc = (int) arraySize;

      if(headerInfo.argc != 1) /* program name plus input file */
      {
         fprintf(stderr,"%s:%d: input file missing\n",__FILE__,__LINE__);
         return -1;
      }
      headerInfo.argv = (const char**)opj_malloc(headerInfo.argc*sizeof(char*));
      if(headerInfo.argv == NULL)
      {
         fprintf(stderr,"%s:%d: MEMORY OUT\n",__FILE__,__LINE__);
         return -1;
      }
      for(i = 0; i < headerInfo.argc; i++)
      {
         headerInfo.argv[i] = NULL;
         object = (*env)->GetObjectArrayElement(headerInfo.env, headerInfo.javaParameters, i);
         if ( catchAndRelease(&headerInfo) == -1)
            return -1;
         if (object != NULL)
         {
            headerInfo.argv[i] = (*env)->GetStringUTFChars(headerInfo.env, object, &isCopy);
            if ( catchAndRelease(&headerInfo) == -1)
               return -1;
         }
          (*env)->DeleteLocalRef(env, object);

      }
   #ifdef DEBUG_SHOW_ARGS
      for(i = 0; i < headerInfo.argc; i++)
      {
         fprintf(stderr,"ARG[%i]%s\n",i,headerInfo.argv[i]);
      }
      printf("\n");
   #endif /* DEBUG */
   }

   opj_set_default_decoder_parameters(&parameters);
   //extract file name and release headerInfo.env array
   if (headerInfo.argv && headerInfo.argv[0] && headerInfo.argv[0][0]!='\0')
   {
      hasFile = OPJ_TRUE;
      p_file_info = (opj_file_info_t*)opj_calloc(1, sizeof(opj_file_info_t));
      strcpy(p_file_info->infile, headerInfo.argv[0]);

      //now check if it is segments
      /*printf("C: before transfering codestream\n");*/
      fid = (*env)->GetFieldID(headerInfo.env, klass,"segmentPositions", "[J");
      if ( catchAndRelease(&headerInfo) == -1)
         return -1;

      headerInfo.segmentPositions = (*env)->GetObjectField(headerInfo.env, obj, fid);
      if ( catchAndRelease(&headerInfo) == -1)
         return -1;

      if (headerInfo.segmentPositions != NULL)
      {
         int numPositions = 0;
         int numLengths = 0;
         int i = 0;
         OPJ_SIZE_T dataCount=0;
         OPJ_SIZE_T readCount=0;

         numPositions = (*env)->GetArrayLength(headerInfo.env, headerInfo.segmentPositions);
         if ( catchAndRelease(&headerInfo) == -1)
            return -1;

         headerInfo.bodySegmentPositions = (*env)->GetLongArrayElements(headerInfo.env, headerInfo.segmentPositions, &isCopy);
         if ( catchAndRelease(&headerInfo) == -1)
            return -1;

         fid = (*env)->GetFieldID(headerInfo.env, klass,"segmentLengths", "[J");
         if ( catchAndRelease(&headerInfo) == -1)
            return -1;

         headerInfo.segmentLengths = (*env)->GetObjectField(headerInfo.env, obj, fid);
         if ( catchAndRelease(&headerInfo) == -1)
            return -1;

         if (headerInfo.segmentLengths != NULL)
         {

            numLengths = (*env)->GetArrayLength(headerInfo.env, headerInfo.segmentLengths);
            if ( catchAndRelease(&headerInfo) == -1)
               return -1;

            headerInfo.bodySegmentLengths = (*env)->GetLongArrayElements(headerInfo.env, headerInfo.segmentLengths, &isCopy);
            if ( catchAndRelease(&headerInfo) == -1)
               return -1;
         }
         if (numPositions == 0 || numLengths == 0 || numPositions != numLengths)
         {
            release(&headerInfo);
            return -1;
         }

         headerInfo.opjSegmentPositions = (OPJ_OFF_T*)opj_malloc(numPositions * sizeof(OPJ_OFF_T));
         for (i = 0; i < numPositions; ++i)
         {
            headerInfo.opjSegmentPositions[i] = headerInfo.bodySegmentPositions[i];
         }

         headerInfo.opjSegmentLengths =  (OPJ_SIZE_T*)opj_malloc(numLengths*sizeof(OPJ_SIZE_T));
         for (i = 0; i < numLengths; ++i)
         {
            headerInfo.opjSegmentLengths[i] = headerInfo.bodySegmentLengths[i];
            p_file_info->dataLength += headerInfo.bodySegmentLengths[i];
         }

         p_file_info->numSegmentsMinusOne = numPositions-1;
         p_file_info->p_segmentPositionsList = headerInfo.opjSegmentPositions;
         p_file_info->p_segmentLengths = headerInfo.opjSegmentLengths;
      }
   }

   if (hasFile)
   {
      OPJ_OFF_T offsetToData = 0;
      if (p_file_info->p_segmentPositionsList != NULL)
         offsetToData = p_file_info->p_segmentPositionsList[0];
      decod_format = getDecodeFormat( p_file_info->infile, offsetToData);

   }
   else
   {
      /* Preparing the transfer of the codestream from Java to C*/
      /*printf("C: before transfering codestream\n");*/
      fid = (*env)->GetFieldID(headerInfo.env, klass,"compressedStream", "[B");
      if ( catchAndRelease(&headerInfo) == -1)
         return -1;

      headerInfo.jbaCompressed = (*env)->GetObjectField(headerInfo.env, obj, fid);
      if ( catchAndRelease(&headerInfo) == -1)
         return -1;

      if (headerInfo.jbaCompressed != NULL)
      {
         buf_info.len = (*env)->GetArrayLength(headerInfo.env, headerInfo.jbaCompressed);
         if ( catchAndRelease(&headerInfo) == -1)
            return -1;

         headerInfo.jbBodyCompressed = (*env)->GetByteArrayElements(headerInfo.env, headerInfo.jbaCompressed, &isCopy);
         if ( catchAndRelease(&headerInfo) == -1)
            return -1;

         buf_info.buf = (unsigned char*)headerInfo.jbBodyCompressed;
      }
      if (!buf_info.buf )
      {
         release(&headerInfo);
         return -1;
      }
      buf_info.cur = buf_info.buf;
      decod_format = buffer_format(&buf_info);
   }
   if(decod_format == -1)
   {
      fprintf(stderr,"%s:%d: decode format missing\n",__FILE__,__LINE__);
      release(&headerInfo);
      return -1;
   }

   /*-----------------------------------------------*/
   if(decod_format == J2K_CFMT)
      codec_format = OPJ_CODEC_J2K;
   else
      if(decod_format == JP2_CFMT)
         codec_format = OPJ_CODEC_JP2;
      else
         if(decod_format == JPT_CFMT)
            codec_format = OPJ_CODEC_JPT;
         else
         {
            /* clarified in infile_format() : */
            release(&headerInfo);
            return -1;
         }

   parameters.decod_format = decod_format;
   if (hasFile)
   {
      headerInfo.stream = opj_stream_create_file_stream_v4(p_file_info,OPJ_J2K_STREAM_CHUNK_SIZE,1);
   }
   else
   {
      headerInfo.stream =  opj_stream_create_buffer_stream(&buf_info, 1);
   }

   if(headerInfo.stream == NULL)
   {
      fprintf(stderr,"%s:%d: NO headerInfo.stream\n",__FILE__,__LINE__);
      release(&headerInfo);
      return -1;
   }
   headerInfo.codec = opj_create_decompress(codec_format);
   if(headerInfo.codec == NULL)
   {
      fprintf(stderr,"%s:%d: NO coded\n",__FILE__,__LINE__);
      release(&headerInfo);
      return -1;
   }

   opj_set_info_handler(headerInfo.codec, error_callback, &msgErrorCallback_vars);
   opj_set_info_handler(headerInfo.codec, warning_callback, &msgErrorCallback_vars);
   opj_set_info_handler(headerInfo.codec, info_callback, &msgErrorCallback_vars);

   if( !opj_setup_decoder(headerInfo.codec, &parameters))
   {
      fprintf(stderr,"%s:%d:\n\topj_setup_decoder failed\n",__FILE__,__LINE__);
      release(&headerInfo);
      return -1;
   }

   if( !opj_read_header(headerInfo.stream, headerInfo.codec, &headerInfo.image))
   {
      fprintf(stderr,"%s:%d:\n\topj_read_header failed\n",__FILE__,__LINE__);
      release(&headerInfo);
      return -1;
   }

   width = headerInfo.image->comps[0].w;
   height = headerInfo.image->comps[0].h;
   /* Set JAVA width and height:
   */
   fid = (*env)->GetFieldID(headerInfo.env, klass, "width", "I");
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   (*env)->SetIntField(headerInfo.env, obj, fid, width);
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   fid = (*env)->GetFieldID(headerInfo.env, klass, "height", "I");
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   (*env)->SetIntField(headerInfo.env, obj, fid, height);
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   fid = (*env)->GetFieldID(headerInfo.env, klass, "bitsPerSample", "I");
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   (*env)->SetIntField(headerInfo.env, obj, fid, headerInfo.image->comps[0].prec);
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   fid = (*env)->GetFieldID(headerInfo.env, klass, "samplesPerPixel", "I");
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   (*env)->SetIntField(headerInfo.env, obj, fid, headerInfo.image->numcomps);
   if ( catchAndRelease(&headerInfo) == -1)
      return -1;

   release(&headerInfo);

   return 0; /* OK */
} /* Java_OpenJPEGJavaDecoder_internalDecodeJ2KtoImage() */
