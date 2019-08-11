package com.tremendo.unclutterig;

import android.content.*;
import android.content.pm.*;
import com.tremendo.unclutterig.util.*;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.*;


public class UnclutterIG implements IXposedHookLoadPackage {

	public static final String MODULE_PACKAGE_NAME = UnclutterIG.class.getPackage().getName();

	protected static final String INSTAGRAM_PACKAGE_NAME = "com.instagram.android";

	private static String hookedAppVersion;

	private static boolean shouldHideAds;

	private static boolean shouldHidePaidPosts;

	private static boolean shouldHideExploreFeed;


	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

		if (MODULE_PACKAGE_NAME.equals(lpparam.packageName)) {
			findAndHookMethod(com.tremendo.unclutterig.ui.MainActivity.PrefsFragment.class.getName(), lpparam.classLoader,
			"isXposedActive", XC_MethodReplacement.returnConstant(true));
		}

		if (!INSTAGRAM_PACKAGE_NAME.equals(lpparam.packageName) || !INSTAGRAM_PACKAGE_NAME.equals(lpparam.processName)) {
			return;
		}


		resetVariables();

		new MainFeedHooks(lpparam).doHooks();

		new StoryReelHooks(lpparam).doHooks();

		new ExplorePageHooks(lpparam).doHooks();

		String mainActivityClassName = findMainActivityClassName();

		if (mainActivityClassName != null) {
			tryHookingMethod(mainActivityClassName, lpparam.classLoader,
			"onResume", new XC_MethodHook() {
				@Override
				public void afterHookedMethod(final MethodHookParam param) throws Throwable {
					reloadModulePreferences();
				}
			});
		}

	}



	private static String findMainActivityClassName() {
		PackageInfo packageInfo = getPackageInfo();

		for (ActivityInfo activityInfo : packageInfo.activities) {
			String activityName = activityInfo.name;

			if (activityName.contains(".MainActivity")) {
				return activityName;
			}
		}

		return null;
	}



	public static String getHookedVersionName() {
		if (hookedAppVersion == null) {
			hookedAppVersion = getPackageInfo().versionName;
		}

		return hookedAppVersion;
	}



	private static PackageInfo getPackageInfo() {
		Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
		Context context = (Context) callMethod(activityThread, "getSystemContext");
		PackageManager packageManager = context.getPackageManager();

		try {
			return packageManager.getPackageInfo(INSTAGRAM_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
		} catch (PackageManager.NameNotFoundException e) {
			XposedBridge.log("Unclutter IG: Couldn't find Instagram package name in PackageManager.");
			return null;
		}
	}



	protected static void reloadModulePreferences() {
		XSharedPreferences prefs = new XSharedPreferences(MODULE_PACKAGE_NAME);

		prefs.makeWorldReadable();
		shouldHideAds = prefs.getBoolean("hide_ads", false);
		shouldHidePaidPosts = prefs.getBoolean("hide_paidpartnerships", false);
		shouldHideExploreFeed = prefs.getBoolean("hide_explore", false);
	}



	protected static boolean shouldHideAds() {
		return shouldHideAds;
	}



	protected static boolean shouldHidePaidPartnershipPosts() {
		return shouldHidePaidPosts;
	}



	protected static boolean shouldHideExploreFeed() {
		return shouldHideExploreFeed;
	}



	protected static void tryHookingMethod(String className, ClassLoader classLoader, final String methodName, final Object... parameterTypesAndCallback) {
		try {
			findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
		} catch (NoSuchMethodError e) {
			errorLog(String.format("No such method '%s' in class '%s'", methodName, className));
		} catch (XposedHelpers.ClassNotFoundError e) {
			errorLog(String.format("Couldn't find class '%s'", className));
		}
	}



	public static void errorLog(CharSequence errorMessage) {
		XposedBridge.log("Unclutter IG: " + errorMessage + "...  Instagram version '" + getHookedVersionName() + "' not supported");
	}



	private void resetVariables() {
		hookedAppVersion = null;
		reloadModulePreferences();
		MediaObjectUtils.resetVariables();
	}


}
