package edu.cmu.sei.dome.cloudlets.packagehandler.linux;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import edu.cmu.sei.dome.cloudlets.log.Log;

public class CDEExecutor extends LinuxTerminalExecutor {

	private static final FilenameFilter cdeFilter = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			return (filename.endsWith(".cde"));
		}
	};

	public CDEExecutor(File pkg) throws FileNotFoundException {
		super(pkg);
		File[] fs = pkg.listFiles(cdeFilter);
		if (fs.length == 0)
			throw new FileNotFoundException();
		this.executable = fs[0];
		this.executable.setExecutable(true, false);
		Log.println("Execute CDE: " + executable.getName() + ".");
	}

	@Override
	public Process execute(String... args) throws IOException {
		String[] cmd = new String[] { "./" + executable.getName() };
		String[] cmd_args = Arrays.copyOf(cmd, cmd.length + args.length);
		System.arraycopy(args, 0, cmd_args, cmd.length, args.length);

		return super.execute(cmd_args);
	}

}
