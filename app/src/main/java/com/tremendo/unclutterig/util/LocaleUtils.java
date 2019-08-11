package com.tremendo.unclutterig.util;

import java.util.*;


public final class LocaleUtils {


	private LocaleUtils() {
		throw new UnsupportedOperationException();
	}



	/*
	 *   These Locale languages appear to have 'Sponsored' labels at runtime that differ (or are not provided) from what's expected in the default resource string
	 *   (Not thoroughly tested for regional Locales)
	 */
	private static HashMap<String, String> getKnownLocaleAlternates() {
		HashMap<String, String> map = new HashMap<String, String>();

		map.put("ar", getSponsoredLabelArabic());	// Arabic 	(Replaces 'مُموَّل')
		map.put("bn", "প্রযোজিত"); 					// Bengali  (Added)
		map.put("fil", "Sponsored");				// Filipino (Replaces 'May sponsor')
		map.put("fr", "Sponsorisé");				// French 	(Replaces 'Sponsorisée')
		map.put("ta", "வ்பரதாரர்கள்");				// Tamil 	(Added)
		map.put("ur", getSponsoredLabelUrdu());		// Urdu 	(Added)

		return map;
	}



	protected static boolean hasAlternateValue(String localeLanguage) {
		return getKnownLocaleAlternates().containsKey(localeLanguage);
	}



	protected static String getAlternateValue(String localeLanguage) {
		return getKnownLocaleAlternates().get(localeLanguage);
	}



	/*
	 *   Converts 'مُموَّل' value from runtime to Unicode. 'مُموَّل' is defined in IG resources but doesn't return proper match when comparing strings)
	 */
	private static String getSponsoredLabelArabic() {
		return unicodeToString("\u0645\u064F\u0645\u0648\u0651\u064E\u0644");
	}



	/*
	 *   Converts 'بتعاون' value from runtime to Unicode.  Value is not defined in IG resources
	 */
	private static String getSponsoredLabelUrdu() {
		return unicodeToString("\u0628\u062A\u0639\u0627\u0648\u0646");
	}



	private static String unicodeToString(String unicodeRepresentation) {
		try {
			byte[] bytes = unicodeRepresentation.getBytes("UTF-8");
			return new String(bytes, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return null;
		}
	}


}
