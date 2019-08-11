package com.tremendo.unclutterig;

import android.os.*;
import android.view.*;
import static com.tremendo.unclutterig.UnclutterIG.*;
import com.tremendo.unclutterig.util.ResourceUtils;
import java.lang.reflect.*;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.*;


public class ExplorePageStoredHooks {


	/*
	 *   If app version was previously hooked and Explore page 'loader' class name successfully stored, then these hooks will nullify loader methods
	 *   (instead of finding and hooking into adapters to hide their items upon page load).
	 */
	protected static void doStoredExploreLoaderHooks(String storedExploreLoaderClassName, final LoadPackageParam lpparam) {
		try {
			Class<?> ExploreLoaderClass = findClass(storedExploreLoaderClassName, lpparam.classLoader);

			XC_MethodHook overrideExploreLoadHook = new XC_MethodHook() {
				@Override
				public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
					if (shouldHideExploreFeed()) {
						param.setResult(null);
					}
				}
			};

			for (Method method: ExploreLoaderClass.getDeclaredMethods()) {
				if (method.getReturnType() == void.class) {
					if (method.getParameterTypes().length > 0 && method.getParameterTypes()[0] == ExploreLoaderClass) {
						XposedBridge.hookMethod(method, overrideExploreLoadHook);
					}
				}
			}


			tryHookingMethod(storedExploreLoaderClassName, lpparam.classLoader,
			"onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
				@Override
				public void afterHookedMethod(final MethodHookParam param) throws Throwable {
					ViewGroup viewGroup = (ViewGroup) param.getResult();

					ExplorePageHooks.setUnclutteredStatusIndicator(viewGroup);

					if (shouldHideExploreFeed()) {
						View[] exploreFeedLoadingIndicators = new View[] {
							viewGroup.findViewById(ResourceUtils.getId("listview_progressbar")),	/* Newer versions of 'Explore' page, using ListView  */
							viewGroup.findViewById(ResourceUtils.getId("loading_stub")),			/* Prior versions of 'Explore' page, using ViewPager */
						};

						for (View loadingIndicatorView : exploreFeedLoadingIndicators) {
							ExplorePageHooks.hideView(loadingIndicatorView);
						}
					}

				}
			});


			tryHookingMethod(storedExploreLoaderClassName, lpparam.classLoader,
			"onResume", new XC_MethodHook() {
				@Override
				public void afterHookedMethod(final MethodHookParam param) throws Throwable {
					reloadModulePreferences();
				}
			});

		} catch (XposedHelpers.ClassNotFoundError e) {
			XposedBridge.log(String.format("Unclutter IG: Something went wrong... Failed to find previously stored 'explore page' class name (%s) in version '%s'", storedExploreLoaderClassName, getHookedVersionName()));
		}


		/*
		 *   Prevents the pull-down refresh function of the 'Explore' page while view is uncluttered.
		 *   Otherwise may be left with a constant 'loading' spinner animation due to the nullified loader methods above.
		 */
		tryHookingMethod("com.instagram.ui.widget.refresh.RefreshableListView", lpparam.classLoader,
			"setupAndEnableRefresh", View.OnClickListener.class, new XC_MethodHook() {
			@Override
			public void beforeHookedMethod(final MethodHookParam param) throws Throwable {
				View refreshableListView = (View) param.thisObject;
				View unclutteredIndicator = refreshableListView.getRootView().findViewWithTag(ExplorePageHooks.UNCLUTTERED_INDICATOR_TAG);
				
				if (shouldHideExploreFeed() && unclutteredIndicator != null && unclutteredIndicator.isShown()) {
					param.setResult(null);
				}
			}
		});

	}


}
