package edu.cmu.sei.dome.cloudlets.server;

import java.util.ArrayList;

public final class Commons {

	private Commons() {
	}

	public static final String STORE = "uploads/";
	public static final String LOG = "log/";
	public static OS MY_OS = OS.linux;
	public static boolean DEBUG = true;
	public static final String NAME = "CloudletServer";
	public static final double VERSION = 1.0;
	public static int PORT = 8080;
	public static final String CODENAME = "SINAI";

	public static final String getAttributes() {
		return String.format("name=%s,os=%s,version=%f", CODENAME,
				MY_OS.toString(), VERSION);
	}

	// file types
	public static final String FILETYPE_EXE = "exe";
	public static final String FILETYPE_CDE = "cde";
	public static final String FILETYPE_JAR = "jar";
	@SuppressWarnings("serial")
	public static final ArrayList<String> linuxTypes = new ArrayList<String>() {
		{
			add(FILETYPE_CDE);
			add(FILETYPE_JAR);
		}
	};
	@SuppressWarnings("serial")
	public static final ArrayList<String> windowsTypes = new ArrayList<String>() {
		{
			add(FILETYPE_EXE);
			add(FILETYPE_JAR);
		}
	};

	// JSON keys
	public static final String JSON_CHECKSUM = "checksum";
	public static final String JSON_NAME = "name";
	public static final String JSON_OS = "os";
	public static final String JSON_SIZE = "size";
	public static final String JSON_TYPE = "type";

	// exception messages
	public static final String UnsupportedFileTypeException = "This file type is not supported.";
	public static final String InterruptedException = "A running program has unexpectedly been interrupted.";
	public static final String GeneralException = "An exception occurred.";
	public static final String ChecksumException = "The uploaded file does not match the submitted checksum.";
	public static final String FilesizeException = "The uploaded file has not the specified size.";

	public static String WrongOSException() {
		return "The operating system must be " + MY_OS + ".";
	}

}

enum OS {
	windows, linux
}