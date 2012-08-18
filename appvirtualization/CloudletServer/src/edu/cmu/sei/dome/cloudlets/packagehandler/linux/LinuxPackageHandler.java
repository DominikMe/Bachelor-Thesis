package edu.cmu.sei.dome.cloudlets.packagehandler.linux;

import java.io.File;
import java.io.FileNotFoundException;

import edu.cmu.sei.dome.cloudlets.constants.Commons;
import edu.cmu.sei.dome.cloudlets.constants.OS;
import edu.cmu.sei.dome.cloudlets.fileprocessing.FileDecompressor;
import edu.cmu.sei.dome.cloudlets.log.Log;
import edu.cmu.sei.dome.cloudlets.packagehandler.Executor;
import edu.cmu.sei.dome.cloudlets.packagehandler.PackageHandlerImpl;
import edu.cmu.sei.dome.cloudlets.packagehandler.PackageInfo;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.WrongOSException;

public class LinuxPackageHandler implements PackageHandlerImpl {

	@Override
	public void decompress(String appId) throws PackageNotFoundException {
		File pkg = new File(Commons.STORE + appId);
		if (!pkg.isDirectory())
			throw new PackageNotFoundException();
		File[] fs = pkg.listFiles(FileDecompressor.TARGZ_FILTER);
		if (fs.length == 0)
			throw new PackageNotFoundException();
		File archive = fs[0];
		FileDecompressor.untargz(archive.getAbsolutePath());
	}

	private static final String CDE = "cde";
	private static final String JAR = "jar";

	@Override
	public Executor execute(String appId) throws UnsupportedFileTypeException,
			PackageNotFoundException, WrongOSException, FileNotFoundException {
		File pkg = new File(Commons.STORE + appId);
		if (!pkg.isDirectory())
			throw new PackageNotFoundException();

		PackageInfo info = PackageInfo.getPackageInfo(appId);
		Log.println("Compare OS: " + info.os + " ?= linux");
		if (!info.os.toLowerCase().equals(OS.linux.toString())) {
			throw new WrongOSException();
		}
		Log.println("Filetype is " + info.type + ".");
		
		File[] fs = pkg.listFiles(Executor.DIRECTORY_FILTER);
		if (fs.length == 0)
			throw new PackageNotFoundException();
		File pkgDir = fs[0];
		
		if (info.type.equals(CDE)) {
			return new CDEExecutor(pkgDir);
		} else if (info.type.equals(JAR)) {
			return new JARExecutor(pkgDir);
		} else
			throw new UnsupportedFileTypeException();
	}

}
