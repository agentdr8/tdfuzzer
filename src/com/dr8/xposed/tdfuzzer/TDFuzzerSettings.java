package com.dr8.xposed.tdfuzzer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

@SuppressLint("WorldReadableFiles")
public class TDFuzzerSettings extends Activity {

	SharedPreferences prefs = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTitle(R.string.launcher_name);
		super.onCreate(savedInstanceState);


		// Display the fragment as the main content.
		if (savedInstanceState == null)
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					new PrefsFragment()).commit();

		prefs = getSharedPreferences("com.dr8.xposed.tdfuzzer_preferences", MODE_WORLD_READABLE);
	}

	public static class PrefsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// this is important because although the handler classes that read these settings
			// are in the same package, they are executed in the context of the hooked package
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

}
