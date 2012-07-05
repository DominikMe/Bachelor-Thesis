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


#ifndef COMMSUPPORT_H
#define COMMSUPPORT_H
#include <Windows.h>

typedef enum
{
 MESSAGE_TYPE_JPEG_IMAGE              =1,
 MESSAGE_TYPE_IMAGE_REPONSE           =2,
 MESSAGE_START_TRAIN_MODE_REQUEST     =3,
 MESSAGE_START_TRAIN_MODE_RESPONSE    =4,
 MESSAGE_END_TRAIN_MODE_REQUEST       =5,
 MESSAGE_END_TRAIN_MODE_RESPONSE      =6,
 MESSAGE_TRAINING_COLLECTION_UPDATE   =7

}TMessageType;

typedef struct
{
 int MessageType;
 int MessageLength;
} TMessageHeader;

typedef struct
{
 int x;
 int y;
 int width;
 int height;
} TRect;

typedef struct
{
 int   DetectTimeInMs;
 int   ObjectsFound;
 int   DrawRect;
 int   HavePerson;
 TRect FaceRect;
 float confidence;
 char  Name[256];
} TMessageImageResponse;


typedef struct
{
 char  Name[256];
} TMessageTrainStartRequest;

typedef struct
{
 char  Name[256];
} TMessageTrainStartResponse;

typedef struct
{
 int   nPersons;
 int   newPersonFaces;
 char  Name[256];
} TMessageTrainingCollectionUpdate;


typedef int (*TProcessRecvdMessage)(SOCKET Socket,TMessageHeader *MessageHeader,BYTE *MessageData);

typedef struct
{
 SOCKET                Socket;
 DWORD                 MessageHeaderBytesNeeded;
 DWORD                 MessageState;
 TMessageHeader        MessageHeader;
 BYTE                 *MessageData;
 DWORD                 SizeOfMessageData;
 DWORD                 MessageDataBytesNeeded;
 TProcessRecvdMessage  ProcessRecvdMessagePtr;
} TCommReader;

TCommReader * CreateCommReader(SOCKET Socket,DWORD MaxDataSize,TProcessRecvdMessage  TProcessRecvdMessagePtr);
int ResetCommReader(TCommReader *CommReader);
int DeleteCommReader(TCommReader *CommReader);
int ReadData(TCommReader *CommReader);
int SendData(SOCKET Socket, int MessageType,char *DataBuffer,int BufferLength);
int SetSocketBufferSizes(SOCKET Socket,int InputSize,int OutputSize);
int SetSocketTCPNoDelay(SOCKET Socket);
#endif