package edu.cmu.sei.rtss.cloudlet.facerec.test;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import edu.cmu.sei.rtss.cloudlet.facerec.ui.FaceRecClientCameraPreview;

public class FaceRecClientCameraPreviewTest extends
		ActivityInstrumentationTestCase2<FaceRecClientCameraPreview> {

	FaceRecClientCameraPreview alfred;
	Instrumentation instr;

	public FaceRecClientCameraPreviewTest() {
		super(FaceRecClientCameraPreview.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		setActivityInitialTouchMode(false);
		instr = getInstrumentation();
		Intent args = new Intent(instr.getTargetContext(), FaceRecClientCameraPreview.class);
		args.putExtra("address", "192.168.168.176");
		args.putExtra("port", 9876);
		setActivityIntent(args);
		

		alfred = getActivity();
	}

	public void testBla() {
		assertTrue(alfred == null);
	}
}
