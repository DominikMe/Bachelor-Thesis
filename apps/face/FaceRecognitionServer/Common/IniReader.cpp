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
#include <iostream>
#include <Windows.h>
#include <stdio.h>
#include "IniReader.h"

CIniReader::CIniReader(LPCWSTR szFileName)
{
wcscpy_s(m_szFileName, szFileName);
}
int CIniReader::ReadInteger(LPCWSTR szSection, LPCWSTR szKey, int iDefaultValue)
{
 int iResult = GetPrivateProfileInt(szSection,  szKey, iDefaultValue, m_szFileName); 
 return iResult;
}
float CIniReader::ReadFloat(LPCWSTR szSection, LPCWSTR szKey, float fltDefaultValue)
{
 WCHAR szResult[255];
 WCHAR szDefault[255];
 float fltResult;
 wsprintf(szDefault, L"%f",fltDefaultValue);
 GetPrivateProfileString(szSection,  szKey, szDefault, szResult, sizeof(TCHAR[255]), m_szFileName); 
 fltResult =  (float)_wtof(szResult);
 return fltResult;
}
bool CIniReader::ReadBoolean(LPCWSTR szSection, LPCWSTR szKey, bool bolDefaultValue)
{
 WCHAR szResult[255];
 WCHAR szDefault[255];
 bool bolResult;
 wsprintf(szDefault, L"%s", bolDefaultValue? L"True" : L"False");
 GetPrivateProfileString(szSection, szKey, szDefault, szResult, sizeof(szResult), m_szFileName); 
 bolResult =  (wcscmp(szResult, L"True") == 0 || wcscmp(szResult, L"true") == 0) ? true : false;
 return bolResult;
}
LPCWSTR CIniReader::ReadString(LPCWSTR szSection, LPCWSTR szKey, const LPCWSTR szDefaultValue)
{
 WCHAR* szResult = new TCHAR[255];
 memset(szResult, 0x00, sizeof( TCHAR[255]));
 GetPrivateProfileString(szSection,  szKey, szDefaultValue, szResult, sizeof(TCHAR[255]), m_szFileName); 
 return szResult;
}
