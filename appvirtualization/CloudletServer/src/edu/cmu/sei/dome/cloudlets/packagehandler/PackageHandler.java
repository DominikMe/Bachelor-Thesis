package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.FileNotFoundException;

import edu.cmu.sei.dome.cloudlets.constants.OS;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.WrongOSException;
import edu.cmu.sei.dome.cloudlets.packagehandler.linux.LinuxPackageHandler;
import edu.cmu.sei.dome.cloudlets.packagehandler.windows.WindowsPackageHandler;

public final class PackageHandler {

	private PackageHandlerImpl impl;
	private static PackageHandler instance;

	private PackageHandler(OS os) {
		switch (os) {
		case windows:
			impl = new WindowsPackageHandler();
			break;
		case linux:
			impl = new LinuxPackageHandler();
			break;
		}
	}

	public static synchronized PackageHandler getInstance(OS os) {
		switch (os) {
		case windows:
			if ((instance == null)
					|| !(instance.impl instanceof WindowsPackageHandler)) {
				instance = new PackageHandler(os);
			}
			return instance;
		case linux:
			if ((instance == null)
					|| !(instance.impl instanceof LinuxPackageHandler)) {
				instance = new PackageHandler(os);
			}
			return instance;
		}
		return null;
	}

	public void decompress(String appId) throws PackageNotFoundException {
		impl.decompress(appId);
	}

	public Executor execute(String appId) throws UnsupportedFileTypeException,
			PackageNotFoundException, WrongOSException, FileNotFoundException {
		return impl.execute(appId);
	}

}
