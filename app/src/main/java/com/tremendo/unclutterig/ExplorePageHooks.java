package com.tremendo.unclutterig;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.tremendo.unclutterig.util.*;
import java.lang.reflect.*;
import org.json.*;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.*;


public class ExplorePageHooks extends UnclutterIG {

	protected static final String UNCLUTTERED_INDICATOR_TAG = "uncluttered_indicator";

	private static final String SEARCH_ICON_TAG = "search_icon";
	
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

					if (isExploreTopicHeader(param.thisObject, adapter.getClass())) {

						if (shouldHideExploreFeed()) {
							param.setResult(null);
						}

						findAndStoreExploreLoaderClassName(adapter.getClass());
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

				if (isOnExplorePage(param.thisObject, adapter.getClass())) {

					if (shouldHideExploreFeed()) {
						param.setResult(null);
					}

					View viewGroup = (ViewGroup) param.thisObject;
					ViewGroup parentView = (ViewGroup) viewGroup.getParent();

					setUnclutteredStatusIndicator(parentView);
				}
			}
		});
	}



	private static boolean isExploreTopicHeader(Object adapterContainer, Class<?> adapterClass) {

		if (getExploreTopicAdapterClassName() != null) {
			return getExploreTopicAdapterClassName().equals(adapterClass.getName());
		}

		View adapterHolderView = (View) adapterContainer;
		if (getTopicHeaderResourceId() == adapterHolderView.getId() && matchesExploreHeaderPadding(adapterHolderView.getPaddingBottom())) {
			exploreTopicAdapterClassName = adapterClass.getName();
			return true;
		}

		return false;
	}



	private static boolean matchesExploreHeaderPadding(int padding) {
		int headerPaddingResourceId = getId(getTopicHeaderPaddingResourceName(), "dimen", INSTAGRAM_PACKAGE_NAME);
		return padding == (int) getDimensionResource(headerPaddingResourceId);
	}



	private static int getTopicHeaderResourceId() {
		if (isAppStructureRevised()) {
			return getId("destination_hscroll");
		}
		return getId("topic_cluster_hscroll");
	}



	private static String getTopicHeaderPaddingResourceName() {
		if (isAppStructureRevised()) {
			return "explore_header_vertical_padding";
		}
		return "topic_cluster_header_padding";
	}



	private static float getDimensionResource(int dimensionId) {
		final Context context = AndroidAppHelper.currentApplication().getApplicationContext();
		return context.getResources().getDimension(dimensionId);
	}



	private static boolean isOnExplorePage(Object adapterContainer, Class<?> adapterClass) {

		if (getExploreFeedAdapterClassName() != null) {
			return getExploreFeedAdapterClassName().equals(adapterClass.getName());
		}

		if (!isAppStructureRevised()) {
			return isLegacyExploreFeedAdapter(adapterClass);
		}
		
		View rootView = ((View) adapterContainer).getRootView();
		if (isSearchTabButtonSelected(rootView) && isExplorePagePrimaryFeed(rootView)) {
			exploreFeedAdapterClassName = adapterClass.getName();
			return true;
		}

		return false;
	}



	private static boolean isSearchTabButtonSelected(View rootView) {
		ViewGroup tabBar = rootView.findViewById(getId("tab_bar"));
		if (tabBar!= null) {
			findAndTagSearchIcon(tabBar);
			View searchTabIcon = tabBar.findViewWithTag(SEARCH_ICON_TAG);
			return (searchTabIcon != null && hasSelectedState(searchTabIcon));
		}
		return false;
	}



	/*
	 *   Tagging 'search' icon in tab bar for convenient way of finding it from rootView
	 */
	private static void findAndTagSearchIcon(final ViewGroup viewGroup) {
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View view = viewGroup.getChildAt(i);
			if ("SEARCH".equals(String.valueOf(view.getTag()))) {
				view.findViewById(getId("tab_icon")).setTag(SEARCH_ICON_TAG);
			}
			else if (view instanceof ViewGroup) {
				findAndTagSearchIcon((ViewGroup) view);
			}
		}
	}



	private static boolean hasSelectedState(View imageView) {
		int selectedState = getId("state_selected", "attr", "android");
		for (int drawableState : imageView.getDrawableState()) {
			if (drawableState == selectedState) {
				return true;
			}
		}
		return false;
	}



	private static boolean isExplorePagePrimaryFeed(View adapterHolderView) {
		return hasPeekContainer(adapterHolderView.getRootView());
	}



	/*
	 *   For newer versions... hack-ish way of determining whether explore page is at top-most level
	 */
	private static boolean hasPeekContainer(View rootView) {
		return (rootView.findViewById(getId("peek_container")) != null);
	}



	private static boolean isLegacyExploreFeedAdapter(Class<?> AdapterObjectClass) {
		ClassToScan UnknownAdapterClass = new ClassToScan(AdapterObjectClass);

		if (UnknownAdapterClass.hasFieldType(AbsListView.OnScrollListener.class)) {
			exploreFeedAdapterClassName = UnknownAdapterClass.getName();
			return true;
		}

		return false;
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
		return getId(idName, "id", INSTAGRAM_PACKAGE_NAME);
	}



	private static int getId(String resName, String resType, String pkgName) {
		return AndroidAppHelper.currentApplication().getApplicationContext().getResources().getIdentifier(resName, resType, pkgName);
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
