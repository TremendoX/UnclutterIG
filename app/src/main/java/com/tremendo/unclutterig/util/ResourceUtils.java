package com.tremendo.unclutterig.util;

import android.app.*;
import android.content.res.*;
import com.tremendo.unclutterig.*;
import org.json.*;

import de.robv.android.xposed.*;


public final class ResourceUtils {


	public static int getId(String resourceName) {
		return getId(resourceName, "id");
	}



	public static int getId(String resourceName, String resourceType) {
		return getId(resourceName, resourceType, AndroidAppHelper.currentPackageName());
	}



	public static int getId(String resourceName, String resourceType, String pkgName) {
		return getResources().getIdentifier(resourceName, resourceType, pkgName);
	}



	public static String getString(String resourceName) {
		int resourceId = getId(resourceName, "string");
		return getString(resourceId);
	}



	public static String getString(int resourceId) {
		return getResources().getString(resourceId);
	}



	public static float getDimension(int dimensionId) {
		return getResources().getDimension(dimensionId);
	}



	private static Resources getResources() {
		return AndroidAppHelper.currentApplication().getApplicationContext().getResources();
	}



	public static String getCurrentVersionStoredValue(String key) {
		final XSharedPreferences storedHookPrefs = new XSharedPreferences(UnclutterIG.MODULE_PACKAGE_NAME, UnclutterIG.MODULE_PACKAGE_NAME+"_stored_hooks");
		storedHookPrefs.makeWorldReadable();

		JSONObject storedValuesJSON = JSONUtils.asJSONObject(storedHookPrefs.getString(key, null));
		String currentVersionStoredValue = JSONUtils.getStringFromJSON(storedValuesJSON, UnclutterIG.getHookedVersionName());

		return currentVersionStoredValue;
	}


}
