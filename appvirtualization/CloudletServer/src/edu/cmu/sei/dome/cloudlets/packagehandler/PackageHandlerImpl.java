package edu.cmu.sei.dome.cloudlets.packagehandler;

import java.io.FileNotFoundException;

import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.PackageNotFoundException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.UnsupportedFileTypeException;
import edu.cmu.sei.dome.cloudlets.packagehandler.exceptions.InvalidCloudletException;

public interface PackageHandlerImpl {

	public void decompress(String appId) throws PackageNotFoundException;

	public Executor execute(String appId) throws UnsupportedFileTypeException,
			PackageNotFoundException, InvalidCloudletException, FileNotFoundException;

}
