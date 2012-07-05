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
#include <Windows.h>
#include <stdio.h>
#include "SaveBufferToFile.h"
//---------------------------------------------------------------------------
int SaveBufferToFile(TCHAR *FileNamePrefix,DWORD id,TCHAR *FileNamePostix,char *Buffer,int BufferLength)
{
  TCHAR OutputFileName[1024];
  HANDLE hOutputFile;
  DWORD BytesSWritten;

  wsprintf(OutputFileName,L"%ls%08d%ls",FileNamePrefix,id,FileNamePostix);
  hOutputFile = CreateFile(OutputFileName,// name of the write
					   GENERIC_WRITE,
					   0,
					   NULL,                   // default security
					   CREATE_ALWAYS,          // overwrite existing
					   FILE_ATTRIBUTE_NORMAL,  // normal file
					   NULL);                  // no attr. template
 if (hOutputFile==INVALID_HANDLE_VALUE)
	 {
	  printf("File Open Output file Failed %ls\n",OutputFileName);;
	  return(-1);
	 }
 if (!WriteFile(hOutputFile,Buffer,BufferLength,&BytesSWritten,NULL))
   {
    CloseHandle(hOutputFile);
    printf("Write Failed\n");
    return(-1);
   }
  if (BytesSWritten!=BufferLength)
   {
    CloseHandle(hOutputFile);
    printf("Write not complete\n");
    return(-1);
   }
  CloseHandle(hOutputFile);
  return(0);
}
//---------------------------------------------------------------------------