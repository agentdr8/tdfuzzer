package com.dr8.xposed.tdfuzzer;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;
import static de.robv.android.xposed.XposedHelpers.setStaticByteField;

import java.util.Hashtable;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Mod implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {

	private static final String target = "com.nitrodesk.droid20.nitroid";
	private static final String TAG = "TDFuzzer";
    private static boolean DEBUG = false;
	private static XSharedPreferences pref;
	public static String os = "";

    private static void log(String msg) {
	        XposedBridge.log(TAG + ": " + msg);
	}
    
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		pref = new XSharedPreferences("com.dr8.xposed.tdfuzzer", "com.dr8.xposed.tdfuzzer_preferences");
		pref.reload();
		DEBUG = pref.getBoolean("debug", false);
		
		if (!lpparam.packageName.equals(target)) {
			return;
		} else {
			if (DEBUG) log("Hooked TD package, looking for classes and methods");
			findAndHookMethod("com.nitrodesk.activesync.ActiveSyncRequestBase", lpparam.classLoader, "GetDeviceType", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam mparam) throws Throwable {
					String dt = (String) mparam.thisObject;
					dt = pref.getString("dev_type", "iPhone");
					if (DEBUG) log("Found devicetype method, changing result to " + dt);
					mparam.setResult(dt);
				}
			});	

			findAndHookMethod("com.nitrodesk.activesync.ActiveSyncRequestBase", lpparam.classLoader, "getRequestHeaders", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam mparam) throws Throwable {
					if (DEBUG) log("Found getRequestHeaders method, changing UA and TD-Info headers");
					@SuppressWarnings("unchecked")
					Hashtable<String, String> ht = (Hashtable<String, String>) mparam.getResult();
					String myua = (String) ht.get("User-Agent");
					if (DEBUG) log("User-Agent was " + myua);
					String newua = pref.getString("dev_user_agent", "Apple-iPhone6C1/1104.201");
					ht.put("User-Agent", newua);
					if (DEBUG) log("User-Agent now " + newua);
					if (DEBUG) log("Removing TD-Info header");
					ht.remove("TD-Info");
					mparam.setResult(ht);
				}
			});

			final Class<?> asreqbase = XposedHelpers.findClass("com.nitrodesk.activesync.ActiveSyncRequestBase", lpparam.classLoader);
			final Class<?> asreqprov = XposedHelpers.findClass("com.nitrodesk.activesync.ASRequestProvision", lpparam.classLoader);

			if (DEBUG) log("setting mProtocolVersion to -115");
			setStaticByteField(asreqbase, "mProtocolVersion", (byte)-115);
			
			if (DEBUG) log("setting bWBXMLMode true");
			setStaticBooleanField(asreqprov, "bWBXMLMode", true);

			findAndHookMethod("com.nitrodesk.activesync.ASRequestProvision141", lpparam.classLoader, "writeDeviceInfo", "com.nitrodesk.wbxml.WBXMLSerializer", new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam mparam) throws Throwable {
					if (DEBUG) log("Found writeDeviceInfo method, changing results");
					
					Object wb = mparam.args[0];
					try {
						callMethod(wb, "setCodePage", (byte)18);
						callMethod(wb, "startTag", 22, true);
						callMethod(wb, "startTag", 8, true);
						callMethod(wb, "startTag", 23, true);
						callMethod(wb, "text", pref.getString("dev_model_name", "iPhone6C1"));
						callMethod(wb, "endTag");
						callMethod(wb, "startTag", 26, true);
						if (pref.getString("dev_type", "iPhone").equals("iPhone")) {
							os = "iOS ";
						} else if (pref.getString("dev_type", "iPhone").equals("Android")) {
							os = "Android ";
						} else {
							os = "Windows Phone ";
						}
						callMethod(wb, "text", os + pref.getString("dev_os_ver", "7.1.1 11D201"));
						callMethod(wb, "endTag");
						callMethod(wb, "startTag", 27, true);
						callMethod(wb, "text", pref.getString("dev_lang", "en"));
						callMethod(wb, "endTag");
						callMethod(wb, "startTag", 25, true);
						callMethod(wb, "text", pref.getString("dev_friendly_name", "iPhone 5s"));
						callMethod(wb, "endTag");
						callMethod(wb, "endTag");
						callMethod(wb, "endTag");
						callMethod(wb, "setCodePage", (byte)14);
					} catch (Throwable t) {
						if (DEBUG) log("writeDeviceInfoException " + t);
					}
					mparam.setResult(wb);
					return wb;
				}
			});
		} 
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam)
			throws Throwable {

	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
	}

}
