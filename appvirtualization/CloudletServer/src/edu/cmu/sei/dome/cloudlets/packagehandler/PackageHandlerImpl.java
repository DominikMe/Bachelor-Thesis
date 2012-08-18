package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.FileNotFoundException;

import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.WrongOSException;

public interface PackageHandlerImpl {

	void decompress(String appId) throws PackageNotFoundException;

	Executor execute(String appId) throws UnsupportedFileTypeException,
			PackageNotFoundException, WrongOSException, FileNotFoundException;

}
