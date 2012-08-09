package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.FileNotFoundException;

import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.WrongOSException;
import edu.cmu.sei.dome.cloudlets.packagehandler.linux.LinuxPackageHandler;
import edu.cmu.sei.dome.cloudlets.packagehandler.windows.WindowsPackageHandler;
import edu.cmu.sei.dome.cloudlets.server.OS;

public class PackageHandler {

	PackageHandlerImpl impl;

	public PackageHandler(OS os) {
		switch (os) {
		case windows:
			impl = new WindowsPackageHandler();
			break;
		case linux:
			impl = new LinuxPackageHandler();
			break;
		}
	}

	public void decompress(String pkgId) throws PackageNotFoundException {
		impl.decompress(pkgId);
	}

	public Executor execute(String pkgId) throws UnsupportedFileTypeException,
			PackageNotFoundException, WrongOSException, FileNotFoundException {
		return impl.execute(pkgId);
	}

}
