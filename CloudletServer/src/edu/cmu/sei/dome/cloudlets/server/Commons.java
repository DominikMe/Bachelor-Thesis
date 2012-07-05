package edu.cmu.sei.dome.cloudlets.server;

import java.util.ArrayList;

public final class Commons {

	private Commons() {
	}

	static final String STORE = "uploads/";;
	static OS MY_OS = OS.linux;
	static boolean DEBUG = true;
	static final String NAME = "CloudletServer";
	static final double VERSION = 1.0;
	public static int PORT = 8080;
	static final String CODENAME = "SINAI";

	static final String getAttributes() {
		return String.format("name=%s,os=%s,version=%f", CODENAME, MY_OS.toString(), VERSION);
	}

	// file types
	static final String FILETYPE_EXE = "exe";
	static final String FILETYPE_CDE = "cde";
	static final String FILETYPE_JAR = "jar";
	@SuppressWarnings("serial")
	static final ArrayList<String> linuxTypes = new ArrayList<String>() {
		{
			add(FILETYPE_CDE);
			add(FILETYPE_JAR);
		}
	};
	@SuppressWarnings("serial")
	static final ArrayList<String> windowsTypes = new ArrayList<String>() {
		{
			add(FILETYPE_EXE);
			add(FILETYPE_JAR);
		}
	};

	// JSON keys
	static final String JSON_CHECKSUM = "checksum";
	static final String JSON_NAME = "name";
	static final String JSON_OS = "os";
	static final String JSON_SIZE = "size";
	static final String JSON_TYPE = "type";

	// exception messages
	public static final String UnsupportedFileTypeException = "This file type is not supported.";
	public static final String InterruptedException = "A running program has unexpectedly been interrupted.";

	public static String WrongOSException() {
		return "The operating system must be " + MY_OS + ".";
	}

}

enum OS {
	windows, linux
}