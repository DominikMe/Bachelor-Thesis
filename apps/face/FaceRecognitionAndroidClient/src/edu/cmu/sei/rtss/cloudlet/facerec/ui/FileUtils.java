/**
---------------------
Copyright 2012 Carnegie Mellon University

This material is based upon work funded and supported by the Department of Defense under Contract No. 
FA8721-05-C-0003 with Carnegie Mellon University for the operation of the Software Engineering Institute, 
a federally funded research and development center.

Any opinions, findings and conclusions or recommendations expressed in this material are those of the 
author(s) and do not necessarily reflect the views of the United States Department of Defense.

NO WARRANTY
THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN “AS-IS” 
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY 
MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, 
OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF 
ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

This material contains SEI Proprietary Information and may not be disclosed outside of the SEI without 
the written consent of the Director’s Office and completion of the Disclosure of Information process.
------------
**/

package edu.cmu.sei.rtss.cloudlet.facerec.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

/**
 * Utility class for dealing with files. 
 * @author ssimanta
 *
 */
public class FileUtils {
	
	public static final String LOG_TAG = FileUtils.class.getName();
	
	public static String parseDataFileToString(final String fileName) {
		try {
			final File file = new File(fileName);
			InputStream stream = new FileInputStream(file);

			int size = stream.available();
			byte[] bytes = new byte[size];
			stream.read(bytes);
			stream.close();

			return new String(bytes);

		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException in reading data file  " + fileName + " \n" + e.getMessage());
		}
		return null;
	}
	
	public static boolean writeStringtoDataFile(final String contents,
			final String fileName) {
		FileWriter fileWriter = null;
		boolean success = false; 
		try {
			fileWriter = new FileWriter(fileName);

			if (fileWriter != null) {
				fileWriter.write(contents);
				fileWriter.flush();
				fileWriter.close();
				success = true; 
			}

			
		} catch (FileNotFoundException e1) {
			Log.e(LOG_TAG, "File not found " + fileName + " \n" + e1.getMessage());
			e1.printStackTrace();


		} catch (IOException ioe) {
			Log.e(LOG_TAG, "IOException while writing to file -> " + fileName + " \n" + ioe.getMessage());
			ioe.printStackTrace();
		}
		
		return success; 
	}

}
