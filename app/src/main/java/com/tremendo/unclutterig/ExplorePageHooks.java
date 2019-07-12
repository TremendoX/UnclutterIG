package com.tremendo.unclutterig;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.tremendo.unclutterig.util.*;
import java.lang.reflect.*;
import java.util.*;
import org.json.*;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.*;


public class ExplorePageHooks extends UnclutterIG {

	protected static final String UNCLUTTERED_INDICATOR_TAG = "uncluttered_indicator";

	private static String exploreTopicAdapterClassName;

	private static String exploreFeedAdapterClassName;

	private static boolean appStructureRevised;


	protected static void doHooks(final LoadPackageParam lpparam) {

		final XSharedPreferences storedHookPrefs = new XSharedPreferences(MODULE_PACKAGE_NAME, MODULE_PACKAGE_NAME+"_stored_hooks");
		storedHookPrefs.makeWorldReadable();

		JSONObject storedExploreLoaderClassesJSON = JSONUtils.asJSONObject(storedHookPrefs.getString("explore_loader_class", null));
		final String currentVersionExploreLoaderClassName = JSONUtils.getStringFromJSON(storedExploreLoaderClassesJSON, getHookedVersionName());

		if (currentVersionExploreLoaderClassName != null) {
			ExplorePageStoredHooks.doStoredExploreLoaderHooks(currentVersionExploreLoaderClassName, lpparam);
			return;
		}

		setIsAppStructureRevised(lpparam.classLoader);

		hookExploreTopicAdapter(lpparam.classLoader);

		hookExploreFeedAdapter(lpparam.classLoader);
	}



	private static void hookExploreTopicAdapter(ClassLoader classLoader) {
		try {
			ClassToScan RecyclerViewClass = new ClassToScan(findClass("com.instagram.ui.recyclerpager.HorizontalRecyclerPager", classLoader).getSuperclass());
			Method recyclerViewSetAdapterMethod = RecyclerViewClass.findMethodByName("setAdapter");

			if (recyclerViewSetAdapterMethod == null) {
				errorLog("Couldn't determine adapter for 'Explore' page header to hide suggested topics");
				return;
			}

			XposedBridge.hookMethod(recyclerViewSetAdapterMethod, new XC_MethodHook() {
				@Override
				public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
					Object adapter = param.args[0];

					if (adapter == null) {
						return;
					}

					try {
						if (isExploreTopicAdapter(adapter.getClass())) {
							if (shouldHideExploreFeed()) {
								param.setResult(null);
							}
							
							findAndStoreExploreLoaderClassName(adapter.getClass());
						}
					} catch (ReflectiveOperationException e) {
						XposedBridge.log("Unclutter IG: Stopping unsuccessful attempts at hiding 'Explore' page topics header.  Feature not available.");
						XposedBridge.unhookMethod(param.method, this);
					}
				}
			});
		} catch (XposedHelpers.ClassNotFoundError e) {
			errorLog("Couldn't find RecyclerView class to scan adapters for 'Explore' page header");
		}
	}



	private static void hookExploreFeedAdapter(ClassLoader classLoader) {
		Method setAdapterMethod = null;

		if (isAppStructureRevised()) {
			setAdapterMethod = findMethodExact(ListView.class, "setAdapter", ListAdapter.class);
		}
		else {
			try {
				ClassToScan ViewPagerClass = ClassToScan.find("android.support.v4.view.ViewPager", classLoader);
				setAdapterMethod = ViewPagerClass.findMethodByName("setAdapter");
			} catch (XposedHelpers.ClassNotFoundError e) {
				XposedBridge.log("Unclutter IG: Couldn't find ViewPager class. Not able to scan adapters for 'Explore' page feed");
			}
		}

		if (setAdapterMethod == null) {
			errorLog("Couldn't determine adapter for 'Explore' page feed to hide suggested posts");
			return;
		}

		XposedBridge.hookMethod(setAdapterMethod, new XC_MethodHook() {
			@Override
			public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
				Object adapter = param.args[0];

				if (adapter == null) {
					return;
				}

				try {
					if (isExploreFeedAdapter(adapter.getClass())) {

						if (shouldHideExploreFeed()) {
							param.setResult(null);
						}

						View viewGroup = (ViewGroup) param.thisObject;
						ViewGroup parentView = (ViewGroup) viewGroup.getParent();

						setUnclutteredStatusIndicator(parentView);
					}
				} catch (ReflectiveOperationException e) {
					XposedBridge.log("Unclutter IG: Stopping unsuccessful attempts at hiding 'Explore' page feed.  Feature not available.");
					XposedBridge.unhookMethod(param.method, this);
				}
			}
		});
	}



	/*   
	 *   Identifies the RecyclerView adapter that populates the 'Explore' page header with topics / categories.
	 *   
	 *   Can be distinguished from other RecyclerView adapters:
	 *      - Newer Instagram versions - Has method that returns 'ExploreTopicCluster' type
	 *      - Older Instagram versions - Has both Map field and 'user' class field (will need to determine correct class)
	 */
	private static boolean isExploreTopicAdapter(Class<?> AdapterObjectClass) throws ReflectiveOperationException {

		ClassToScan UnknownAdapterClass = new ClassToScan(AdapterObjectClass);

		if (UnknownAdapterClass.getName().equals(getExploreTopicAdapterClassName())) {
			return true;
		}

		boolean isExploreTopicAdapter = false;

		if (getExploreTopicAdapterClassName() == null) {

			boolean canSearchForTopicAdapter = true;
			ClassLoader classLoader = UnknownAdapterClass.getClassLoader();

			if (isAppStructureRevised()) {
				try {
					Class<?> ExploreTopicClusterClass = findClass("com.instagram.explore.topiccluster.ExploreTopicCluster", classLoader);
					isExploreTopicAdapter = UnknownAdapterClass.hasMethodType(ExploreTopicClusterClass);
				} catch (XposedHelpers.ClassNotFoundError e) {
					canSearchForTopicAdapter = false;
					errorLog("Couldn't find 'ExploreTopicCluster' class. Not able to look for method of this type within potential 'Explore' topic adapter");
				}
			}
			else {
				try {
					String userClassName = UserUtils.findUserClassName(classLoader);
					Class<?> UserClass = findClass(userClassName, classLoader);
					isExploreTopicAdapter = UnknownAdapterClass.hasFieldType(UserClass) && UnknownAdapterClass.hasFieldType(Map.class);
				} catch (ClassNotFoundException e) {
					canSearchForTopicAdapter = false;
					errorLog("Couldn't determine 'user' class. Not able to search for field of this type within potential 'Explore' topic adapter");
				}
			}

			if (isExploreTopicAdapter) {
				exploreTopicAdapterClassName = UnknownAdapterClass.getName();
			}

			if (!canSearchForTopicAdapter) {
				throw new ReflectiveOperationException();
			}
		}

		return isExploreTopicAdapter;
	}



	/*   
	 *   Identifies the adapter that populates the 'Explore' feed with recommended posts.
	 *   Adapter is either from ViewPager (older versions) or ListView (newer versions).
	 *
	 *   Can be distinguished from other adapters by having specific fields.
	 */
	private static boolean isExploreFeedAdapter(Class<?> AdapterObjectClass) throws ReflectiveOperationException {
		ClassToScan UnknownAdapterClass = new ClassToScan(AdapterObjectClass);

		if (UnknownAdapterClass.getName().equals(getExploreFeedAdapterClassName())) {
			return true;
		}

		boolean isExploreFeedAdapter = false;

		if (getExploreFeedAdapterClassName() == null) {

			if (isAppStructureRevised()) {
				try {
					Class<?> ExploreTopicClusterClass = findClass("com.instagram.explore.topiccluster.ExploreTopicCluster", UnknownAdapterClass.getClassLoader());
					isExploreFeedAdapter = UnknownAdapterClass.hasFieldType(ExploreTopicClusterClass);
				} catch (XposedHelpers.ClassNotFoundError e) {
					errorLog("Couldn't find 'ExploreTopicCluster' class. Not able to look for field of this type within potential 'Explore' feed adapter");
					throw new ReflectiveOperationException();
				}
			}
			else {
				isExploreFeedAdapter = UnknownAdapterClass.hasFieldType(AbsListView.OnScrollListener.class);
			}

			if (isExploreFeedAdapter) {
				exploreFeedAdapterClassName = UnknownAdapterClass.getName();
			}
		}

		return isExploreFeedAdapter;
	}


	/*
	 *   When feed adapter is detected, module's receiver handles this intent and stores 'loader' class name in SharedPreferences.
	 *   Next time app is started, module can directly hook loader class instead of needing to scan for correct adapters.
	 */
	private static void findAndStoreExploreLoaderClassName(Class<?> AdapterObjectClass) {
		Constructor constructor = AdapterObjectClass.getDeclaredConstructors()[0];
		Class ClassToHook = null;

		if (constructor.getParameterTypes().length > 2) {
			if (isAppStructureRevised()) {
				Class ParameterContainingRelevantConstructor = constructor.getParameterTypes()[2];
				constructor = ParameterContainingRelevantConstructor.getDeclaredConstructors()[0];
				ClassToHook = constructor.getParameterTypes()[0];
			}
			else {
				ClassToHook = constructor.getParameterTypes()[2];
			}
		}

		if (ClassToHook != null) {
			Intent intent = new Intent("com.tremendo.unclutterig.STORE_EXPLORE_HOOK");
			intent.putExtra("version", getHookedVersionName());
			intent.putExtra("explore_loader_class", ClassToHook.getName());
			AndroidAppHelper.currentApplication().sendBroadcast(intent);
		}
	}



	protected static void setUnclutteredStatusIndicator(ViewGroup viewGroup) {
		if (viewGroup == null) {
			return;
		}

		View unclutteredIndicator = viewGroup.findViewWithTag(UNCLUTTERED_INDICATOR_TAG);

		if (shouldHideExploreFeed()) {
			if (unclutteredIndicator == null) {
				viewGroup.addView(unclutteredTextView(), 0);
			} else {
				unclutteredIndicator.setVisibility(View.VISIBLE);
			}
		} else if (unclutteredIndicator != null) {
			unclutteredIndicator.setVisibility(View.GONE);
		}
	}



	private static TextView unclutteredTextView() {
		TextView textView = new TextView(AndroidAppHelper.currentApplication().getApplicationContext());
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		textView.setLayoutParams(layoutParams);
		textView.setGravity(Gravity.CENTER);
		textView.setTextColor(Color.LTGRAY);
		textView.setTag(UNCLUTTERED_INDICATOR_TAG);
		textView.setText("'Explore' page uncluttered");
		return textView;
	}



	protected static void hideView(View view) {
		if (view == null) {
			return;
		}

		view.getLayoutParams().height = 1;
		view.getLayoutParams().width = 1;
		view.setVisibility(View.GONE);
	}



	protected static int getId(String idName) {
		return AndroidAppHelper.currentApplication().getApplicationContext().getResources().getIdentifier(idName, "id", INSTAGRAM_PACKAGE_NAME);
	}



	private static String getExploreFeedAdapterClassName() {
		return exploreFeedAdapterClassName;
	}



	private static String getExploreTopicAdapterClassName() {
		return exploreTopicAdapterClassName;
	}



	/*
	 *   Revised versions of 'Explore' page are instances of ExpandingListView (inherits from ListView).
	 *   Prior versions used instances of ViewPager, from Android support library
	 */
	private static void setIsAppStructureRevised(ClassLoader classLoader) {
		try {
			findClass("com.instagram.ui.widget.expanding.ExpandingListView", classLoader);
			appStructureRevised = true;
		} catch (XposedHelpers.ClassNotFoundError e) {
			appStructureRevised = false;
		}
	}



	private static boolean isAppStructureRevised() {
		return appStructureRevised;
	}


}
