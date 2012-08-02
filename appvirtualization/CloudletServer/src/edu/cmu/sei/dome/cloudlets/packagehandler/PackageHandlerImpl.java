package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.FileNotFoundException;

import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.WrongOSException;

public interface PackageHandlerImpl {

	void decompress(String pkgId) throws PackageNotFoundException;

	Executor execute(String pkgId) throws UnsupportedFileTypeException,
			PackageNotFoundException, WrongOSException, FileNotFoundException;

}
