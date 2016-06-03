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

#ifdef _WIN32
#include "windirent.h"
#else
#include <dirent.h>
#endif /* _WIN32 */

#ifdef _WIN32
#include <windows.h>
#else
#include <strings.h>
#define _stricmp strcasecmp
#define _strnicmp strncasecmp
#endif /* _WIN32 */

#include "openjpeg.h"
#include "opj_getopt.h"
#include "convert.h"
#include "index.h"

#include "format_defs.h"


/**
error callback returning the message to Java andexpecting a callback_variables_t client object
*/
void error_callback(const char *msg, void *client_data) {
   jstring jbuffer;
   callback_variables_t* vars = NULL;
   JNIEnv *env = NULL;

   if (!client_data)
      return;
   vars = (callback_variables_t*) client_data;
   env = vars->env;

   jbuffer = (*env)->NewStringUTF(env, msg);
   (*env)->ExceptionClear(env);
   (*env)->CallVoidMethod(env, *(vars->jobj), vars->error_mid, jbuffer);

   if ((*env)->ExceptionOccurred(env)) {
      fprintf(stderr,"C: Exception during call back method\n");
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
   }
   (*env)->DeleteLocalRef(env, jbuffer);
}
/**
warning callback returning the message to Java andexpecting a callback_variables_t client object
*/
void warning_callback(const char *msg, void *client_data) {
   jstring jbuffer;
   callback_variables_t* vars = NULL;
   JNIEnv *env = NULL;

   if (!client_data)
      return;
   vars = (callback_variables_t*) client_data;
   env = vars->env;

   jbuffer = (*env)->NewStringUTF(env, msg);
   (*env)->ExceptionClear(env);
   (*env)->CallVoidMethod(env, *(vars->jobj), vars->message_mid, jbuffer);

   if ((*env)->ExceptionOccurred(env)) {
      fprintf(stderr,"C: Exception during call back method\n");
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
   }
   (*env)->DeleteLocalRef(env, jbuffer);
}
/**
information callback returning the message to Java andexpecting a callback_variables_t client object
*/
void info_callback(const char *msg, void *client_data) {
   jstring jbuffer;
   callback_variables_t* vars = NULL;
   JNIEnv *env = NULL;

   if (!client_data)
      return;
   vars = (callback_variables_t*) client_data;
   env = vars->env;

   jbuffer = (*env)->NewStringUTF(env, msg);
   (*env)->ExceptionClear(env);
   (*env)->CallVoidMethod(env, *(vars->jobj), vars->message_mid, jbuffer);

   if ((*env)->ExceptionOccurred(env)) {
      fprintf(stderr,"C: Exception during call back method\n");
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
   }
   (*env)->DeleteLocalRef(env, jbuffer);
}

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
static l_codec *codec_format (int format)
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


static int get_header (opj_stream_t stream, int format, opj_image_t* image)
{
   opj_image_t* image = NULL;
   opj_codec_t* l_codec = codec_format (format);

   /* catch events using our callbacks and give a local context */      
   opj_set_info_handler(l_codec, info_callback,00);
   opj_set_warning_handler(l_codec, warning_callback,00);
   opj_set_error_handler(l_codec, error_callback,00);

   opj_read_header(l_stream, l_codec, &image);
   print_image(image);
}


void print_image(opj_image_t* image)
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

void print_image_header (opj_image_comp_t* comp_header, FILE* out_stream)
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

static const char *clr_space(OPJ_COLOR_SPACE i)
{
   if(i == OPJ_CLRSPC_SRGB) return "OPJ_CLRSPC_SRGB";
   if(i == OPJ_CLRSPC_GRAY) return "OPJ_CLRSPC_GRAY";
   if(i == OPJ_CLRSPC_SYCC) return "OPJ_CLRSPC_SYCC";
   if(i == OPJ_CLRSPC_EYCC) return "OPJ_CLRSPC_EYCC";
   if(i == OPJ_CLRSPC_CYMK) return "OPJ_CLRSPC_CYMK";
   if(i == OPJ_CLRSPC_UNKNOWN) return "OPJ_CLRSPC_UNKNOWN";
   return "CLRSPC_UNDEFINED";
}



