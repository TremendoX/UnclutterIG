package com.tremendo.unclutterig.receiver;

import android.content.*;
import android.util.*;
import com.tremendo.unclutterig.util.*;
import org.json.*;


public class StoredHooksReceiver extends BroadcastReceiver {

	SharedPreferences storedHookPrefs;


	@Override
	public void onReceive(Context context, Intent receivedIntent) {
		String intentAction = receivedIntent.getAction();

		if (intentAction == null) {
			Log.e("Unclutter IG", "Null intent action received");
			return;
		}

		storedHookPrefs = context.getSharedPreferences(context.getPackageName()+"_stored_hooks", Context.MODE_WORLD_READABLE);

		if ("com.tremendo.unclutterig.STORE_EXPLORE_HOOK".equals(intentAction)) {
			storeExploreLoaderClass(receivedIntent);
		}

		if ("com.tremendo.unclutterig.STORE_SPONSORED_FIELD".equals(intentAction)) {
			storeSponsoredFieldName(receivedIntent);
		}

	}



	private void storeExploreLoaderClass(Intent intent) {
		String appVersionName = intent.getStringExtra("version");
		String exploreLoaderClassName = intent.getStringExtra("explore_loader_class");

		storeValue(appVersionName, "explore_loader_class", exploreLoaderClassName);
	}



	private void storeSponsoredFieldName(Intent intent) {
		String appVersionName = intent.getStringExtra("version");
		String sponsoredFieldName = intent.getStringExtra("sponsored_object_field_name");

		storeValue(appVersionName, "sponsored_object_field_name", sponsoredFieldName);
	}

	
	
	private void storeValue(String appVersion, String valueKey, String value) {
		
		if (appVersion == null || valueKey == null || value == null) {
			return;
		}

		String storedValuesStringInfo = storedHookPrefs.getString(valueKey, null);
		JSONObject storedValuesJSON = JSONUtils.asJSONObject(storedValuesStringInfo);

		if (!storedValuesJSON.has(appVersion)) {
			try {
				Log.i("Unclutter IG", String.format("Storing '%s' for version '%s'", valueKey, appVersion));
				storedValuesJSON.put(appVersion, value);
				storedHookPrefs.edit()
					.putString(valueKey, storedValuesJSON.toString().replace("\"", "'"))
					.apply();
			} catch (JSONException e) {
				Log.w("Unclutter IG", "Error putting value in JSONObject:\n" + e);
			}
		}
	}
	

}
