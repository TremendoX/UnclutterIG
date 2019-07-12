package com.tremendo.unclutterig.util;

import android.util.*;
import org.json.*;


public final class JSONUtils extends JSONObject {


	private JSONUtils() {
		throw new UnsupportedOperationException();
	}



	public static JSONObject asJSONObject(String jsonString) {
		JSONObject jsonObject = new JSONObject();

		if (jsonString != null) {
			try {
				jsonObject = new JSONObject(jsonString);
			} catch (JSONException e) {
				Log.w("Unclutter IG", String.format("Error parsing string into JSONObject: \"%s\"", jsonString));
				return new JSONObject();
			}
		}

		return jsonObject;
	}



	public static String getStringFromJSON(JSONObject jsonObject, String key) {
		if (jsonObject.has(key)) {
			try {
				return jsonObject.getString(key);
			} catch (JSONException e) {
				Log.w("Unclutter IG", String.format("Error retrieving string \"%s\" from JSONObject", key));
			}
		}

		return null;
	}


}
