package com.tremendo.unclutterig;

import android.content.*;
import android.view.*;
import com.tremendo.unclutterig.util.*;
import java.lang.reflect.*;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.*;


public class MainFeedHooks extends UnclutterIG {


	public static void doHooks(final LoadPackageParam lpparam) {

		String feedViewClassName = findFeedViewClassName(lpparam.classLoader);

		if (feedViewClassName == null) {
			errorLog("Couldn't determine 'main feed' class to hide sponsored content");
			return;
		}

		Class<?> FeedViewClass = findClass(feedViewClassName, lpparam.classLoader);
		Method[] methodsToHook = findMethodsByExactParameters(FeedViewClass, View.class, int.class, View.class, ViewGroup.class, Object.class, Object.class);

		if (methodsToHook.length == 0) {
			errorLog("Couldn't find method within 'main feed' class to hide sponsored content");
			return;
		}

		Method feedItemViewMethod = methodsToHook[0];

		XposedBridge.hookMethod(feedItemViewMethod, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				View feedItemView = (View) param.getResult();
				Object mediaObjectInFeedItem = param.args[3];

				if (mediaObjectInFeedItem != null) {

					if (shouldHideAds()) {
						if (MediaObjectUtils.isSponsoredContent(mediaObjectInFeedItem)) {
							showFeedItemView(feedItemView, false);
							return;
						}
					}

					if (shouldHidePaidPartnershipPosts()) {
						if (MediaObjectUtils.isPaidPartnershipContent(mediaObjectInFeedItem)) {
							showFeedItemView(feedItemView, false);
							return;
						}
					}
				}

				showFeedItemView(feedItemView, true);
			}
		});

	}



	/* 
	 *   Scans field types in the third parameter of LoadMoreButton's method 'setViewType(LoadMoreButton, ?, *?*)'.
	 *   The relevant field type will contain a Context field and 'getViewTypeCount' method
	 */
	private static String findFeedViewClassName(ClassLoader classLoader) {
		try {
			ClassToScan LoadMoreButtonClass = ClassToScan.find("com.instagram.ui.widget.loadmore.LoadMoreButton", classLoader);
			Method setViewTypeMethod = LoadMoreButtonClass.findMethodByName("setViewType");

			if (setViewTypeMethod != null && setViewTypeMethod.getParameterTypes().length > 2) {
				Class<?> ParameterContainingRelevantFieldType = setViewTypeMethod.getParameterTypes()[2];

				for (Field declaredField: ParameterContainingRelevantFieldType.getDeclaredFields()) {
					ClassToScan DeclaredFieldType = new ClassToScan(declaredField.getType());

					if (DeclaredFieldType.findMethodByName("getViewTypeCount") != null && DeclaredFieldType.hasFieldType(Context.class)) {
						return DeclaredFieldType.getName();
					}
				}
			}
		} catch (XposedHelpers.ClassNotFoundError e) {
			XposedBridge.log("Unclutter IG: Couldn't find 'LoadMoreButton' class. Not able to scan its methods to search for 'main feed' class name");
		}

		return null;
	}



	private static void showFeedItemView(View view, boolean setVisible) {
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

		if (layoutParams == null) {
			layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		if (setVisible) {
			layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			view.setVisibility(View.VISIBLE);
		} else {
			layoutParams.height = 1;
			view.setVisibility(View.GONE);
		}

		view.setLayoutParams(layoutParams);
	}


}
