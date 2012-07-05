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

// FaceRecognitionServer.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <winsock2.h>
#include <malloc.h>
#include <direct.h>
#include "IniReader.h"
#include "jpeglib.h"
#include "CommSupport.h"
#include "IplImageJpegConversion.h"
#include "SaveBufferToFile.h"
#include "Spinner.h"

//---------------------------------------------------------------------------

typedef enum
{
 RECOGNISE,
 TRAIN_MODE
} TFaceMode;

static HANDLE      TerminateEventHandle=NULL;
static HANDLE      TCP_Events=WSA_INVALID_EVENT,TCP_Events_Listen=WSA_INVALID_EVENT;
static HANDLE      FileHandle=INVALID_HANDLE_VALUE;
static SOCKET      ListenSocket=INVALID_SOCKET,AcceptSocket=INVALID_SOCKET;

static TCommReader *CommReader;
static DWORD       FrameNumber=0;
static  TFaceMode  FaceMode=RECOGNISE;

//Face Recognition Vars Start	
static int                      SAVE_EIGENFACE_IMAGES = 1;// Set to 0 if you dont want images of the Eigenvectors saved to files (for debugging).
static IplImage              ** faceImgArr    = 0; // array of face images
static const char             * faceCascadeFilename = "haarcascade_frontalface_alt.xml";
static char                     faceCascadeFilenameWithPath[1024];
static const char             * FaceDataFilename = "facedata.xml";
static char                     FaceDataFilenameWithPath[1024];
static const char             * TrainFilename = "train.txt";
static char                     TrainFilenameWithPath[1024];
static const char             * OutAverageImageFilename = "out_averageImage.bmp";
static char                     OutAverageImageFilenameWithPath[1024];
static const char             * OutEigenfacesFilename = "out_eigenfaces.bmp";
static char                     OutEigenfacesFilenameWithPath[1024];


static CvMat                  * trainPersonNumMat; // the person numbers during training
static int                      faceWidth    = 120;// Default dimensions for faces in the face recognition database.
static int                      faceHeight   = 90; //	"		"		"		"		"		"		"		"
static int                      nPersons     = 0;  // the number of people in the training set.
static IplImage               * pAvgTrainImg = 0;  // the average image
static int                      nTrainFaces  = 0;  // the number of training images
static int                      nEigens      = 0;  // the number of eigenvalues
static IplImage              ** eigenVectArr = 0;  // eigenvectors
static CvMat                  * eigenValMat  = 0;  // eigenvalues
static float                  * projectedTestFace;
static CvHaarClassifierCascade* faceCascade;
static vector<string>           personNames;       // array of person names (indexed by the person number).
static CvMat                  * projectedTrainFaceMat = 0; // projected training faces
static CvMat                  * personNumTruthMat = 0; // array of person numbers
static BOOL                     saveNextFaces = FALSE;
static char                     newPersonName[256];
static int                      newPersonFaces;
static bool                     ShowImageDisplay=true;
static bool                     USE_MAHALANOBIS_DISTANCE=false; // You might get better recognition accuracy if you enable this.
static char                     *DataPath;
//Face Recognition Vars End


static int       ProcessReceivedImage(IplImage *camImg);
static BOOL      CtrlHandler(DWORD fdwCtrlType);
static void      CleanupExit(void);
static int       ProcessRecvdMessage(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData);
static int       ProcessReceivedImage(SOCKET Socket,IplImage *camImg);
static void      StoreTrainingData(void);
static int       LoadTrainingData(CvMat ** pTrainPersonNumMat);
static IplImage* ConvertImageToGreyscale(const IplImage *imageSrc);
static CvRect    DetectFaceInImage(const IplImage *inputImg, const CvHaarClassifierCascade* cascade, int *Time, int *NumObects);
static IplImage* CropImage(const IplImage *img, const CvRect region);
static IplImage* ResizeImage(const IplImage *origImg, int newWidth, int newHeight);
static int       FindNearestNeighbor(float * projectedTestFace, float *pConfidence);
static int       StartTrainingMode(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData);
static int       EndTrainingMode(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData);
static CvMat*    RetrainOnline(void);
static int       Learn(char *szFileTrain,bool UsePath);
static int       LoadFaceImgArray(char * filename,bool UsePath);
static void      DoPCA(void);
static void      StoreEigenfaceImages(void);
static IplImage* ConvertFloatImageToUcharImage(const IplImage *srcImg);
static void      PrintUsage(_TCHAR* Arg0);
static LPSTR     UnicodeToAnsi(LPCWSTR s);
static void      RecognizeFileList(char *szFileTest,bool UsePath);
//---------------------------------------------------------------------------

int _tmain(int argc, _TCHAR* argv[])
{
 DWORD             iResult;
 WORD              wVersionRequested;
 WSADATA           wsaData;
 WSANETWORKEVENTS  NetworkEvents;
 sockaddr_in       Service;
 int               wsaerr;
 HANDLE            Handles[3];
 DWORD             NumHandles;
 unsigned short    Port;
 LPCWSTR           wtmpstr;


 CIniReader Reader(L".//FaceRecognitionServer.ini");
 
 Port=Reader.ReadInteger(L"SERVER_PORT",L"PORT",9000);

 wtmpstr=Reader.ReadString(L"DATA_STORAGE",L"PATH",L"");
 DataPath=UnicodeToAnsi(wtmpstr);
 delete[]wtmpstr;
 printf("Data Path is %s\n",DataPath);
 sprintf(faceCascadeFilenameWithPath,"%s\\%s",DataPath,faceCascadeFilename);
 sprintf(FaceDataFilenameWithPath,"%s\\%s",DataPath,FaceDataFilename);
 sprintf(TrainFilenameWithPath,"%s\\%s",DataPath,TrainFilename);
 sprintf(OutAverageImageFilenameWithPath,"%s\\%s",DataPath,OutAverageImageFilename);
 sprintf(OutEigenfacesFilenameWithPath,"%s\\%s",DataPath,OutEigenfacesFilename);
 ShowImageDisplay=Reader.ReadBoolean(L"IMAGEDISPLAY",L"SHOWIMAGEDISPLAY",true);

  PrintUsage(argv[0]);


  if( argc >= 2 && _wcsicmp(argv[1], L"train") == 0 ) 
    {
		char *szFileTrain;
		if (argc == 3)
			szFileTrain = UnicodeToAnsi(argv[2]);	// use the given arg
		else 
		 {
		   printf("ERROR: No training file given.\n");
		   return 1;
	     }
		Learn(szFileTrain,false);
		delete [] szFileTrain;
		return(0);
	 }
   else if( argc >= 2 && _wcsicmp(argv[1], L"test") == 0)
	{
		char *szFileTest;
		if (argc == 3)
			szFileTest = UnicodeToAnsi(argv[2]);	// use the given arg
		else 
		{
			printf("ERROR: No testing file given.\n");
			return 1;
		}
		RecognizeFileList(szFileTest,false);
		delete [] szFileTest;
		return(0);
	}

 SpinnerInit();

 SetPriorityClass(GetCurrentProcess(),REALTIME_PRIORITY_CLASS);
 SetThreadPriority(GetCurrentThread(),THREAD_PRIORITY_TIME_CRITICAL);


if (ShowImageDisplay)
  {
	cvNamedWindow("Server", CV_WINDOW_AUTOSIZE);
    cvWaitKey(1);
  }

USE_MAHALANOBIS_DISTANCE=Reader.ReadBoolean(L"RECOGNITION",L"USE_MAHALANOBIS_DISTANCE",false);
printf("[RECOGNITION] USE_MAHALANOBIS_DISTANCE=%s\n",USE_MAHALANOBIS_DISTANCE?"True":"False");

 TerminateEventHandle = CreateEvent(NULL,FALSE,FALSE,NULL);
 if (TerminateEventHandle==NULL)
   {
    printf("Create Event Handle Failed\n");
	CleanupExit();
    return(-1);
   }


 SetConsoleCtrlHandler((PHANDLER_ROUTINE) CtrlHandler, TRUE); 

 wVersionRequested = MAKEWORD(2, 2);
 wsaerr = WSAStartup(wVersionRequested, &wsaData);

 if (wsaerr != 0)
   {
     printf("Server: The Winsock dll not found!\n");
	 CleanupExit();
     return -1;
   }

 trainPersonNumMat = 0;  // the person numbers during training
 projectedTestFace = 0;
 saveNextFaces = FALSE;
 newPersonFaces = 0;

 printf("Recognizing person in the camera ...\n");

 // Load the previously saved training data
 if ( LoadTrainingData( &trainPersonNumMat ) ) 
   {
		faceWidth = pAvgTrainImg->width;
		faceHeight = pAvgTrainImg->height;
   }

  // Project the test images onto the PCA subspace
  projectedTestFace = (float *)cvAlloc( nEigens*sizeof(float) );

  // Load the HaarCascade classifier for face detection.
  faceCascade = (CvHaarClassifierCascade*)cvLoad(faceCascadeFilenameWithPath, 0, 0, 0 );
  if( !faceCascade ) {
		printf("ERROR in recognizeFromCam(): Could not load Haar cascade Face detection classifier in '%s'...\n", faceCascadeFilenameWithPath);
		CleanupExit();
		return -1;
	}

while(1)
 {
 // Re-initialize the local data.
  saveNextFaces = FALSE;
  newPersonFaces = 0;
  FaceMode=RECOGNISE;

  ListenSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

  if (ListenSocket == INVALID_SOCKET)
   {
    printf("Server: Error at socket(): %ld\n", WSAGetLastError());
    CleanupExit();
    return -1;
   }
  Service.sin_family = AF_INET;

  Service.sin_addr.s_addr = INADDR_ANY;
  
  Service.sin_port = htons(Port);

  if (bind(ListenSocket, (SOCKADDR*)&Service, sizeof(Service)) == SOCKET_ERROR)
   {
     printf("Server: bind() failed: %ld.\n", WSAGetLastError());
     CleanupExit();
     return 0;
   }
 
  if (listen(ListenSocket, 10) == SOCKET_ERROR)
   printf("Server: listen(): Error listening on socket %ld.\n", WSAGetLastError());
  else
   printf("Server: listen() , waiting for connections on port %d...\n",Port);
 

  TCP_Events_Listen = WSACreateEvent();
   if (TCP_Events_Listen==WSA_INVALID_EVENT)
     {
      printf("WSAreate Event Handle Failed\n");
	  CleanupExit();
      return(-1);
   }
  WSAEventSelect(ListenSocket, TCP_Events_Listen, FD_ACCEPT);

 
  TCP_Events = WSACreateEvent();
  if (TCP_Events==WSA_INVALID_EVENT)
     {
      printf("WSAreate Event Handle Failed\n");
	  CleanupExit();
      return(-1);
     }
 

  NumHandles=3;
  Handles[0]=TerminateEventHandle;
  Handles[1]=TCP_Events;
  Handles[2]=TCP_Events_Listen;
  while(1)
    {
     iResult=WaitForMultipleObjects(NumHandles,Handles,false,INFINITE);
     if (iResult== WAIT_OBJECT_0+0)
	   {
	    printf("Terminate Received\n");
	    CleanupExit();
	    return(-1);
	   }
	 else if (iResult== WAIT_OBJECT_0+1)
	   {
	     WSAEnumNetworkEvents(AcceptSocket,TCP_Events,&NetworkEvents);
         if (NetworkEvents.lNetworkEvents & FD_READ)
	       {

		    if (NetworkEvents.iErrorCode[FD_READ_BIT] == 0)
		      {
			   if (ReadData(CommReader)<0)
			      {
			       printf("ReadData Error\n");
				   if (AcceptSocket!=INVALID_SOCKET)
				     {
					  if (CommReader)
						  {
							DeleteCommReader(CommReader);
							CommReader=NULL;
					      }
 	                  closesocket(AcceptSocket);
                      AcceptSocket=INVALID_SOCKET;
				     }
			       break;			
			      }
 	          }
	       }
	     else if (NetworkEvents.lNetworkEvents & FD_CLOSE)
	      {
		   if (NetworkEvents.iErrorCode[FD_CLOSE_BIT] != 0)
		     {
              printf("Close Error %d\n",NetworkEvents.iErrorCode[FD_CLOSE_BIT]);
		     }
		   printf("Close Received\n");
           if (AcceptSocket!=INVALID_SOCKET)
             {
 	          closesocket(AcceptSocket);
			  if (CommReader)
				{
				 DeleteCommReader(CommReader);
				 CommReader=NULL;
			    }
              AcceptSocket=INVALID_SOCKET;
             }
           if (TCP_Events_Listen!=WSA_INVALID_EVENT)
             {
	          WSACloseEvent(TCP_Events_Listen);
	          TCP_Events_Listen=WSA_INVALID_EVENT;
             }
           break;
	      }
	   }
     else if (iResult== WAIT_OBJECT_0+2)
	   {
	    WSAEnumNetworkEvents(ListenSocket,TCP_Events_Listen,&NetworkEvents);
        if (NetworkEvents.lNetworkEvents & FD_ACCEPT)
	      {
		   if (NetworkEvents.iErrorCode[FD_ACCEPT_BIT] == 0)
		     {
               AcceptSocket = accept(ListenSocket, NULL, NULL);
              printf("Server: Client Connected!\n");
			  CommReader=CreateCommReader(AcceptSocket,100*1024,ProcessRecvdMessage);
			  if (SetSocketBufferSizes(AcceptSocket,500 * 1024,500 * 1024)==0)
			      {
                   printf("Socket Buffers Set\n");
                  }	
			  if (SetSocketTCPNoDelay(AcceptSocket)==0)
			      {
                   printf("Set TCP_NODELAY: TRUE\n");
                  }
	          closesocket(ListenSocket);
			  ListenSocket=INVALID_SOCKET ;
              WSAEventSelect(AcceptSocket, TCP_Events, FD_READ|FD_CLOSE);
			  NumHandles=2;
			  WSACloseEvent(TCP_Events_Listen);
			  TCP_Events_Listen=WSA_INVALID_EVENT;

		     }
	      }
	   }
   }
 }

  return 0;
 
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
  IplImage *Frame;

  int retval=0;

  //printf("Data In %d\n",MessageHeader->MessageLength);
  switch (MessageHeader->MessageType)
   {
    case  MESSAGE_TYPE_JPEG_IMAGE:
                          //SaveBufferToFile(L"Data\\Image",FrameNumber,L".jpg",(char *)MessageData,MessageHeader->MessageLength);
                          FrameNumber++;
                          Frame=Jpeg2IplImage((char *)MessageData,MessageHeader->MessageLength);
                          if (ProcessReceivedImage(Socket,Frame)<0) retval=-1;
                          cvReleaseImage( &Frame );
						  break;
    case  MESSAGE_START_TRAIN_MODE_REQUEST:
		                  if (StartTrainingMode(Socket,MessageHeader,MessageData)<0) retval=-1;
		                  break;
    case  MESSAGE_END_TRAIN_MODE_REQUEST:
		                  if (EndTrainingMode(Socket,MessageHeader,MessageData)<0) retval=-1;
		                  break;
	default:
		                  printf("Bad Message received\n");
						  break;
   }
  return(retval);
}
//---------------------------------------------------------------------------
static int ProcessReceivedImage(SOCKET Socket,IplImage *camImg)
{
  int                    iNearest, nearest;
  IplImage              *greyImg;
  IplImage              *faceImg;
  IplImage              *sizedImg;
  IplImage              *equalizedImg;
  IplImage              *processedFaceImg;
  IplImage              *shownImg;
  CvRect                 faceRect;
  float                  confidence;
  TMessageImageResponse  MessageImageResponse;

  // Make sure the image is greyscale, since the Eigenfaces is only done on greyscale image.
  greyImg = ConvertImageToGreyscale(camImg);

  // Perform face detection on the input image, using the given Haar cascade classifier.
  faceRect = DetectFaceInImage(greyImg, faceCascade,&MessageImageResponse.DetectTimeInMs,&MessageImageResponse.ObjectsFound );
  // Make sure a valid face was detected.
  if (faceRect.width > 0) 
    {
	 faceImg = CropImage(greyImg, faceRect);	// Get the detected face image.
     // Make sure the image is the same dimensions as the training images.
	 sizedImg = ResizeImage(faceImg, faceWidth, faceHeight);
	 // Give the image a standard brightness and contrast, in case it was too dark or low contrast.
	 equalizedImg = cvCreateImage(cvGetSize(sizedImg), 8, 1);	// Create an empty greyscale image
	 cvEqualizeHist(sizedImg, equalizedImg);
	 processedFaceImg = equalizedImg;
	 if (!processedFaceImg) 
	   {
		printf("ERROR in recognizeFromCam(): Don't have input image!\n");
		exit(1);
	   }

	 // If the face rec database has been loaded, then try to recognize the person currently detected.
	 if (nEigens > 0) 
	   {
		// project the test image onto the PCA subspace
		cvEigenDecomposite(
					processedFaceImg,
					nEigens,
					eigenVectArr,
					0, 0,
					pAvgTrainImg,
					projectedTestFace);

		// Check which person it is most likely to be.
		iNearest = FindNearestNeighbor(projectedTestFace, &confidence);
		nearest  = trainPersonNumMat->data.i[iNearest];

		printf("Most likely person in camera: '%s' (confidence=%f.\n", personNames[nearest-1].c_str(), confidence);

	   }//endif nEigens

	 // Possibly save the processed face to the training set.
	 if (saveNextFaces) 
	   {
		char cstr[256];
		TMessageTrainingCollectionUpdate MessageTrainingCollectionUpdate;

		MessageTrainingCollectionUpdate.nPersons=htonl(nPersons);
		MessageTrainingCollectionUpdate.newPersonFaces=htonl(newPersonFaces);
		strcpy(MessageTrainingCollectionUpdate.Name,newPersonName);
	    int MessageLen=sizeof(MessageTrainingCollectionUpdate) - sizeof(MessageTrainingCollectionUpdate.Name)+strlen(MessageTrainingCollectionUpdate.Name)+1;
        if (SendData(Socket, MESSAGE_TRAINING_COLLECTION_UPDATE,(char *)&MessageTrainingCollectionUpdate,MessageLen)<0) return(-1);
		// Use a different filename each time.
		sprintf(cstr, "%s\\Data\\%d_%s%d.pgm", DataPath,nPersons+1, newPersonName, newPersonFaces+1);
		printf("Storing the current face of '%s' into image '%s'.\n", newPersonName, cstr);
		cvSaveImage(cstr, processedFaceImg, NULL);
		newPersonFaces++;
	   }

	 // Free the resources used for this frame.
	 cvReleaseImage( &greyImg );
	 cvReleaseImage( &faceImg );
	 cvReleaseImage( &sizedImg );
	 cvReleaseImage( &equalizedImg );
    }

  MessageImageResponse.DrawRect=false;
  MessageImageResponse.HavePerson=false;
  MessageImageResponse.Name[0]=NULL;
  // Show the data on the screen.
  if (ShowImageDisplay) shownImg = cvCloneImage(camImg);
  if (faceRect.width > 0) 
    {
		MessageImageResponse.DrawRect=true;
	  // Check if a face was detected.
	  // Show the detected face region.
	  MessageImageResponse.FaceRect.x=faceRect.x;
	  MessageImageResponse.FaceRect.y=faceRect.y;
	  MessageImageResponse.FaceRect.height=faceRect.height;
	  MessageImageResponse.FaceRect.width=faceRect.width;
	  if (ShowImageDisplay) cvRectangle(shownImg, cvPoint(faceRect.x, faceRect.y), cvPoint(faceRect.x + faceRect.width-1, faceRect.y + faceRect.height-1), CV_RGB(0,255,0), 1, 8, 0);
	  if (nEigens > 0)
	    {	
		 MessageImageResponse.HavePerson=true;
		 // Check if the face recognition database is loaded and a person was recognized.
		 // Show the name of the recognized person, overlayed on the image below their face.
		 if (ShowImageDisplay)
		   {
		    CvFont font;
		    cvInitFont(&font,CV_FONT_HERSHEY_PLAIN, 1.0, 1.0, 0,1,CV_AA);
		    CvScalar textColor = CV_RGB(0,255,255);	// light blue text
		    char text[256];
		    sprintf_s(text, sizeof(text)-1, "Name: '%s'", personNames[nearest-1].c_str());
			cvPutText(shownImg, text, cvPoint(faceRect.x, faceRect.y + faceRect.height + 15), &font, textColor);
		    sprintf_s(text, sizeof(text)-1, "Confidence: %f", confidence);
		    cvPutText(shownImg, text, cvPoint(faceRect.x, faceRect.y + faceRect.height + 30), &font, textColor);
		  }
		 sprintf_s( MessageImageResponse.Name, sizeof(MessageImageResponse.Name)-1, "%s", personNames[nearest-1].c_str());
		 MessageImageResponse.confidence=confidence;
	    }
	}
  if (ShowImageDisplay)
    {
     cvShowImage("Server", shownImg);
     cvReleaseImage( &shownImg );
	 cvWaitKey(1);
    }
  int * tmp=(int *)&MessageImageResponse.confidence;
  *tmp=htonl(*tmp);
  MessageImageResponse.DetectTimeInMs=htonl(MessageImageResponse.DetectTimeInMs);
  MessageImageResponse.DrawRect=htonl(MessageImageResponse.DrawRect);
  MessageImageResponse.FaceRect.height=htonl(MessageImageResponse.FaceRect.height);
  MessageImageResponse.FaceRect.width=htonl(MessageImageResponse.FaceRect.width);
  MessageImageResponse.FaceRect.x=htonl(MessageImageResponse.FaceRect.x);
  MessageImageResponse.FaceRect.y=htonl(MessageImageResponse.FaceRect.y);
  MessageImageResponse.HavePerson=htonl(MessageImageResponse.HavePerson);
  MessageImageResponse.ObjectsFound=htonl(MessageImageResponse.ObjectsFound);
  int MessageLen=sizeof(MessageImageResponse) - sizeof(MessageImageResponse.Name)+strlen(MessageImageResponse.Name)+1;
  if (SendData(Socket, MESSAGE_TYPE_IMAGE_REPONSE,(char *)&MessageImageResponse,MessageLen)<0) return(-1);
  return(0);
}
//---------------------------------------------------------------------------
static void CleanupExit(void)
{
 if (FileHandle!= INVALID_HANDLE_VALUE)
 {
  CloseHandle(FileHandle);
  FileHandle=INVALID_HANDLE_VALUE;
 }
if (ListenSocket!=INVALID_SOCKET)
   {
 	closesocket(ListenSocket);
    ListenSocket=INVALID_SOCKET;
   }
if (AcceptSocket!=INVALID_SOCKET)
   {
 	closesocket(AcceptSocket);
    AcceptSocket=INVALID_SOCKET;
   }
if (TCP_Events_Listen!=WSA_INVALID_EVENT)
   {
	WSACloseEvent(TCP_Events_Listen);
	TCP_Events_Listen=WSA_INVALID_EVENT;
   }

 if (TerminateEventHandle!=NULL)
   {
	CloseHandle(TerminateEventHandle);
	TerminateEventHandle=NULL;
   }
 if (CommReader)
   {
	DeleteCommReader(CommReader);
	CommReader=NULL;
   }
   WSACleanup();
}
//---------------------------------------------------------------------------
// LoadTrainingData(CvMat ** pTrainPersonNumMat)
// Open the training data from the file 'facedata.xml'.
//---------------------------------------------------------------------------
static int LoadTrainingData(CvMat ** pTrainPersonNumMat)
{
	CvFileStorage * fileStorage;
	int i;

	// create a file-storage interface
	fileStorage = cvOpenFileStorage( FaceDataFilenameWithPath, 0, CV_STORAGE_READ );
	if( !fileStorage ) {
		printf("Can't open training database file '%s'.\n",FaceDataFilenameWithPath);
		return 0;
	}

	// Load the person names. Added by Shervin.
	personNames.clear();	// Make sure it starts as empty.
	nPersons = cvReadIntByName( fileStorage, 0, "nPersons", 0 );
	if (nPersons == 0) {
		printf("No people found in the training database '%s'.\n",FaceDataFilenameWithPath);
		return 0;
	}
	// Load each person's name.
	for (i=0; i<nPersons; i++) {
		string sPersonName;
		char varname[200];
		sprintf( varname, "personName_%d", (i+1) );
		sPersonName = cvReadStringByName(fileStorage, 0, varname );
		personNames.push_back( sPersonName );
	}

	// Load the data
	nEigens = cvReadIntByName(fileStorage, 0, "nEigens", 0);
	nTrainFaces = cvReadIntByName(fileStorage, 0, "nTrainFaces", 0);
	*pTrainPersonNumMat = (CvMat *)cvReadByName(fileStorage, 0, "trainPersonNumMat", 0);
	eigenValMat  = (CvMat *)cvReadByName(fileStorage, 0, "eigenValMat", 0);
	projectedTrainFaceMat = (CvMat *)cvReadByName(fileStorage, 0, "projectedTrainFaceMat", 0);
	pAvgTrainImg = (IplImage *)cvReadByName(fileStorage, 0, "avgTrainImg", 0);
	eigenVectArr = (IplImage **)cvAlloc(nTrainFaces*sizeof(IplImage *));
	for(i=0; i<nEigens; i++)
	{
		char varname[200];
		sprintf( varname, "eigenVect_%d", i );
		eigenVectArr[i] = (IplImage *)cvReadByName(fileStorage, 0, varname, 0);
	}

	// release the file-storage interface
	cvReleaseFileStorage( &fileStorage );

	printf("Training data loaded (%d training images of %d people):\n", nTrainFaces, nPersons);
	printf("People: ");
	if (nPersons > 0)
		printf("<%s>", personNames[0].c_str());
	for (i=1; i<nPersons; i++) {
		printf(", <%s>", personNames[i].c_str());
	}
	printf(".\n");

	return 1;
}
//---------------------------------------------------------------------------
// void StoreTrainingData(void)
// Save the training data to the file 'facedata.xml'.
//---------------------------------------------------------------------------
static void StoreTrainingData(void)
{
	CvFileStorage * fileStorage;
	int i;
	// create a file-storage interface
	fileStorage = cvOpenFileStorage( FaceDataFilenameWithPath, 0, CV_STORAGE_WRITE );

	// Store the person names. Added by Shervin.
	cvWriteInt( fileStorage, "nPersons", nPersons );
	for (i=0; i<nPersons; i++) {
		char varname[200];
		sprintf( varname, "personName_%d", (i+1) );
		cvWriteString(fileStorage, varname, personNames[i].c_str(), 0);
	}

	// store all the data
	cvWriteInt( fileStorage, "nEigens", nEigens );
	cvWriteInt( fileStorage, "nTrainFaces", nTrainFaces );
	cvWrite(fileStorage, "trainPersonNumMat", personNumTruthMat, cvAttrList(0,0));
	cvWrite(fileStorage, "eigenValMat", eigenValMat, cvAttrList(0,0));
	cvWrite(fileStorage, "projectedTrainFaceMat", projectedTrainFaceMat, cvAttrList(0,0));
	cvWrite(fileStorage, "avgTrainImg", pAvgTrainImg, cvAttrList(0,0));
	for(i=0; i<nEigens; i++)
	{
		char varname[200];
		sprintf( varname, "eigenVect_%d", i );
		cvWrite(fileStorage, varname, eigenVectArr[i], cvAttrList(0,0));
	}

	// release the file-storage interface
	cvReleaseFileStorage( &fileStorage );
}
//---------------------------------------------------------------------------
// IplImage* ConvertImageToGreyscale(const IplImage *imageSrc)
// Return a new image that is always greyscale, whether the input image was RGB or Greyscale.
// Remember to free the returned image using cvReleaseImage() when finished.
//---------------------------------------------------------------------------
static IplImage* ConvertImageToGreyscale(const IplImage *imageSrc)
{
	IplImage *imageGrey;
	// Either convert the image to greyscale, or make a copy of the existing greyscale image.
	// This is to make sure that the user can always call cvReleaseImage() on the output, whether it was greyscale or not.
	if (imageSrc->nChannels == 3) {
		imageGrey = cvCreateImage( cvGetSize(imageSrc), IPL_DEPTH_8U, 1 );
		cvCvtColor( imageSrc, imageGrey, CV_BGR2GRAY );
	}
	else {
		imageGrey = cvCloneImage(imageSrc);
	}
	return imageGrey;
}
//---------------------------------------------------------------------------
// CvRect DetectFaceInImage(const IplImage *inputImg, const CvHaarClassifierCascade* cascade, int *Time, int *NumObjects )
// Perform face detection on the input image, using the given Haar cascade classifier.
// Returns a rectangle for the detected region in the given image.
//---------------------------------------------------------------------------
static CvRect DetectFaceInImage(const IplImage *inputImg, const CvHaarClassifierCascade* cascade, int *Time, int *NumObjects )
{
	const CvSize minFeatureSize = cvSize(20, 20);
	const int flags = CV_HAAR_FIND_BIGGEST_OBJECT | CV_HAAR_DO_ROUGH_SEARCH;	// Only search for 1 face.
	const float search_scale_factor = 1.1f;
	IplImage *detectImg;
	IplImage *greyImg = 0;
	CvMemStorage* storage;
	CvRect rc;
	double t;
	CvSeq* rects;

	storage = cvCreateMemStorage(0);
	cvClearMemStorage( storage );

	// If the image is color, use a greyscale copy of the image.
	detectImg = (IplImage*)inputImg;	// Assume the input image is to be used.
	if (inputImg->nChannels > 1) 
	{
		greyImg = cvCreateImage(cvSize(inputImg->width, inputImg->height), IPL_DEPTH_8U, 1 );
		cvCvtColor( inputImg, greyImg, CV_BGR2GRAY );
		detectImg = greyImg;	// Use the greyscale version as the input.
	}

	// Detect all the faces.
	t = (double)cvGetTickCount();
	rects = cvHaarDetectObjects( detectImg, (CvHaarClassifierCascade*)cascade, storage,
				search_scale_factor, 3, flags, minFeatureSize );
	t = (double)cvGetTickCount() - t;
	*Time= cvRound( t/((double)cvGetTickFrequency()*1000.0) );
	*NumObjects=rects->total;
	printf("[Face Detection took %d ms and found %d objects]\n",*Time,*NumObjects );

	// Get the first detected face (the biggest).
	if (rects->total > 0) {
        rc = *(CvRect*)cvGetSeqElem( rects, 0 );
    }
	else
		rc = cvRect(-1,-1,-1,-1);	// Couldn't find the face.

	//cvReleaseHaarClassifierCascade( &cascade );
	//cvReleaseImage( &detectImg );
	if (greyImg)
		cvReleaseImage( &greyImg );
	cvReleaseMemStorage( &storage );

	return rc;	// Return the biggest face found, or (-1,-1,-1,-1).
}
//---------------------------------------------------------------------------
// IplImage* CropImage(const IplImage *img, const CvRect region)
// Returns a new image that is a cropped version of the original image. 
//---------------------------------------------------------------------------
static IplImage* CropImage(const IplImage *img, const CvRect region)
{
	IplImage *imageTmp;
	IplImage *imageRGB;
	CvSize size;
	size.height = img->height;
	size.width = img->width;

	if (img->depth != IPL_DEPTH_8U) {
		printf("ERROR in cropImage: Unknown image depth of %d given in cropImage() instead of 8 bits per pixel.\n", img->depth);
		exit(1);
	}

	// First create a new (color or greyscale) IPL Image and copy contents of img into it.
	imageTmp = cvCreateImage(size, IPL_DEPTH_8U, img->nChannels);
	cvCopy(img, imageTmp, NULL);

	// Create a new image of the detected region
	// Set region of interest to that surrounding the face
	cvSetImageROI(imageTmp, region);
	// Copy region of interest (i.e. face) into a new iplImage (imageRGB) and return it
	size.width = region.width;
	size.height = region.height;
	imageRGB = cvCreateImage(size, IPL_DEPTH_8U, img->nChannels);
	cvCopy(imageTmp, imageRGB, NULL);	// Copy just the region.

    cvReleaseImage( &imageTmp );
	return imageRGB;		
}
//---------------------------------------------------------------------------
// IplImage* ResizeImage(const IplImage *origImg, int newWidth, int newHeight)
// Creates a new image copy that is of a desired size.
// Remember to free the new image later.
static IplImage* ResizeImage(const IplImage *origImg, int newWidth, int newHeight)
{
	IplImage *outImg = 0;
	int origWidth;
	int origHeight;
	if (origImg) {
		origWidth = origImg->width;
		origHeight = origImg->height;
	}
	if (newWidth <= 0 || newHeight <= 0 || origImg == 0 || origWidth <= 0 || origHeight <= 0) {
		printf("ERROR in resizeImage: Bad desired image size of %dx%d\n.", newWidth, newHeight);
		exit(1);
	}

	// Scale the image to the new dimensions, even if the aspect ratio will be changed.
	outImg = cvCreateImage(cvSize(newWidth, newHeight), origImg->depth, origImg->nChannels);
	if (newWidth > origImg->width && newHeight > origImg->height) {
		// Make the image larger
		cvResetImageROI((IplImage*)origImg);
		cvResize(origImg, outImg, CV_INTER_LINEAR);	// CV_INTER_CUBIC or CV_INTER_LINEAR is good for enlarging
	}
	else {
		// Make the image smaller
		cvResetImageROI((IplImage*)origImg);
		cvResize(origImg, outImg, CV_INTER_AREA);	// CV_INTER_AREA is good for shrinking / decimation, but bad at enlarging.
	}

	return outImg;
}
//---------------------------------------------------------------------------
// int FindNearestNeighbor(float * projectedTestFace, float *pConfidence)
// Find the most likely person based on a detection. Returns the index, and stores the confidence value into pConfidence.
//---------------------------------------------------------------------------
static int FindNearestNeighbor(float * projectedTestFace, float *pConfidence)
{
	//double leastDistSq = 1e12;
	double leastDistSq = DBL_MAX;
	int i, iTrain, iNearest = 0;

	for(iTrain=0; iTrain<nTrainFaces; iTrain++)
	{
		double distSq=0;

		for(i=0; i<nEigens; i++)
		{
			float d_i = projectedTestFace[i] - projectedTrainFaceMat->data.fl[iTrain*nEigens + i];
            if (USE_MAHALANOBIS_DISTANCE)
			   distSq += d_i*d_i / eigenValMat->data.fl[i];  // Mahalanobis distance (might give better results than Eucalidean distance)
			else 
			   distSq += d_i*d_i; // Euclidean distance.
		}

		if(distSq < leastDistSq)
		{
			leastDistSq = distSq;
			iNearest = iTrain;
		}
	}

	// Return the confidence level based on the Euclidean distance,
	// so that similar images should give a confidence between 0.5 to 1.0,
	// and very different images should give a confidence between 0.0 to 0.5.
	*pConfidence =(float) (1.0f - sqrt( leastDistSq / (float)(nTrainFaces * nEigens) ) / 255.0f);

	// Return the found index.
	return iNearest;
}
//---------------------------------------------------------------------------
static int StartTrainingMode(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData)
{
  TMessageTrainStartResponse MessageTrainStartResponse ;
  TMessageTrainStartRequest *MessageTrainStartRequest;
  int MessageLen;

  MessageTrainStartRequest=(TMessageTrainStartRequest *)MessageData;
  strcpy(newPersonName, MessageTrainStartRequest->Name);
  strcpy(MessageTrainStartResponse.Name, MessageTrainStartRequest->Name); 
  printf("Collecting all images until you hit 't', to start Training the images as '%s' ...\n", newPersonName);
  newPersonFaces = 0;	// restart training a new person
  saveNextFaces = TRUE;
  MessageLen=sizeof(MessageTrainStartResponse) - sizeof(MessageTrainStartResponse.Name)+strlen(MessageTrainStartResponse.Name)+1;
  if (SendData(Socket, MESSAGE_START_TRAIN_MODE_RESPONSE,(char *)&MessageTrainStartResponse,MessageLen)<0) return(-1);
  FaceMode=TRAIN_MODE;
  return(0);
}
//---------------------------------------------------------------------------
static int EndTrainingMode(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData)
{
 FILE *trainFile;
 char cstr[256];

 saveNextFaces = FALSE;	// stop saving next faces.
 // Store the saved data into the training file.
 printf("Storing the training data for new person '%s'.\n", newPersonName);
 // Append the new person to the end of the training data.
 if (newPersonFaces>0)
   {
    trainFile = fopen(TrainFilenameWithPath, "a");
    for (int i=0; i<newPersonFaces; i++) 
     {
	  sprintf(cstr, "data\\%d_%s%d.pgm", nPersons+1, newPersonName, i+1);
	  fprintf(trainFile, "%d %s %s\n", nPersons+1, newPersonName, cstr);
     }
    fclose(trainFile);
   }

 // Re-initialize the local data.
 projectedTestFace = 0;
 saveNextFaces = FALSE;
 newPersonFaces = 0;

 // Retrain from the new database without shutting down.
 // Depending on the number of images in the training set and number of people, it might take 30 seconds or so.
 cvFree( &trainPersonNumMat );	// Free the previous data before getting new data
 trainPersonNumMat = RetrainOnline();
 // Project the test images onto the PCA subspace
 cvFree(&projectedTestFace);	// Free the previous data before getting new data
 projectedTestFace = (float *)cvAlloc( nEigens*sizeof(float) );

 printf("Recognizing person in the camera ...\n");
 if (SendData(Socket, MESSAGE_END_TRAIN_MODE_RESPONSE,NULL,0)<0) return(-1);
 FaceMode=RECOGNISE;
 return(0);
}
//---------------------------------------------------------------------------
// CvMat* RetrainOnline(void)
// Re-train the new face rec database without shutting down.
// Depending on the number of images in the training set and number of people, it might take 30 seconds or so.
//---------------------------------------------------------------------------
static CvMat* RetrainOnline(void)
{
	CvMat *trainPersonNumMat;
	int i;

	// Free & Re-initialize the global variables.
	if (faceImgArr) {
		for (i=0; i<nTrainFaces; i++) {
			if (faceImgArr[i])
				cvReleaseImage( &faceImgArr[i] );
		}
	}
	cvFree( &faceImgArr ); // array of face images
	cvFree( &personNumTruthMat ); // array of person numbers
	personNames.clear();			// array of person names (indexed by the person number). Added by Shervin.
	nPersons = 0; // the number of people in the training set. Added by Shervin.
	nTrainFaces = 0; // the number of training images
	nEigens = 0; // the number of eigenvalues
	cvReleaseImage( &pAvgTrainImg ); // the average image
	for (i=0; i<nTrainFaces; i++) {
		if (eigenVectArr[i])
			cvReleaseImage( &eigenVectArr[i] );
	}
	cvFree( &eigenVectArr ); // eigenvectors
	cvFree( &eigenValMat ); // eigenvalues
	cvFree( &projectedTrainFaceMat ); // projected training faces

	// Retrain from the data in the files
	printf("Retraining with the new person ...\n");
	if (Learn(TrainFilenameWithPath,true)==0)
	  {
	    printf("Done retraining.\n");

	   // Load the previously saved training data
	   if( !LoadTrainingData( &trainPersonNumMat ) ) {
		printf("ERROR in recognizeFromCam(): Couldn't load the training data!\n");
		exit(1);
	     }
	  }

	return trainPersonNumMat;
}
//---------------------------------------------------------------------------
// int Learn(char *szFileTrain)
// Train from the data in the given text file, and store the trained data into the file 'facedata.xml'.
//---------------------------------------------------------------------------
static int Learn(char *szFileTrain,bool UsePath)
{
	int i, offset;

	// load training data
	printf("Loading the training images in '%s'\n", szFileTrain);
	nTrainFaces = LoadFaceImgArray(szFileTrain,UsePath);
	printf("Got %d training images.\n", nTrainFaces);
	if( nTrainFaces < 2 )
	{
		fprintf(stderr,
		        "Need 2 or more training faces\n"
		        "Input file contains only %d\n", nTrainFaces);
		return(-1);
	}
	// do PCA on the training faces
	DoPCA();
	// project the training images onto the PCA subspace
	projectedTrainFaceMat = cvCreateMat( nTrainFaces, nEigens, CV_32FC1 );
	offset = projectedTrainFaceMat->step / sizeof(float);
	for(i=0; i<nTrainFaces; i++)
	{
		//int offset = i * nEigens;
		cvEigenDecomposite(
			faceImgArr[i],
			nEigens,
			eigenVectArr,
			0, 0,
			pAvgTrainImg,
			//projectedTrainFaceMat->data.fl + i*nEigens);
			projectedTrainFaceMat->data.fl + i*offset);
	}

	// store the recognition data as an xml file
	StoreTrainingData();

	// Save all the eigenvectors as images, so that they can be checked.
	if (SAVE_EIGENFACE_IMAGES) {
		StoreEigenfaceImages();
	}
  return(0);
}
//---------------------------------------------------------------------------
// int LoadFaceImgArray(char * filename)
// Read the names & image filenames of people from a text file, and load all those images listed.
//---------------------------------------------------------------------------
static int LoadFaceImgArray(char * filename,bool UsePath)
{
	FILE * imgListFile = 0;
	char imgFilename[512];
	static char imgFilenamewithPath[1024];
	int iFace, nFaces=0;
	int i;

	// open the input file
	if( !(imgListFile = fopen(filename, "r")) )
	{
		fprintf(stderr, "Can\'t open file %s\n", filename);
		return 0;
	}

	// count the number of faces
	while( fgets(imgFilename, 512, imgListFile) ) ++nFaces;
	rewind(imgListFile);

	// allocate the face-image array and person number matrix
	faceImgArr        = (IplImage **)cvAlloc( nFaces*sizeof(IplImage *) );
	personNumTruthMat = cvCreateMat( 1, nFaces, CV_32SC1 );

	personNames.clear();	// Make sure it starts as empty.
	nPersons = 0;

	// store the face images in an array
	for(iFace=0; iFace<nFaces; iFace++)
	{
		char personName[256];
		string sPersonName;
		int personNumber;

		// read person number (beginning with 1), their name and the image filename.
		fscanf(imgListFile, "%d %s %s", &personNumber, personName, imgFilename);
		sPersonName = personName;
		//printf("Got %d: %d, <%s>, <%s>.\n", iFace, personNumber, personName, imgFilename);

		// Check if a new person is being loaded.
		if (personNumber > nPersons) {
			// Allocate memory for the extra person (or possibly multiple), using this new person's name.
			for (i=nPersons; i < personNumber; i++) {
				personNames.push_back( sPersonName );
			}
			nPersons = personNumber;
			//printf("Got new person <%s> -> nPersons = %d [%d]\n", sPersonName.c_str(), nPersons, personNames.size());
		}

		// Keep the data
		personNumTruthMat->data.i[iFace] = personNumber;

		// load the face image
		if (UsePath)
		{
		 sprintf(imgFilenamewithPath,"%s\\%s",DataPath,imgFilename);
		 faceImgArr[iFace] = cvLoadImage(imgFilenamewithPath, CV_LOAD_IMAGE_GRAYSCALE);
		}
		else  faceImgArr[iFace] = cvLoadImage(imgFilename, CV_LOAD_IMAGE_GRAYSCALE);

		if( !faceImgArr[iFace] )
		{
			fprintf(stderr, "Can\'t load image from %s\n", imgFilename);
			return 0;
		}
	}

	fclose(imgListFile);

	printf("Data loaded from '%s': (%d images of %d people).\n", filename, nFaces, nPersons);
	printf("People: ");
	if (nPersons > 0)
		printf("<%s>", personNames[0].c_str());
	for (i=1; i<nPersons; i++) {
		printf(", <%s>", personNames[i].c_str());
	}
	printf(".\n");

	return nFaces;
}

//---------------------------------------------------------------------------
// void DoPCA(void)
// Do the Principal Component Analysis, finding the average image
// and the eigenfaces that represent any image in the given dataset.
//---------------------------------------------------------------------------
static void DoPCA(void)
{
	int i;
	CvTermCriteria calcLimit;
	CvSize faceImgSize;

	// set the number of eigenvalues to use
	nEigens = nTrainFaces-1;

	// allocate the eigenvector images
	faceImgSize.width  = faceImgArr[0]->width;
	faceImgSize.height = faceImgArr[0]->height;
	eigenVectArr = (IplImage**)cvAlloc(sizeof(IplImage*) * nEigens);
	for(i=0; i<nEigens; i++)
		eigenVectArr[i] = cvCreateImage(faceImgSize, IPL_DEPTH_32F, 1);

	// allocate the eigenvalue array
	eigenValMat = cvCreateMat( 1, nEigens, CV_32FC1 );

	// allocate the averaged image
	pAvgTrainImg = cvCreateImage(faceImgSize, IPL_DEPTH_32F, 1);

	// set the PCA termination criterion
	calcLimit = cvTermCriteria( CV_TERMCRIT_ITER, nEigens, 1);

	// compute average image, eigenvalues, and eigenvectors
	cvCalcEigenObjects(
		nTrainFaces,
		(void*)faceImgArr,
		(void*)eigenVectArr,
		CV_EIGOBJ_NO_CALLBACK,
		0,
		0,
		&calcLimit,
		pAvgTrainImg,
		eigenValMat->data.fl);

	cvNormalize(eigenValMat, eigenValMat, 1, 0, CV_L1, 0);
}

//---------------------------------------------------------------------------
// void StoreEigenfaceImages(void)
// Save all the eigenvectors as images, so that they can be checked.
//---------------------------------------------------------------------------
static void StoreEigenfaceImages(void)
{
	// Store the average image to a file
	printf("Saving the image of the average face as '%s'.\n",OutAverageImageFilenameWithPath);
	cvSaveImage(OutAverageImageFilenameWithPath, pAvgTrainImg);
	// Create a large image made of many eigenface images.
	// Must also convert each eigenface image to a normal 8-bit UCHAR image instead of a 32-bit float image.
	printf("Saving the %d eigenvector images as '%s'\n", nEigens,OutEigenfacesFilenameWithPath);
	if (nEigens > 0) {
		// Put all the eigenfaces next to each other.
		int COLUMNS = 8;	// Put upto 8 images on a row.
		int nCols = min(nEigens, COLUMNS);
		int nRows = 1 + (nEigens / COLUMNS);	// Put the rest on new rows.
		int w = eigenVectArr[0]->width;
		int h = eigenVectArr[0]->height;
		CvSize size;
		size = cvSize(nCols * w, nRows * h);
		IplImage *bigImg = cvCreateImage(size, IPL_DEPTH_8U, 1);	// 8-bit Greyscale UCHAR image
		for (int i=0; i<nEigens; i++) {
			// Get the eigenface image.
			IplImage *byteImg = ConvertFloatImageToUcharImage(eigenVectArr[i]);
			// Paste it into the correct position.
			int x = w * (i % COLUMNS);
			int y = h * (i / COLUMNS);
			CvRect ROI = cvRect(x, y, w, h);
			cvSetImageROI(bigImg, ROI);
			cvCopyImage(byteImg, bigImg);
			cvResetImageROI(bigImg);
			cvReleaseImage(&byteImg);
		}
		cvSaveImage(OutEigenfacesFilenameWithPath, bigImg);
		cvReleaseImage(&bigImg);
	}
}
//---------------------------------------------------------------------------
// IplImage* ConvertFloatImageToUcharImage(const IplImage *srcImg)
// Get an 8-bit equivalent of the 32-bit Float image.
// Returns a new image, so remember to call 'cvReleaseImage()' on the result.
//---------------------------------------------------------------------------
static IplImage* ConvertFloatImageToUcharImage(const IplImage *srcImg)
{
	IplImage *dstImg = 0;
	if ((srcImg) && (srcImg->width > 0 && srcImg->height > 0)) {

		// Spread the 32bit floating point pixels to fit within 8bit pixel range.
		double minVal, maxVal;
		cvMinMaxLoc(srcImg, &minVal, &maxVal);

		//cout << "FloatImage:(minV=" << minVal << ", maxV=" << maxVal << ")." << endl;

		// Deal with NaN and extreme values, since the DFT seems to give some NaN results.
		if (cvIsNaN(minVal) || minVal < -1e30)
			minVal = -1e30;
		if (cvIsNaN(maxVal) || maxVal > 1e30)
			maxVal = 1e30;
		if (maxVal-minVal == 0.0f)
			maxVal = minVal + 0.001;	// remove potential divide by zero errors.

		// Convert the format
		dstImg = cvCreateImage(cvSize(srcImg->width, srcImg->height), 8, 1);
		cvConvertScale(srcImg, dstImg, 255.0 / (maxVal - minVal), - minVal * 255.0 / (maxVal-minVal));
	}
	return dstImg;
}
//---------------------------------------------------------------------------
// Show how to use this program from the command-line.
static void PrintUsage(_TCHAR* Arg0)
{
	printf("\n\n%ls\n"
		"Usage: %ls [<command>] \n"
		"  Valid commands are: \n"
		"    train <train_file> \n"
		"    test <test_file> \n"
		" (if no args are supplied, then online server mode is enabled).\n",Arg0,Arg0);
}
//---------------------------------------------------------------------------
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
// Recognize the face in each of the test images given, and compare the results with the truth.
static void RecognizeFileList(char *szFileTest,bool UsePath)
{
	int i, nTestFaces  = 0;         // the number of test images
	CvMat * trainPersonNumMat = 0;  // the person numbers during training
	float * projectedTestFace = 0;
	char *answer;
	int nCorrect = 0;
	int nWrong = 0;
	double timeFaceRecognizeStart;
	double tallyFaceRecognizeTime;
	float confidence;

	// load test images and ground truth for person number
	nTestFaces = LoadFaceImgArray(szFileTest,UsePath);
	printf("%d test faces loaded\n", nTestFaces);

	// load the saved training data
	if( !LoadTrainingData( &trainPersonNumMat ) ) return;

	// project the test images onto the PCA subspace
	projectedTestFace = (float *)cvAlloc( nEigens*sizeof(float) );
	timeFaceRecognizeStart = (double)cvGetTickCount();	// Record the timing.
	for(i=0; i<nTestFaces; i++)
	{
		int iNearest, nearest, truth;

		// project the test image onto the PCA subspace
		cvEigenDecomposite(
			faceImgArr[i],
			nEigens,
			eigenVectArr,
			0, 0,
			pAvgTrainImg,
			projectedTestFace);

		iNearest = FindNearestNeighbor(projectedTestFace, &confidence);
		truth    = personNumTruthMat->data.i[i];
		nearest  = trainPersonNumMat->data.i[iNearest];

		if (nearest == truth) {
			answer = "Correct";
			nCorrect++;
		}
		else {
			answer = "WRONG!";
			nWrong++;
		}
		printf("nearest = %d, Truth = %d (%s). Confidence = %f\n", nearest, truth, answer, confidence);
	}
	tallyFaceRecognizeTime = (double)cvGetTickCount() - timeFaceRecognizeStart;
	if (nCorrect+nWrong > 0) {
		printf("TOTAL ACCURACY: %d%% out of %d tests.\n", nCorrect * 100/(nCorrect+nWrong), (nCorrect+nWrong));
		printf("TOTAL TIME: %.1fms average.\n", tallyFaceRecognizeTime/((double)cvGetTickFrequency() * 1000.0 * (nCorrect+nWrong) ) );
	}

}
//---------------------------------------------------------------------------
