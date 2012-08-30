package edu.cmu.sei.dome.cloudlets.client.test;

import edu.cmu.sei.dome.cloudlets.client.CloudletClientActivity;
import android.test.ActivityInstrumentationTestCase2;

public class CloudletClientActivityTest extends
		ActivityInstrumentationTestCase2<CloudletClientActivity> {

	CloudletClientActivity mActivity;

	public CloudletClientActivityTest() {
		super(CloudletClientActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		setActivityInitialTouchMode(false);

		mActivity = getActivity();
	}

	public void testToast() {
		assertTrue(mActivity != null);
		mActivity.showToast("Voilà!");
	}
}
