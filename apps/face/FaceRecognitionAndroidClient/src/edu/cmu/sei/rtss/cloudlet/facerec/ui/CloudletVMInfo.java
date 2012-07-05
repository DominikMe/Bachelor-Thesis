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
import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * 
 * This is a in-memory class used to store the cloudlet information and will be
 * used by the different applications that are launched from the
 * CloudletClientApp.
 * 
 * @author ssimanta
 * 
 */
public class CloudletVMInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 198893833829181L;

	public static final String LOG_TAG = CloudletVMInfo.class.getName();

	public static final String IP_ADDRESS_KEY = "IP_ADDRESS";
	public static final String PORT_KEY = "PORT";

	protected String ipAddress;
	protected int port;
	
	
	public CloudletVMInfo()
	{
		
	}
	
	public CloudletVMInfo(String ip, int port)
	{
		this.ipAddress = ip;
		this.port = port; 
	}
	
	public CloudletVMInfo(final JSONObject jsonObj)
	{
		if( jsonObj != null)
		{
			try {
				this.ipAddress = jsonObj.getString(IP_ADDRESS_KEY);
				this.port = jsonObj.getInt(PORT_KEY);

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean writeToFile(String fileName) {

		// create a new JSON object and put the IP and port there
		JSONObject rootJSONObj = new JSONObject();
		try {
			rootJSONObj.put(IP_ADDRESS_KEY, ipAddress);
			rootJSONObj.put(PORT_KEY, port);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return FileUtils
				.writeStringtoDataFile(rootJSONObj.toString(), fileName);

	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("CloudletVMInfo ->").append("[").append("IP: ")
				.append(ipAddress != null ? ipAddress : "null")
				.append(" , PORT: ").append(port).append("]");
		return buf.toString();
	}

	public boolean loadFromFile(String fileName) {

		if (fileName == null) {
			Log.e(LOG_TAG, "Input file name to loadFromFile is " + fileName);
			return false;
		}

		File file = new File(fileName);

		if (!file.exists()) {
			Log.e(LOG_TAG, "Input file [" + fileName
					+ "] to CloudletVMInfo#loadFromFile() doesn't exists.");
			return false;
		}

		String fileContents = FileUtils.parseDataFileToString(fileName);

		if (fileContents == null || fileContents.trim().length() == 0) {
			Log.e(LOG_TAG, "Contents of input file [" + fileName
					+ "] to CloudletVMInfo#loadFromFile() are empty.");
			return false;
		}

		// read the contents the file
		try {
			JSONObject rootJSONObject = new JSONObject(fileContents);
			String ip = rootJSONObject.getString(IP_ADDRESS_KEY);
			if (ip != null && ip.trim().length() > 0) {
				this.ipAddress = ip;
			} else {
				Log.e(LOG_TAG, "IP address in input file [" + fileName
						+ "] to CloudletVMInfo#loadFromFile() is [" + ip
						+ "] is either empty OR null.");
				return false;
			}

			this.port = rootJSONObject.getInt(PORT_KEY);

		} catch (JSONException e) {
			e.printStackTrace();
		}

		// if you got here all is well.
		return true;

	}

}
