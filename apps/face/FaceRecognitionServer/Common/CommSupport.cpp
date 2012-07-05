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
#include <stdio.h>
#include "CommSupport.h"
#define NEED_MESSSAGE_HEADER 1
#define NEED_MESSSAGE_DATA   2

static int WriteNetData( SOCKET Socket, char *Buffer, int BytestoSend);

//---------------------------------------------------------------------------
TCommReader * CreateCommReader(SOCKET Socket,DWORD MaxDataSize,TProcessRecvdMessage  ProcessRecvdMessagePtr)
{
 TCommReader *NewCommReader=new TCommReader;
 NewCommReader->Socket=Socket;
 NewCommReader->MessageHeaderBytesNeeded=sizeof(TMessageHeader);
 NewCommReader->MessageState=NEED_MESSSAGE_HEADER;
 NewCommReader->ProcessRecvdMessagePtr=ProcessRecvdMessagePtr;
 NewCommReader->SizeOfMessageData=MaxDataSize;
 if ( NewCommReader->SizeOfMessageData>0)
    NewCommReader->MessageData=new BYTE[NewCommReader->SizeOfMessageData];
 else NewCommReader->MessageData=NULL;
 return(NewCommReader);
}
//---------------------------------------------------------------------------
int DeleteCommReader(TCommReader *CommReader)
{
	if (CommReader==NULL) return(-1);
	if (CommReader->MessageData) delete [] CommReader->MessageData;
	delete CommReader;
	return(0);
}
//---------------------------------------------------------------------------
int ResetCommReader(TCommReader *CommReader)
{
 CommReader->MessageHeaderBytesNeeded=sizeof(TMessageHeader);
 CommReader->MessageState=NEED_MESSSAGE_HEADER;
 return(0);
}
//---------------------------------------------------------------------------
int ReadData(TCommReader *CommReader)
{
 char *  MessageHeaderBytePointer=(char *)&CommReader->MessageHeader; 
 int     BytesToRead,BytesRead,retval;
 DWORD   BytesAvailable;

 if ( ioctlsocket (CommReader->Socket, FIONREAD, &BytesAvailable)!=0) 
   {
	printf("ioctlsocket Error::Reconnecting\n");
    return(-1);
   }			
 while  (BytesAvailable!=0)
  {
   switch (CommReader->MessageState)
    {
     case NEED_MESSSAGE_HEADER:
	      if (BytesAvailable>=CommReader->MessageHeaderBytesNeeded) BytesToRead=CommReader->MessageHeaderBytesNeeded;
	      else  BytesToRead=BytesAvailable;
	      BytesRead=recv(CommReader->Socket,(char *)&MessageHeaderBytePointer[sizeof(TMessageHeader)-CommReader->MessageHeaderBytesNeeded],BytesToRead, 0);
	      if ((BytesRead==SOCKET_ERROR) || (BytesRead==0) || (BytesRead == WSAECONNRESET)) 
			  {
				printf("recv error\n");
				return(-1);
		      }  
	      BytesAvailable-=BytesRead;
	      CommReader->MessageHeaderBytesNeeded-=BytesRead;
	      if (CommReader->MessageHeaderBytesNeeded==0) 
	        {
              CommReader->MessageHeader.MessageLength=htonl(CommReader->MessageHeader.MessageLength);
			  CommReader->MessageHeader.MessageType=htonl(CommReader->MessageHeader.MessageType);
			  if ( CommReader->MessageHeader.MessageLength>0)
			    {
		         CommReader->MessageState=NEED_MESSSAGE_DATA;
			      CommReader->MessageDataBytesNeeded= CommReader->MessageHeader.MessageLength;
			     if ( CommReader->MessageDataBytesNeeded>CommReader->SizeOfMessageData) 
			       {
				    printf("Message Data too large\n");
				    return(-1);
  			       }
			     }
			  else
			     {
				   CommReader->MessageHeaderBytesNeeded=sizeof(TMessageHeader);
				   CommReader->MessageState=NEED_MESSSAGE_HEADER;
				   if ((retval=CommReader->ProcessRecvdMessagePtr(CommReader->Socket,& CommReader->MessageHeader, CommReader->MessageData))<0)
				   {
					return(retval);
				   }
			     }
	        }
	      break;
	 case NEED_MESSSAGE_DATA:
	      if (BytesAvailable>= CommReader->MessageDataBytesNeeded) BytesToRead= CommReader->MessageDataBytesNeeded;
	      else  BytesToRead=BytesAvailable;
	      BytesRead=recv(CommReader->Socket,(char *)& CommReader->MessageData[ CommReader->MessageHeader.MessageLength- CommReader->MessageDataBytesNeeded],BytesToRead, 0);
	      if ((BytesRead==SOCKET_ERROR) || (BytesRead==0) || (BytesRead == WSAECONNRESET)) 
			{
		      printf("recv error %d\n");
		      return(-1);
		    }  
	      BytesAvailable-=BytesRead;
	      CommReader->MessageDataBytesNeeded-=BytesRead;
	      if ( CommReader->MessageDataBytesNeeded==0) 
	        {
              CommReader->MessageHeaderBytesNeeded=sizeof(TMessageHeader);
		      CommReader->MessageState=NEED_MESSSAGE_HEADER;
		      if ((retval=CommReader->ProcessRecvdMessagePtr(CommReader->Socket,& CommReader->MessageHeader, CommReader->MessageData))<0)
			    {
				 return(retval);
				}

	        }
	      break;
	 default:
		  printf("Should Not Happen\n");
		  break;
    }
 }
 return(0);
}
//---------------------------------------------------------------------------
static int WriteNetData( SOCKET Socket, char *Buffer, int BytestoSend)
{
int TotalBytesSent=0, BytesSent;
//printf("%s",Buffer);
while(TotalBytesSent< BytestoSend)
{
 BytesSent = send(Socket, Buffer, BytestoSend -TotalBytesSent, 0);

 if(BytesSent!=SOCKET_ERROR )
  {
   TotalBytesSent += BytesSent;
   Buffer += BytesSent;
  }
 else
 {
  int LastError=WSAGetLastError();
  if ((LastError==WSAEWOULDBLOCK) ||
	  (LastError==WSAENOBUFS))
    {
	 ::Sleep(10);
	 printf("*****************************HERE***********************\n");
     continue;
    }
  return(-1);
 }
}
 return(TotalBytesSent);
}
 //--------------------------------------------------------------------------
int SendData(SOCKET Socket, int MessageType,char *DataBuffer,int BufferLength)
{
 TMessageHeader MessageHeader;
 
 MessageHeader.MessageType=htonl(MessageType);
 MessageHeader.MessageLength=htonl(BufferLength);
 if (WriteNetData(Socket, (char *)&MessageHeader, sizeof(TMessageHeader))!=sizeof(TMessageHeader)) 
   {
	printf("Write Error 1\n");
    return(-1);
   }
  if ((DataBuffer!=NULL) && (BufferLength>0))
    {
     if (WriteNetData(Socket, (char *)DataBuffer,BufferLength)!=BufferLength)
		  {
			printf("Write Error 2\n");
		    return(-1);
	      }
    }
  return(0);
}
 //--------------------------------------------------------------------------
int SetSocketBufferSizes(SOCKET Socket,int InputSize,int OutputSize)
{
  int iResult;
   iResult = setsockopt( Socket, SOL_SOCKET, SO_SNDBUF   , (char *)&InputSize, sizeof(InputSize) ) ;
   if (iResult)
     {
	  return(-1);
     }

   iResult = setsockopt( Socket, SOL_SOCKET, SO_RCVBUF   , (char *)&OutputSize, sizeof(OutputSize) ) ;
   if (iResult)
     {
	  return(-2);
     }
   return(0);
}
 //--------------------------------------------------------------------------
int SetSocketTCPNoDelay(SOCKET Socket)
{
  int iResult;
  BOOL OnOff=1;

  iResult = setsockopt(Socket, SOL_SOCKET, TCP_NODELAY, (const char *)&OnOff,sizeof(OnOff));
  if (iResult == INVALID_SOCKET)
     {
	  return(-1);
     }
   return(0);
}
 //--------------------------------------------------------------------------