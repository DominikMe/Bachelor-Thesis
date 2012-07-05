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


#ifndef INIREADER_H
#define INIREADER_H
#include <wchar.h>
class CIniReader
{
public:
 CIniReader(LPCWSTR szFileName); 
 int ReadInteger(LPCWSTR szSection, LPCWSTR szKey, int iDefaultValue);
 float ReadFloat(LPCWSTR szSection, LPCWSTR szKey, float fltDefaultValue);
 bool ReadBoolean(LPCWSTR szSection, LPCWSTR szKey, bool bolDefaultValue);
 LPCWSTR ReadString(LPCWSTR szSection, LPCWSTR szKey, const LPCWSTR szDefaultValue);
private:
  TCHAR  m_szFileName[255];
};
#endif//INIREADER_H
