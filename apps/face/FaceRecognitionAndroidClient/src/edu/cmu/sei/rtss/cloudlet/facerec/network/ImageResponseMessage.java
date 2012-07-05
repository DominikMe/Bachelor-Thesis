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

package edu.cmu.sei.rtss.cloudlet.facerec.network;

public class ImageResponseMessage {
	public int detectTimeInMs;
	public int objectsFound;
	public int drawRect;
	public int havePerson;
	public FacerecRect faceRect;
	public float confidence;
	public String name;

	public String toString() {

		StringBuffer buf = new StringBuffer();
		buf.append("detect-time: ").append(detectTimeInMs).append("\n")
				.append(" objectsFound: ").append(objectsFound).append("\n")
				.append(" drawRect: ").append(drawRect).append("\n")
				.append(" havePerson: ").append(havePerson).append("\n")
				.append(" cofidence: ").append(confidence).append("\n")
				.append(" name: ").append(name);

		return buf.toString();
	}
	

}


