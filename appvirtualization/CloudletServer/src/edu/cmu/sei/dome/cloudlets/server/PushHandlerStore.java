package edu.cmu.sei.dome.cloudlets.server;

import java.util.HashMap;

public class PushHandlerStore {
	
	private static HashMap<String, PushHandler> pushs = new HashMap<String, PushHandler>();

	public static synchronized PushHandler getPushHandler(String appId) {
		PushHandler push = pushs.get(appId);
		if (push == null) {
			push = new PushHandler(appId);
			pushs.put(appId, push);
		}
		return push;
	}
	
	static synchronized void close(String appId) {
		pushs.remove(appId);
	}

}
