package edu.cmu.sei.dome.cloudlets.log;

import edu.cmu.sei.dome.cloudlets.server.Commons;

public class Log {
	
	private Log() {
	}
	
	/**
	 * Prints to standard output if Commons.DEBUG flag is set.
	 */
	public static void println(Object text) {
		if(Commons.DEBUG)
			System.out.println(text.toString());
	}

}
