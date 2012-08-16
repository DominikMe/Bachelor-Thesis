package edu.cmu.sei.dome.cloudlets.log;

import java.util.HashMap;

public class TimeLogStore {

	private static HashMap<String, TimeLog> timeLogs = new HashMap<String, TimeLog>();

	private TimeLogStore() {
	}

	public static synchronized TimeLog getTimeLog(String appId) {
		TimeLog tlog = timeLogs.get(appId);
		if (tlog == null) {
			tlog = new TimeLog(appId);
			timeLogs.put(appId, tlog);
		}
		return tlog;
	}

	public static synchronized void close(String appId) {
		timeLogs.remove(appId);
	}
}
