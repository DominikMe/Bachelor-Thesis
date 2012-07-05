package edu.cmu.sei.dome.cloudlets.server;

public class Log {
	
	/**
	 * Prints to standard output if Commons.DEBUG flag is set.
	 */
	public static void println(Object text) {
		if(Commons.DEBUG)
			System.out.println(text.toString());
	}

}
