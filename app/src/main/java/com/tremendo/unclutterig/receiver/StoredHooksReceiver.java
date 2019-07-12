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

	}



	private void storeExploreLoaderClass(Intent intent) {
		String appVersionName = intent.getStringExtra("version");
		String exploreLoaderClassName = intent.getStringExtra("explore_loader_class");

		if (appVersionName == null || exploreLoaderClassName == null) {
			return;
		}

		String storedExploreLoaderClassInfo = storedHookPrefs.getString("explore_loader_class", null);
		JSONObject storedExploreLoaderClassJSON = JSONUtils.asJSONObject(storedExploreLoaderClassInfo);

		if (!storedExploreLoaderClassJSON.has(appVersionName)) {
			try {
				Log.i("Unclutter IG", String.format("Storing 'Explore' page hooks for version '%s'", appVersionName));
				storedExploreLoaderClassJSON.put(appVersionName, exploreLoaderClassName);
				storedHookPrefs.edit()
					.putString("explore_loader_class", storedExploreLoaderClassJSON.toString().replace("\"", "'"))
					.apply();
			} catch (JSONException e) {
				Log.w("Unclutter IG", "Error putting value in JSONObject:\n" + e);
			}
		}
	}


}
