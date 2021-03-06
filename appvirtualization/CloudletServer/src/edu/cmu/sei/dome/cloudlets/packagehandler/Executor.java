package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public abstract class Executor {

	public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
		@Override
		public boolean accept(File arg0) {
			return arg0.isDirectory();
		}
	};

	public abstract Process start(String... args) throws IOException;
}
