package edu.cmu.sei.dome.cloudlets.log;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.sei.dome.cloudlets.server.Commons;

public class Log {

	private static Map<String, ArrayList<String>> logbook = new HashMap<String, ArrayList<String>>();
	private static final String GENERAL_TAG = "";

	private Log() {
	}

	/**
	 * Prints to standard output if Commons.DEBUG flag is set.
	 */
	public static synchronized void println(String tag, Object text) {
		if (Commons.DEBUG) {
			String logtext = text.toString();

			if (logbook.get(tag) == null)
				logbook.put(tag, new ArrayList<String>());
			logbook.get(tag).add(logtext);
			System.out.println(logtext);
		}
	}

	public static synchronized void println(Object text) {
		Log.println(Log.GENERAL_TAG, text);
	}

	public synchronized void writeToFile(String tag, String file)
			throws IOException {
		ArrayList<String> logs = logbook.get(tag);
		if (logs != null) {
			String eol = System.lineSeparator();
			FileWriter writer = new FileWriter(file);
			writer.write("LOG for tag \'" + tag + "\'" + eol);
			for (String logtext : logs) {
				writer.write(logtext + eol);
			}
			writer.close();
		}
	}
}
