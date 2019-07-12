package com.tremendo.unclutterig.util;

import android.os.*;
import com.tremendo.unclutterig.*;
import java.lang.reflect.*;

import de.robv.android.xposed.*;
import static de.robv.android.xposed.XposedHelpers.*;


public class UserUtils {

	private static String userClassName;


	private UserUtils() {
		throw new UnsupportedOperationException();
	}



	/*
	 *   There should be a constructor with the targeted parameter type in PeopleTag: 'public PeopleTag(?)'
	 *   Conditions: (1) One parameter only (2) Parameter is not 'PeopleTag' or 'Parcel' type
	 */
	public static String findUserClassName(ClassLoader classLoader) throws ClassNotFoundException {

		if (getUserClassName() != null) {
			return getUserClassName();
		}

		try {
			Class<?> PeopleTagClass = findClass("com.instagram.model.people.PeopleTag", classLoader);
			Constructor[] constructors = PeopleTagClass.getDeclaredConstructors();

			for (Constructor constructor: constructors) {
				Class[] parameterTypes = constructor.getParameterTypes();

				if (parameterTypes.length == 1) {
					Class parameterType = parameterTypes[0];

					if (parameterType != PeopleTagClass && parameterType != Parcel.class) {
						setUserClassName(parameterType.getName());
						return getUserClassName();
					}
				}
			}

		} catch (XposedHelpers.ClassNotFoundError e) {
			UnclutterIG.errorLog("Couldn't find 'PeopleTag' class. Not able to scan its constructors to look for 'user' class name");
		}

		throw new ClassNotFoundException("Not able to find 'user' class name");
	}



	private static String getUserClassName() {
		return userClassName;
	}



	private static void setUserClassName(String className) {
		userClassName = className;
	}


}
