package edu.cmu.sei.dome.cloudlets.client.log;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class TimeLog {

	private final static String TAG = "TimeLog";
	private static Map<Long, String> stamps = new HashMap<Long, String>();

	public static void stamp(String tag) {
		long time = System.currentTimeMillis();
		stamps.put(Long.valueOf(time), tag);
		Log.d(TAG, "@" + time + " " + tag);
	}

	public static void writeToFile(String file) throws IOException {
		String eol = System.getProperty("line.separator");
		Log.d(TAG, "Write TimeLog to " + file);
		FileWriter writer = new FileWriter(file);
		writer.write("TIMELOG " + file + eol);
		Long[] okeys = new Long[0];
		okeys = stamps.keySet().toArray(okeys);
		Arrays.sort(okeys);
		long start = okeys[0];
		for (long key : okeys) {
			long diff = key - start;
			writer.write(String.format("%d: %s (+%d ms)%s", key,
					stamps.get(key), diff, eol));
		}
		writer.close();
	}

	public static void reset() {
		stamps.clear();
	}

}
