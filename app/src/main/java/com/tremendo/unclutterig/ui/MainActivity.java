package com.tremendo.unclutterig.ui;

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.widget.*;
import com.tremendo.unclutterig.R;


public class MainActivity extends Activity {


    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment())
				.commit();
		}

	}



	public static class PrefsFragment extends PreferenceFragment {

		Toast toast;


		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
			setXposedPreference();
		}



		private void setXposedPreference() {
			Preference activateXposedPreference = getPreferenceManager().findPreference("xposed");

			if (activateXposedPreference != null) {
				if (isXposedActive()) {
					getPreferenceScreen().removePreference(activateXposedPreference);
				}
				else {
					activateXposedPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference p) {
							try {
								Intent intent = new Intent("de.robv.android.xposed.installer.OPEN_SECTION");
								intent.setPackage("de.robv.android.xposed.installer")
									  .putExtra("section", "modules")
									  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
								showToast("Launching Xposed Installer...");
							} catch (ActivityNotFoundException e) {
								showToast("Use Xposed Installer to activate module");
							}
							return true;
						}
					});
				}
			}
		}



		/*
		 *   Method is hooked in Xposed, set to true
		 */
		private boolean isXposedActive() {
			return false;
		}



		private void showToast(CharSequence message) {
			if (toast != null) {
				toast.setText(message);
			}
			else {
				toast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
			}
			toast.show();
		}

	}


}
