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

// FaceRecognitionClient.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <stdio.h>
#include <Winsock2.h>
#include <Windows.h>
#include "IniReader.h"
#include "cv.h"
#include "cvaux.h"
#include "highgui.h"
#include "IplImageJpegConversion.h"
#include "CommSupport.h"
#include "Spinner.h"
#define USE_TIMER
#undef  USE_TIMER
using namespace std;

#ifndef max
#define max(a,b)            (((a) > (b)) ? (a) : (b))
#endif

#ifndef min
#define min(a,b)            (((a) < (b)) ? (a) : (b))
#endif



#ifdef USE_TIMER
#define TARGET_RESOLUTION 1         // 1-millisecond target resolution
static HANDLE      hTimer=NULL;
static UINT        wTimerRes;
#endif

typedef enum
{
 RECOGNISE,
 TRAIN_MODE
} TFaceMode;


static SOCKET      ConnectSocket=INVALID_SOCKET;
static HANDLE      TerminateEventHandle=NULL;
static HANDLE      TCP_Events=NULL;
static sockaddr_in Server;
static BOOL        IsConnected=false;
static CvCapture*  Camera = NULL;	// The camera device.
static IplImage *  LastImg=NULL;
static char     *  JpegImageBuffer=NULL;
static DWORD       SizeOfJpegImageBuffer=1024*500;
static TFaceMode   FaceMode;

static TCommReader *CommReader;

static void  CleanUp(void);
static void  CleanUpTCP(void);
static LPSTR UnicodeToAnsi(LPCWSTR s);

static int   ProcessRecvdMessage(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData);
static BOOL  CtrlHandler(DWORD fdwCtrlType);
static int InitializeCamera(CvCapture**  camera);
static IplImage* getCameraFrame(CvCapture*  camera);
static int   SendIplImage(SOCKET Socket,IplImage *Frame);


int _tmain(int argc, _TCHAR* argv[])
{
#ifdef USE_TIMER
 HANDLE            Handles[4];
#else
 HANDLE            Handles[4];
#endif
 WSADATA           wsaData;
 unsigned short    Port;
 int               iResult;
 LPCWSTR           wtmpstr;
 WSANETWORKEVENTS  NetworkEvents;
 LPSTR             RemoteHost=NULL;
 int               NumHandles;
#ifdef USE_TIMER 
 DWORD             Period;
 LARGE_INTEGER     DueTime;
 double            FrameRate;
 TIMECAPS          tc;
#endif

 CIniReader Reader(L".//FaceRecognitionClient.ini");

 #ifdef USE_TIMER
 FrameRate=Reader.ReadFloat(L"CAPTURE", L"FRAMERATE", 0.0);

 if (FrameRate<=0.0) 
 {
	 printf("invalid Frame Rate\n");
	 return(0);
 }

 Period=(DWORD)(1000.0/FrameRate);
 printf("Period %d\n",Period);
#endif

 Port=Reader.ReadInteger(L"REMOTE_SERVER",L"PORT",9000);
 wtmpstr=Reader.ReadString(L"REMOTE_SERVER", L"IP_ADDRESS", L"");
 RemoteHost=UnicodeToAnsi(wtmpstr);
 delete[]wtmpstr;

 if (RemoteHost==NULL) 
   {
	printf("REMOTE_SERVER IP not defined in ini\n");
	return(-1);
   }

 printf("[REMOTE_SERVER] IPADDRESS         %s\n",RemoteHost);
 printf("[REMOTE_SERVER] PORT              %d\n\n",Port);


 SpinnerInit();

 Server.sin_family		= AF_INET;
 Server.sin_addr.s_addr	= inet_addr(RemoteHost);
 Server.sin_port		= htons(Port);

 JpegImageBuffer=new char[SizeOfJpegImageBuffer];

 TerminateEventHandle = CreateEvent(NULL,FALSE,FALSE,NULL);
 if (TerminateEventHandle==NULL)
   {
    printf("Create Event Handle Failed\n");
    CleanUp();
    return(-1);
   }
 
 // Create a GUI window for the user to see the camera image.
 cvNamedWindow("Client", CV_WINDOW_AUTOSIZE);
 cvWaitKey(1);

 if (InitializeCamera(&Camera))
   {
    printf("Initialize Camera Failed\n");
    CleanUp();
    return(-1);
   }
 SetConsoleCtrlHandler((PHANDLER_ROUTINE) CtrlHandler, TRUE);
 
 iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
 if (iResult != NO_ERROR) 
   {
    CleanUp();
	printf("WSAStartup Error\n");
	return(-1);
   }

 #ifdef USE_TIMER
 if (timeGetDevCaps(&tc, sizeof(TIMECAPS)) != TIMERR_NOERROR) 
  {
	printf("timeGetDevCaps ERROR\n");
  }
 
 wTimerRes = min(max(tc.wPeriodMin, TARGET_RESOLUTION), tc.wPeriodMax);
 timeBeginPeriod(wTimerRes); 
#endif

 while(1)
  {
   FaceMode=RECOGNISE;
   CleanUpTCP();
#ifdef USE_TIMER
   hTimer = CreateWaitableTimer(NULL, FALSE, NULL);
#endif
   ConnectSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
   CommReader=CreateCommReader(ConnectSocket,100*1024,ProcessRecvdMessage);
   if (ConnectSocket == INVALID_SOCKET)
     {
	  CleanUp();
	  printf("Socket Error\n"); 
	  return(-1);
     }

   if (SetSocketBufferSizes(ConnectSocket,500 * 1024,500 * 1024))
     {
	  CleanUp();
	  printf("Setsockopt Buffers Error\n"); 
	  return(-1);
     }

   if (SetSocketTCPNoDelay(ConnectSocket))
     {
	  CleanUp();
	  printf("Setsockopt TCP No Delay Error\n"); 
	  return(-1);
     }

   TCP_Events = WSACreateEvent();
   if (TCP_Events==WSA_INVALID_EVENT)
     {
      printf("WSAreate Event Handle Failed\n");
      CleanUp();
      return(-1);
     }

	/* Puts socket in non-blocking mode and
	 * tells windows to signal TCP_EventsMJpeg when the
	 * connect() has completed or an error occurs. */
   WSAEventSelect(ConnectSocket, TCP_Events, FD_CONNECT |FD_READ|FD_CLOSE);
   connect(ConnectSocket, (struct sockaddr *) &Server, sizeof(struct sockaddr_in));



   Handles[0]=TerminateEventHandle;
   Handles[1]=TCP_Events;
   Handles[2]=GetStdHandle(STD_INPUT_HANDLE);
   NumHandles=3;
#ifdef USE_TIMER
   Handles[3]=hTimer;
   NumHandles=4;
#endif
   FlushConsoleInputBuffer(Handles[2]);
   
   while(1)
	{
	 iResult=WaitForMultipleObjects(NumHandles,Handles,false,INFINITE);

	 if ( iResult== WAIT_OBJECT_0)
	   {
	    printf("Terminate Received\n");
	    CleanUp();
        return(0); 
	   }
	 else if ( iResult== WAIT_OBJECT_0+1)
	   {
	    WSAEnumNetworkEvents(ConnectSocket,TCP_Events,&NetworkEvents);
	    if (NetworkEvents.lNetworkEvents & FD_CONNECT)
		  {
		   if (NetworkEvents.iErrorCode[FD_CONNECT_BIT] == 0)
		     {
			  IsConnected=true;
#ifdef USE_TIMER
			  DueTime.QuadPart=0;
              if (!SetWaitableTimer(hTimer, &DueTime, Period, NULL, NULL, FALSE)) 
			    {
                 printf("Time Failed\n");
			     break;
	            }
#endif
              printf("Connected\n");
			  ResetCommReader(CommReader);
#ifndef USE_TIMER
		      LastImg = getCameraFrame(Camera);
		      if (LastImg==NULL) 
		        {
			     printf("ERROR in recognizeFromCam(): Bad input image!\n");
		        }
		      else
		        {
		          if (SendIplImage(ConnectSocket,LastImg))
		             {
				       printf("Send  Error\n");
				       break;
		             }
			    }
#endif
		     }
		   else
		     {	       
			  printf("Connect Failed::Reconnecting\n");
              break;
		     }
	      }
	    else if (NetworkEvents.lNetworkEvents & FD_READ)
	      {
		   if (NetworkEvents.iErrorCode[FD_READ_BIT] == 0)
		     {
			  if (ReadData(CommReader)<0)
			     {
			       printf("ReadData Error::Reconnecting\n");
			       break;			
			     }
		     }
		   } 
	    else if (NetworkEvents.lNetworkEvents & FD_CLOSE)
	      {
		   if (NetworkEvents.iErrorCode[FD_CLOSE_BIT] == 0)
		     {
			  printf("Close Received::Reconnecting\n");
              break;
		     }
		   else
		     {
			  printf("Close Aprupt::Reconnecting\n");
              break;
		      }
	      } 
       }

	 else if ( iResult== WAIT_OBJECT_0+2)
	   {
		 INPUT_RECORD InRec;
         DWORD NumRead;
		 if (ReadConsoleInput(Handles[2],&InRec,1,&NumRead))
		   {
			if (InRec.EventType==KEY_EVENT)
			  {
			    if (InRec.Event.KeyEvent.bKeyDown)
				  {
				   if ((isalpha(InRec.Event.KeyEvent.uChar.AsciiChar)) || (isdigit(InRec.Event.KeyEvent.uChar.AsciiChar)))
					  {
                       if ((InRec.Event.KeyEvent.uChar.AsciiChar=='n') || (InRec.Event.KeyEvent.uChar.AsciiChar=='N'))
						 {
						  if (FaceMode==RECOGNISE)
						    {
						     TMessageTrainStartRequest MessageTrainStartRequest; 
				             printf("Enter Name: ");
						     if (fgets(MessageTrainStartRequest.Name,sizeof(MessageTrainStartRequest.Name),stdin))
						       {
								MessageTrainStartRequest.Name[strlen(MessageTrainStartRequest.Name)-1]=NULL; // Remove new line
							    int MessageLen=sizeof(MessageTrainStartRequest) - sizeof(MessageTrainStartRequest.Name)+strlen(MessageTrainStartRequest.Name)+1;
                                if (SendData(ConnectSocket, MESSAGE_START_TRAIN_MODE_REQUEST,(char *)&MessageTrainStartRequest,MessageLen)==0) FaceMode=TRAIN_MODE;
								else break;	
						       }
						     }
						  else printf("Invaild Mode: Already in training mode\n");
						 }
                       else if ((InRec.Event.KeyEvent.uChar.AsciiChar=='t') || (InRec.Event.KeyEvent.uChar.AsciiChar=='T'))
						 {
						  if (FaceMode== TRAIN_MODE)
						    {
                             if (SendData(ConnectSocket, MESSAGE_END_TRAIN_MODE_REQUEST,(char *)NULL,0)==0) FaceMode=RECOGNISE;
							 else break;
						    }
						  else printf("Invaild Mode: Not in training mode\n");
						 }
					  }
				   }
			  }
            else if (InRec.EventType== MOUSE_EVENT)
			  {
			  }
            else if (InRec.EventType==FOCUS_EVENT)
			  {
			  }           
            else if (InRec.EventType==MENU_EVENT)
			  {
			  }           
            else if (InRec.EventType=WINDOW_BUFFER_SIZE_EVENT)
			  {
			  }           
            else
			  {
			  }           

		   }
	   }
#ifdef USE_TIMER
	 else if ( iResult== WAIT_OBJECT_0+3)
	   {
		// Get the camera frame
		LastImg = getCameraFrame(Camera);
		if (LastImg==NULL) 
		  {
			printf("ERROR in recognizeFromCam(): Bad input image!\n");
		  }
		else
		  {
		   IplImage *shownImg;
		   // Display the image.
		   // Show the data on the screen.
		   shownImg = cvCloneImage(LastImg);
		   cvShowImage("Client", shownImg);
		   cvReleaseImage( &shownImg );
		   cvWaitKey(1);
		   if (SendIplImage(ConnectSocket,LastImg))
		     {
				printf("Send  Error\n");
				break;
		     }
		 }
		//printf("Timer\n");
	   }
#endif
	 else
	   {
	    printf("Timeout::Reconnecting\n");
		break;
	   }
    }
  }
 return 0;
}

//---------------------------------------------------------------------------
static void CleanUp(void)
{
 CleanUpTCP();

 if ((TerminateEventHandle!=INVALID_HANDLE_VALUE)&& (TerminateEventHandle!=NULL))
 {
  CloseHandle(TerminateEventHandle);
  TerminateEventHandle=INVALID_HANDLE_VALUE;
 }

 WSACleanup();
#ifdef USE_TIMER
 timeEndPeriod(wTimerRes);
#endif
  // Free the camera and memory resources used.
 if (Camera) 
  {
   cvReleaseCapture( &Camera );
   Camera=NULL;
  }
 
 if (JpegImageBuffer)
  {
	delete[] JpegImageBuffer;
    JpegImageBuffer=NULL;
  }
printf("Done\n");
}

//---------------------------------------------------------------------------
static void CleanUpTCP(void)
{
 IsConnected=false;
#ifdef USE_TIMER
 if  (hTimer!=NULL)
   {
    CancelWaitableTimer(hTimer);
    CloseHandle(hTimer);
    hTimer=NULL;
   }
#endif
 if (ConnectSocket!=INVALID_SOCKET)
   {
    shutdown(ConnectSocket,SD_BOTH);
    closesocket (ConnectSocket);
    ConnectSocket=INVALID_SOCKET;
   }
 if ((TCP_Events=INVALID_HANDLE_VALUE)&& (TCP_Events!=NULL))
   {
    CloseHandle(TCP_Events);
    TCP_Events=INVALID_HANDLE_VALUE;
   }
 if ((TerminateEventHandle!=INVALID_HANDLE_VALUE)&& (TerminateEventHandle!=NULL))
   {
    ResetEvent(TerminateEventHandle);
   }

 if ((TerminateEventHandle!=INVALID_HANDLE_VALUE)&& (TerminateEventHandle!=NULL))
   {
    ResetEvent(TerminateEventHandle);
   }
 if (CommReader)
	{
     DeleteCommReader(CommReader);
	 CommReader=NULL;
    }
}
//--------------------------------------------------------------
static LPSTR UnicodeToAnsi(LPCWSTR s)
{
if (s==NULL) return NULL;
int cw=lstrlenW(s);
if (cw==0) {CHAR *psz=new CHAR[1];*psz='\0';return psz;}
int cc=WideCharToMultiByte(CP_ACP,0,s,cw,NULL,0,NULL,NULL);
if (cc==0) return NULL;
CHAR *psz=new CHAR[cc+1];
cc=WideCharToMultiByte(CP_ACP,0,s,cw,psz,cc,NULL,NULL);
if (cc==0) {delete[] psz;return NULL;}
psz[cc]='\0';
return psz;
}
//---------------------------------------------------------------------------
static BOOL CtrlHandler(DWORD fdwCtrlType)
{
 SetEvent(TerminateEventHandle);
 return(TRUE);
}
//---------------------------------------------------------------------------
static int ProcessRecvdMessage(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData)
{
  TMessageImageResponse            * MessageImageResponse;
  TMessageTrainStartResponse       * MessageTrainStartResponse;
  TMessageTrainingCollectionUpdate * MessageTrainingCollectionUpdate;
  int                              * tmp;
  char                               cstr[256];

  //printf("Data In %d\n",MessageHeader->MessageLength);
  switch(MessageHeader->MessageType)
    {
     case MESSAGE_TYPE_IMAGE_REPONSE:
		  IplImage *shownImg;
#ifndef USE_TIMER
		  LastImg = getCameraFrame(Camera);
		  if (LastImg==NULL) 
		    {
	         printf("ERROR in recognizeFromCam(): Bad input image!\n");
		    }
		  else
		    {
		     if (SendIplImage(Socket,LastImg))
		       {
				printf("Send  Error\n");
				return(-1);
		       }
			 } 
#endif
		  MessageImageResponse=(TMessageImageResponse *)MessageData;
          tmp=(int *)&MessageImageResponse->confidence;
          *tmp=ntohl(*tmp);
          MessageImageResponse->DetectTimeInMs=ntohl(MessageImageResponse->DetectTimeInMs);
          MessageImageResponse->DrawRect=ntohl(MessageImageResponse->DrawRect);
          MessageImageResponse->FaceRect.height=ntohl(MessageImageResponse->FaceRect.height);
          MessageImageResponse->FaceRect.width=ntohl(MessageImageResponse->FaceRect.width);
          MessageImageResponse->FaceRect.x=ntohl(MessageImageResponse->FaceRect.x);
          MessageImageResponse->FaceRect.y=ntohl(MessageImageResponse->FaceRect.y);
          MessageImageResponse->HavePerson=ntohl(MessageImageResponse->HavePerson);
          MessageImageResponse->ObjectsFound=ntohl(MessageImageResponse->ObjectsFound);
		  shownImg = cvCloneImage(LastImg);
		  printf("[Face Detection took %d ms and found %d objects]\n", MessageImageResponse->DetectTimeInMs,MessageImageResponse->ObjectsFound);
		  if (MessageImageResponse->DrawRect)
		    {
				cvRectangle(shownImg, cvPoint(MessageImageResponse->FaceRect.x, MessageImageResponse->FaceRect.y), 
					                  cvPoint(MessageImageResponse->FaceRect.x + MessageImageResponse->FaceRect.width-1, 
									  MessageImageResponse->FaceRect.y + MessageImageResponse->FaceRect.height-1), CV_RGB(0,255,0), 1, 8, 0);
		    }
		  if  (MessageImageResponse->HavePerson)
		    {
			  CvFont font;
			  cvInitFont(&font,CV_FONT_HERSHEY_PLAIN, 1.0, 1.0, 0,1,CV_AA);
			  CvScalar textColor = CV_RGB(0,255,255);	// light blue text
			  char text[256];
			  sprintf_s(text, sizeof(text)-1, "Name: '%s'", MessageImageResponse->Name);
			  cvPutText(shownImg, text, cvPoint( MessageImageResponse->FaceRect.x, MessageImageResponse->FaceRect.y +  MessageImageResponse->FaceRect.height + 15), &font, textColor);
			  sprintf_s(text, sizeof(text)-1, "Confidence: %f",  MessageImageResponse->confidence);
			  cvPutText(shownImg, text, cvPoint(MessageImageResponse->FaceRect.x, MessageImageResponse->FaceRect.y + MessageImageResponse->FaceRect.height + 30), &font, textColor);
			  printf("Most likely person in camera: '%s' (confidence=%f.\n", MessageImageResponse->Name, MessageImageResponse->confidence);
		    }
		   cvShowImage("Client", shownImg);
		   cvReleaseImage( &shownImg );
		   cvWaitKey(1);

		   break;
     case MESSAGE_START_TRAIN_MODE_RESPONSE:
		   MessageTrainStartResponse=(TMessageTrainStartResponse *)MessageData;
		   printf("Collecting all images until you hit 't', to start Training the images as '%s' ...\n",  MessageTrainStartResponse->Name);
		   break;
     case MESSAGE_END_TRAIN_MODE_RESPONSE:
		   printf("Recognizing person in the camera ...\n");
		   break;
     case MESSAGE_TRAINING_COLLECTION_UPDATE:
           MessageTrainingCollectionUpdate=(TMessageTrainingCollectionUpdate *)MessageData;
		   MessageTrainingCollectionUpdate->newPersonFaces=ntohl(MessageTrainingCollectionUpdate->newPersonFaces);
		   MessageTrainingCollectionUpdate->nPersons=ntohl(MessageTrainingCollectionUpdate->nPersons);
		   sprintf(cstr, "Data/%d_%s%d.pgm", MessageTrainingCollectionUpdate->nPersons+1, MessageTrainingCollectionUpdate->Name, MessageTrainingCollectionUpdate->newPersonFaces+1);
		   printf("Storing the current face of '%s' into image '%s'.\n", MessageTrainingCollectionUpdate->Name, cstr);
		   break;
	 default:
		     printf("Invalid Message Received\n");
			 break;

    }
  return(0);
}
//---------------------------------------------------------------------------
// Grab the next camera frame. Waits until the next frame is ready,
// and provides direct access to it, so do NOT modify the returned image or free it!
// Will automatically initialize the camera on the first frame.
static IplImage* getCameraFrame(CvCapture*  camera)
{
	IplImage *frame;
	// If the camera hasn't been initialized, then open it.
	frame = cvQueryFrame( camera );
	if (!frame) {
		fprintf(stderr, "ERROR in recognizeFromCam(): Could not access the camera or video file.\n");
		return NULL;
	}
	return frame;
}
 //---------------------------------------------------------------------------
static int InitializeCamera(CvCapture**  camera)
{
  IplImage *frame;
  printf("Acessing the camera ...\n");
 *camera = cvCaptureFromCAM( 0 );
 if (!*camera) 
   {
	printf("ERROR in getCameraFrame(): Couldn't access the camera.\n");
	return(-1);
   }
 // Try to set the camera resolution
 cvSetCaptureProperty( *camera, CV_CAP_PROP_FRAME_WIDTH, 320 );
 cvSetCaptureProperty( *camera, CV_CAP_PROP_FRAME_HEIGHT, 240 );
 // Wait a little, so that the camera can auto-adjust itself
 Sleep(1000);	// (in milliseconds)
 frame = cvQueryFrame( *camera );	// get the first frame, to make sure the camera is initialized.
 if (frame) 
   {
	printf("Got a camera using a resolution of %dx%d.\n", (int)cvGetCaptureProperty( *camera, CV_CAP_PROP_FRAME_WIDTH), (int)cvGetCaptureProperty( *camera, CV_CAP_PROP_FRAME_HEIGHT) );
   }
 return(0);
}
 //---------------------------------------------------------------------------
 static int SendIplImage(SOCKET Socket,IplImage *Frame)
 {
  
  int OutputBufferSize=SizeOfJpegImageBuffer;
 
  if (!IplImage2Jpeg(Frame,80,JpegImageBuffer,&OutputBufferSize))
   {
     if (OutputBufferSize>0)
	 {
	  //printf("OutputBufferSize %d\n",OutputBufferSize);
	  if (SendData(Socket, MESSAGE_TYPE_JPEG_IMAGE,(char *)JpegImageBuffer,OutputBufferSize))
		  {
			printf("Write Error\n");
		    return(-1);
	      }
	 }
    else 
    {
	  printf("Jpeg Conversion Error\n");
      return(-2);
    }
  }
  else 
    {
	  printf("Jpeg Conversion Error\n");
      return(-2);
    }
  return(0);
 }
 //---------------------------------------------------------------------------
