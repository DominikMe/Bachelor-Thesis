#include "jinclude.h"
#include "jpeglib.h"
#include "jerror.h"



//=============================================================================
// Initialize destination --- called by jpeg_start_compress before any data is actually written.
METHODDEF(void)
init_destination (j_compress_ptr cinfo)
{
mem_dest_ptr dest = (mem_dest_ptr) cinfo->dest;
dest->pub.next_output_byte = dest->buffer;
dest->pub.free_in_buffer = dest->bufsize;
dest->datacount=0;
}

//=============================================================================
// Empty the output buffer --- called whenever buffer fills up.
METHODDEF(boolean)
empty_output_buffer (j_compress_ptr cinfo)
{
mem_dest_ptr dest = (mem_dest_ptr) cinfo->dest;
dest->pub.next_output_byte = dest->buffer;
dest->pub.free_in_buffer = dest->bufsize;

return TRUE;
}

//=============================================================================
// Terminate destination --- called by jpeg_finish_compress
// after all data has been written. Usually needs to flush buffer.
METHODDEF(void)
term_destination (j_compress_ptr cinfo)
{
/* expose the finale compressed image size */

mem_dest_ptr dest = (mem_dest_ptr) cinfo->dest;
dest->datacount = dest->bufsize - dest->pub.free_in_buffer;

}

//=============================================================================
GLOBAL(void)
jpeg_memory_dest(j_compress_ptr cinfo, JOCTET *buffer,int bufsize)
{
mem_dest_ptr dest;
if (cinfo->dest == NULL) { /* first time for this JPEG object? */
cinfo->dest = (struct jpeg_destination_mgr *)
(*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_PERMANENT,
sizeof(memory_destination_mgr));
}

dest = (mem_dest_ptr) cinfo->dest;
dest->bufsize=bufsize;
dest->buffer=buffer;
dest->pub.init_destination = init_destination;
dest->pub.empty_output_buffer = empty_output_buffer;
dest->pub.term_destination = term_destination;
}

