package edu.cmu.sei.dome.cloudlets.packagehandler.windows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import edu.cmu.sei.dome.cloudlets.log.Log;

public class CameyoExecutor extends WindowsTerminalExecutor {

	private static final FilenameFilter CAMEYO_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".exe"));
		}
	};

	public CameyoExecutor(File pkg) throws FileNotFoundException {
		super(pkg);
		File[] fs = pkg.listFiles(CAMEYO_FILTER);
		if (fs.length == 0)
			throw new FileNotFoundException();
		this.executable = fs[0];
		this.executable.setExecutable(true, false);
		Log.println("Execute Cameyo-EXE: " + executable.getName() + ".");
	}

	@Override
	public Process start(String... args) throws IOException {
		String[] cmd = new String[] { executable.getName(), "-exec", "#1" };
		String[] cmd_args = Arrays.copyOf(cmd, cmd.length + args.length);
		System.arraycopy(args, 0, cmd_args, cmd.length, args.length);

		return super.start(cmd_args);
	}

}
