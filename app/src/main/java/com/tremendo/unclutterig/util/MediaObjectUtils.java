package com.tremendo.unclutterig.util;

import com.tremendo.unclutterig.*;
import java.lang.reflect.*;
import java.util.*;

import de.robv.android.xposed.*;
import static de.robv.android.xposed.XposedHelpers.*;


public final class MediaObjectUtils {


	private static List<String> mediaObjectFieldNamesToScan;

	private static String sponsoredContentObjectFieldName;

	private static String paidPartnershipObjectMethodName;

	private static boolean shouldFindPaidPartnership = true;



	private MediaObjectUtils() {
		throw new UnsupportedOperationException();
	}



	public static boolean isSponsoredContent(Object mediaObject) {
		String sponsoredObjectField = findSponsoredContentObjectFieldName(mediaObject);

		if (sponsoredObjectField != null) {
			Object sponsoredContentDataObject = getObjectField(mediaObject, sponsoredObjectField);
			return (sponsoredContentDataObject != null);
		}

		return false;
	}



	/*
	 *   'Media object' class has many fields of unknown types. Scanning the fields within those objects to look for a 'Sponsored' string value.
	 *   May be called many times while user is scrolling before getting desired result; reuses list of appropriate field names to be scanned.
	 */
	private static String findSponsoredContentObjectFieldName(Object mediaObject) {
		if (getSponsoredContentObjectFieldName() == null) {
			List<String> fieldNamesToScan = getFieldNamesToScan(mediaObject);

			for (String fieldNameInMediaObject : fieldNamesToScan) {
				Object unknownObject = getObjectField(mediaObject, fieldNameInMediaObject);

				if (hasSponsoredString(unknownObject)) {
					setSponsoredContentObjectFieldName(fieldNameInMediaObject);
					break;
				}
			}
		}

		return getSponsoredContentObjectFieldName();
	}



	public static boolean isPaidPartnershipContent(Object mediaObject) {

		if (getPaidPartnershipMethodName() != null) {
			Object paidPartnershipDataObject = callMethod(mediaObject, paidPartnershipObjectMethodName);
			return (paidPartnershipDataObject != null);
		}

		if (isFindingPaidPartnershipContentSupported()) {
			boolean supported;

			try {
				String userObjectClassName = UserUtils.findUserClassName(mediaObject.getClass().getClassLoader());
				Class<?> UserClass = findClass(userObjectClassName, mediaObject.getClass().getClassLoader());

				/*
				 *   Should be two methods that return objects of the 'user' class within the media object:
				 *   - One object holds 'partner' data when applicable or is otherwise null in non-branded content.  The calling method should have no parameters.
				 *   - Second object always exists, for the user who submitted the post (calling method may also have parameters in newer IG versions and can be disregarded).
				 *
				 *   Keeps a reference to the name of the method that can return null.
				 */
				Method[] userObjectMethods = findMethodsByExactParameters(mediaObject.getClass(), UserClass);

				supported = (userObjectMethods.length > 0);

				if (userObjectMethods.length > 1) {
					boolean includesNullUserItem = false;

					for (int index = 0; index < userObjectMethods.length; index++) {
						Object userMetadataObject = callMethod(mediaObject, userObjectMethods[index].getName());

						if (userMetadataObject == null) {
							includesNullUserItem = true;
							setPaidPartnershipMethodName(userObjectMethods[index].getName());
							break;
						}
					}

					return (!includesNullUserItem);
				}

				else if (userObjectMethods.length == 1) {
					setPaidPartnershipMethodName(userObjectMethods[0].getName());

					Object paidPartnershipDataObject = callMethod(mediaObject, paidPartnershipObjectMethodName);
					return (paidPartnershipDataObject != null);
				}

			} catch (ClassNotFoundException e) {
				supported = false;
				UnclutterIG.errorLog("Couldn't determine 'user' class. Not able to search for methods of this type within media object to find branded content");
			}

			setShouldFindPaidPartnership(supported);

		}

		return false;
	}



	private static String getSponsoredContentObjectFieldName() {
		return sponsoredContentObjectFieldName;
	}



	private static void setSponsoredContentObjectFieldName(String fieldName) {
		sponsoredContentObjectFieldName = fieldName;
	}



	private static String getPaidPartnershipMethodName() {
		return paidPartnershipObjectMethodName;
	}



	private static void setPaidPartnershipMethodName(String methodName) {
		paidPartnershipObjectMethodName = methodName;
	}



	private static void setShouldFindPaidPartnership(boolean shouldAttempt) {
		shouldFindPaidPartnership = shouldAttempt;
	}



	private static boolean isFindingPaidPartnershipContentSupported() {
		return shouldFindPaidPartnership;
	}



	private static List<String> getFieldNamesToScan(Object mediaObject) {
		List<String> fieldNamesToScan;

		if (mediaObjectFieldNamesToScan != null) {
			fieldNamesToScan = new ArrayList<String>(mediaObjectFieldNamesToScan);
		} else {
			fieldNamesToScan = new ArrayList<String>(0);

			for (Field field: mediaObject.getClass().getDeclaredFields()) {
				Class fieldType = field.getType();

				if (!fieldType.getName().startsWith("android") && !fieldType.getName().startsWith("java") && !fieldType.isPrimitive()) {
					fieldNamesToScan.add(field.getName());
				}
			}

			mediaObjectFieldNamesToScan = fieldNamesToScan;
		}

		return fieldNamesToScan;
	}



	private static boolean hasSponsoredString(Object object) {
		if (object != null) {
			Field[] declaredFields = object.getClass().getDeclaredFields();

			for (Field field: declaredFields) {
				if (field.getType() == String.class) {
					if ("Sponsored".equals(getObjectField(object, field.getName()))) {
						return true;
					}
				}
			}
		}

		return false;
	}


}
