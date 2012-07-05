//---------------------
//Copyright 2012 Carnegie Mellon University
//
//This material is based upon work funded and supported by the Department of Defense under Contract No. 
//FA8721-05-C-0003 with Carnegie Mellon University for the operation of the Software Engineering Institute, 
//a federally funded research and development center.
//
//Any opinions, findings and conclusions or recommendations expressed in this material are those of the 
//author(s) and do not necessarily reflect the views of the United States Department of Defense.

//NO WARRANTY
//THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN “AS-IS” 
//BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY 
//MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, 
//OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF 
//ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
//
//This material contains SEI Proprietary Information and may not be disclosed outside of the SEI without 
//the written consent of the Director’s Office and completion of the Disclosure of Information process.
//------------

#include "stdafx.h"
#include "IplImageJpegConversion.h"
#include "jpeglib.h"

//---------------------------------------------------------------------------
struct my_error_mgr {
  struct jpeg_error_mgr pub;	/* "public" fields */

  jmp_buf setjmp_buffer;	/* for return to caller */
};
typedef struct my_error_mgr * my_error_ptr;

METHODDEF(void) my_error_exit (j_common_ptr cinfo);

//---------------------------------------------------------------------------

int IplImage2Jpeg(IplImage *Frame_RGB, int Quality,char *OutputBuffer, int *OutputBufferSize)
{
 struct jpeg_compress_struct   CinfoWrite;
 struct my_error_mgr           JerrWrite;
 int                           row_stride;		/* physical row width in output buffer */
 unsigned char                 *outdata_BGR,*indata_RGB;
 JSAMPROW                      row_ptr[1];
 IplImage                      *Frame_BGR;
 int                            height     = Frame_RGB->height;
 int                            width      = Frame_RGB->width;
 int                            step       = Frame_RGB->widthStep/sizeof(uchar);
 int                            channels   = Frame_RGB->nChannels;


 Frame_BGR=cvCreateImage(cvGetSize(Frame_RGB),IPL_DEPTH_8U,Frame_RGB->nChannels);

 outdata_BGR = (uchar *) Frame_BGR->imageData;
 indata_RGB  = (uchar *) Frame_RGB->imageData;

 for(int i=0;i<height;i++) for(int j=0;j<width;j++)
 {
  outdata_BGR[i*step+j*channels+0]=indata_RGB[i*step+j*channels+2];
  outdata_BGR[i*step+j*channels+1]=indata_RGB[i*step+j*channels+1];
  outdata_BGR[i*step+j*channels+2]=indata_RGB[i*step+j*channels+0];
 }


 CinfoWrite.err = jpeg_std_error(&JerrWrite.pub);
 JerrWrite.pub.error_exit = my_error_exit;
 if (setjmp(JerrWrite.setjmp_buffer))
  {
	/* If we get here, the JPEG code has signaled an error.
	 * We need to clean up the JPEG object, close the input file, and return.
	 */
	jpeg_destroy_compress(&CinfoWrite);
	cvReleaseImage(&Frame_BGR);
	return -1;
  }

 jpeg_create_compress(&CinfoWrite);
 jpeg_memory_dest(&CinfoWrite, (JOCTET *)OutputBuffer,*OutputBufferSize);
  
 CinfoWrite.image_width=Frame_BGR->width;
 CinfoWrite.image_height=Frame_BGR->height;
 CinfoWrite.input_components= Frame_BGR->nChannels ;
 CinfoWrite.in_color_space=JCS_RGB;
 
 jpeg_set_defaults(&CinfoWrite);
 jpeg_set_quality(&CinfoWrite, Quality, TRUE);
 jpeg_start_compress(&CinfoWrite, TRUE);
  
 row_stride = Frame_BGR->width * Frame_BGR->nChannels;
 
 while (CinfoWrite.next_scanline < CinfoWrite.image_height) 
 {
	row_ptr[0] = &outdata_BGR[CinfoWrite.next_scanline * row_stride];
	jpeg_write_scanlines(&CinfoWrite, row_ptr, 1);
 }
 
 (void)jpeg_finish_compress(&CinfoWrite);
 mem_dest_ptr dest = (mem_dest_ptr) CinfoWrite.dest;
 *OutputBufferSize = dest->datacount;
  
 jpeg_destroy_compress(&CinfoWrite);
 cvReleaseImage(&Frame_BGR);
 return(0);
}

//---------------------------------------------------------------------------
IplImage * Jpeg2IplImage(char *InputBuffer, int InputBufferSize)
{
 struct jpeg_decompress_struct CinfoRead;
 struct my_error_mgr           JerrRead;
 JSAMPROW                      row_pointer[1];
 IplImage                      *Frame;
 char                          *raw_image;
 int                            location=0;

 CinfoRead.err = jpeg_std_error(&JerrRead.pub);
 JerrRead.pub.error_exit = my_error_exit;

 /* Establish the setjmp return context for my_error_exit to use. */
 if (setjmp(JerrRead.setjmp_buffer))
  {
	/* If we get here, the JPEG code has signaled an error.
	 * We need to clean up the JPEG object, close the input file, and return.
	 */
	jpeg_destroy_decompress(&CinfoRead);
	return NULL;
  }

 jpeg_create_decompress(&CinfoRead);
 
 jpeg_memory_src (&CinfoRead, (JOCTET *)InputBuffer, InputBufferSize);
 
 (void) jpeg_read_header(&CinfoRead, TRUE);
 (void) jpeg_start_decompress(&CinfoRead);

 Frame = cvCreateImageHeader(cvSize(CinfoRead.output_width,CinfoRead.output_height), IPL_DEPTH_8U, CinfoRead.num_components);
 cvCreateData( Frame);

 raw_image = Frame->imageData;
 row_pointer[0] = (unsigned char *)malloc(CinfoRead.output_width*CinfoRead.num_components);


 while (CinfoRead.output_scanline < CinfoRead.output_height) {
	/* jpeg_read_scanlines expects an array of pointers to scanlines.
	 * Here the array is only one element long, but you could ask for
	 * more than one scanline at a time if that's more convenient.
	 */
	  jpeg_read_scanlines(&CinfoRead,row_pointer,1);
	  //for (unsigned int i = 0; i < CinfoRead.image_width*CinfoRead.num_components; i++)
	  //  {
	  //    raw_image[location++] = row_pointer[0][i];
	  //  }
	  for (unsigned int i = 0; i < CinfoRead.image_width*CinfoRead.num_components;i+=3)
	    {
	      raw_image[location++] = row_pointer[0][i+2];
          raw_image[location++] = row_pointer[0][i+1];
		  raw_image[location++] = row_pointer[0][i];
	    }
  }
 (void) jpeg_finish_decompress(&CinfoRead);
 free(row_pointer[0]);
 return(Frame);

}
//---------------------------------------------------------------------------
METHODDEF(void) my_error_exit (j_common_ptr cinfo)
{
  /* cinfo->err really points to a my_error_mgr struct, so coerce pointer */
  my_error_ptr myerr = (my_error_ptr) cinfo->err;

  /* Always display the message. */
  /* We could postpone this until after returning, if we chose. */
  (*cinfo->err->output_message) (cinfo);

  /* Return control to the setjmp point */
  longjmp(myerr->setjmp_buffer, 1);
}
//---------------------------------------------------------------------------


