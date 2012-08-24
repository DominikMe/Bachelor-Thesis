import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import edu.cmu.sei.dome.cloudlets.constants.CloudletProperties;
import edu.cmu.sei.dome.cloudlets.packagehandler.PackageInfo;

public class JSONConfigTests {

	@Test
	public void test() throws FileNotFoundException, IOException {
		PackageInfo info = PackageInfo.getPackageInfo(new FileInputStream(
				"test.json"));
		Assert.assertEquals(info.name, "FaceRecognition");
		Assert.assertEquals(info.description,
				"This application does Face Recognition.");
		Assert.assertEquals(info.checksum, "e918a05b332ce38f6ca2bd217bed7d3b");
		Assert.assertEquals(info.size, 12492671);
		Assert.assertEquals(info.type, "exe");
		Assert.assertEquals(info.clientPackage,
				"edu.cmu.sei.rtss.cloudlet.facerec");
		Assert.assertEquals(info.port, 9876);

		Map<String, Object> c1 = info.cloudlets[0];
		Assert.assertEquals(c1.get("os"), "windows");
		Assert.assertEquals(c1.get("architecture"), "x86");
		Assert.assertEquals(c1.get("cores_min"), 2l);

		Map<String, Object> c2 = info.cloudlets[1];
		Assert.assertEquals(c2.get("os"), "windows");
		Assert.assertEquals(c2.get("architecture"), "x86-64");
		System.out.println(info);
	}

	@Test
	public void testProps() throws IOException {
		Assert.assertEquals(CloudletProperties.getCloudletProperties("cloudlet.json")
				.toString(), "os:windows,architecture:x86,cores:2");
	}

	@Test
	public void testMatch() throws IOException {
		PackageInfo info = PackageInfo.getPackageInfo(new FileInputStream(
				"test.json"));
		CloudletProperties props = CloudletProperties.getCloudletProperties("cloudlet.json");
		Assert.assertTrue(info.matches(props));
		Assert.assertTrue(props.matches(info));
	}
}
