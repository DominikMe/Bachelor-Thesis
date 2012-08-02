package edu.cmu.sei.dome.cloudlets.packagehandler.linux;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.log.TimeLog;

public class InstallExecutor extends LinuxTerminalExecutor {

	private static final FileFilter executableFilter = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return !f.isDirectory() && f.canExecute();
		}
	};

	public InstallExecutor(File pkg) throws FileNotFoundException {
		super(pkg);
		File[] fs = pkg.listFiles(executableFilter);
		if (fs.length == 0)
			throw new FileNotFoundException();
		this.executable = fs[0];
		this.executable.setExecutable(true, false);
		Log.println("Execute CDE: " + executable.getName() + ".");
	}

	@Override
	public Process execute(String... args) throws IOException {
		try {
			install(this.cwd).waitFor();
			TimeLog.stamp("Installation finished.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String[] cmd = new String[] { "./" + executable.getName() };
		String[] cmd_args = Arrays.copyOf(cmd, cmd.length + args.length);
		System.arraycopy(args, 0, cmd_args, cmd.length, args.length);

		return super.execute(cmd_args);
	}

	private Process install(File cwd) throws IOException {
		TimeLog.stamp("Start installation.");
		Log.println("Start remote installation of: " + cwd.getName());

		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(cwd);
		String[] cmd = new String[] { "bash", "-c",
				"echo jk42GbU | sudo -S dpkg -i packages/archives/*" };
		pb.command(cmd);
		return pb.start();
	}

}
