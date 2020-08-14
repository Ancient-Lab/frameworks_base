/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.ancient;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.input.InputManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.text.format.Time;
import android.view.InputDevice;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.internal.R;
import com.android.internal.statusbar.IStatusBarService;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import java.util.Locale;

public class Utils {
    
    private static OverlayManager mOverlayService;

    private static final String TAG = "Utils";

    public static final String INTENT_SCREENSHOT = "action_handler_screenshot";
    public static final String INTENT_REGION_SCREENSHOT = "action_handler_region_screenshot";

     // Switch themes
    private static final String[] SWITCH_THEMES = {
        "com.android.system.switch.stock", // 0
        "com.android.system.switch.oneplus", // 1
        "com.android.system.switch.narrow", // 2
        "com.android.system.switch.contained", // 3
        "com.android.system.switch.telegram", // 4
        "com.android.system.switch.square", // 5
    };

    private static final String[] QS_TILE_THEMES = {
        "com.android.systemui.qstile.default", // 0
        "com.android.systemui.qstile.circletrim", // 1
        "com.android.systemui.qstile.dualtonecircletrim", // 2
        "com.android.systemui.qstile.squircletrim", // 3
        "com.android.systemui.qstile.wavey", // 4
        "com.android.systemui.qstile.pokesign", // 5
        "com.android.systemui.qstile.ninja", // 6
        "com.android.systemui.qstile.dottedcircle", // 7
        "com.android.systemui.qstile.attemptmountain", // 8
        "com.android.systemui.qstile.squaremedo", // 9
        "com.android.systemui.qstile.inkdrop", // 10
        "com.android.systemui.qstile.cookie", // 11
        "com.android.systemui.qstile.circleoutline", //12
        "com.android.systemui.qstile.neonlike", // 13
        "com.android.systemui.qstile.oos", // 14
        "com.android.systemui.qstile.triangles", // 15
        "com.android.systemui.qstile.divided", // 16
        "com.android.systemui.qstile.cosmos", // 17
        "com.android.systemui.qstile.squircle", // 18
        "com.android.systemui.qstile.teardrop", // 19
        "com.android.systemui.qstile.anci1", // 20
        "com.android.systemui.qstile.anci2", // 21
        "com.android.systemui.qstile.anci3", // 22
        "com.android.systemui.qstile.anci4", // 23
        "com.android.systemui.qstile.anci5", // 24
        "com.android.systemui.qstile.anci6", // 25
        "com.android.systemui.qstile.anci7", // 26
        "com.android.systemui.qstile.anci8", // 27
        "com.android.systemui.qstile.anci9", // 28
        "com.android.systemui.qstile.anci10", // 29
        "com.android.systemui.qstile.anci11", // 30
        "com.android.systemui.qstile.anci12", // 31
        "com.android.systemui.qstile.anci13", // 32
        "com.android.systemui.qstile.anci14", // 33
        "com.android.systemui.qstile.anci15", // 34
        "com.android.systemui.qstile.anci16", // 35
    };

    private static final String[] QS_CLOCK_THEMES = {
        "com.android.systemui.qsclock.default", // 0
        "com.android.systemui.qsclock.left", // 1
        "com.android.systemui.qsclock.right", // 2
        "com.android.systemui.qsclock.anci", // 3
        "com.android.systemui.qsclock.anci0", // 4
        "com.android.systemui.qsclock.anci1", // 5
        "com.android.systemui.qsclock.anci2", // 6
        "com.android.systemui.qsclock.anci3", // 7
        "com.android.systemui.qsclock.anci4", // 8
        "com.android.systemui.qsclock.anci5", // 9
        "com.android.systemui.qsclock.anci6", // 10
        "com.android.systemui.qsclock.anci7", // 11
        "com.android.systemui.qsclock.anci8", // 12
        "com.android.systemui.qsclock.anci9", // 13
        "com.android.systemui.qsclock.anci10", // 14
        "com.android.systemui.qsclock.anci11", // 15
        "com.android.systemui.qsclock.anci12", // 16
        "com.android.systemui.qsclock.anci13", // 17
        "com.android.systemui.qsclock.anci14", // 18
        "com.android.systemui.qsclock.anci15", // 19
        "com.android.systemui.qsclock.anci16", // 20
        "com.android.systemui.qsclock.anci17", // 21
        "com.android.systemui.qsclock.anci18", // 22
        "com.android.systemui.qsclock.anci19", // 23
        "com.android.systemui.qsclock.anci20", // 24
        "com.android.systemui.qsclock.anci21", // 25
    };

    private static final String[] QS_ANALOG_THEMES = {
        "com.android.analog.style.default", // 0
        "com.android.analog.style.anci1", // 1
        "com.android.analog.style.anci2", // 2
        "com.android.analog.style.anci3", // 3
        "com.android.analog.style.anci4", // 4
        "com.android.analog.style.anci5", // 5
        "com.android.analog.style.anci6", // 6
        "com.android.analog.style.anci7", // 7
        "com.android.analog.style.anci8", // 8
        "com.android.analog.style.anci9", // 9
        "com.android.analog.style.anci10", // 10
        "com.android.analog.style.anci11", // 11
        "com.android.analog.style.anci12", // 12
        "com.android.analog.style.anci13", // 13
        "com.android.analog.style.anci14", // 14
        "com.android.analog.style.anci15", // 15
    };

    private static final String[] QS_SETTING_THEMES = {
        "com.android.setting.style.default", // 0
        "com.android.setting.style.left", // 1
        "com.android.setting.style.center", // 2
        "com.android.setting.style.anci", // 3
        "com.android.setting.style.anci0", // 4
        "com.android.setting.style.anci1", // 5
        "com.android.setting.style.anci2", // 6
        "com.android.setting.style.anci3", // 7
        "com.android.setting.style.anci4", // 8
        "com.android.setting.style.anci5", // 9
        "com.android.setting.style.anci6", // 10
        "com.android.setting.style.anci7", // 11
        "com.android.setting.style.anci8", // 12
        "com.android.setting.style.anci9", // 13
        "com.android.setting.style.anci10", // 14
        "com.android.setting.style.anci11", // 15
        "com.android.setting.style.anci12", // 16
        "com.android.setting.style.anci13", // 17
        "com.android.setting.style.anci14", // 18
        "com.android.setting.style.anci15", // 19
        "com.android.setting.style.anci16", // 20
        "com.android.setting.style.anci17", // 21
        "com.android.setting.style.anci18", // 22
        "com.android.setting.style.anci19", // 23
        "com.android.setting.style.anci20", // 24
        "com.android.setting.style.anci21", // 25
    };

    private static final String[] QS_BARHEIGHT_THEMES = {
        "com.android.system.bar.default", // 0
        "com.android.system.bar.one", // 1
        "com.android.system.bar.two", // 2
        "com.android.system.bar.three", // 3
    };

    private static final String[] QS_MERGEBG_THEMES = {
        "com.android.systemui.mergebg.style.default", // 0
        "com.android.systemui.mergebg.style.anci1", // 1
        "com.android.systemui.mergebg.style.anci2", // 2
        "com.android.systemui.mergebg.style.anci3", // 3
        "com.android.systemui.mergebg.style.anci4", // 4
        "com.android.systemui.mergebg.style.anci5", // 5
        "com.android.systemui.mergebg.style.anci6", // 6
        "com.android.systemui.mergebg.style.anci7", // 7
        "com.android.systemui.mergebg.style.anci8", // 8
        "com.android.systemui.mergebg.style.anci9", // 9
        "com.android.systemui.mergebg.style.anci10", // 10
    };

    private static final String[] QS_NAVBAR_THEMES = {
        "com.android.systemui.navbar.style.default", // 0
        "com.android.systemui.navbar.style.anci1", // 1
        "com.android.systemui.navbar.style.anci2", // 2
        "com.android.systemui.navbar.style.anci3", // 3
        "com.android.systemui.navbar.style.anci4", // 4
        "com.android.systemui.navbar.style.anci5", // 5
    };

    private static IStatusBarService mStatusBarService = null;
    private static IStatusBarService getStatusBarService() {
        synchronized (Utils.class) {
            if (mStatusBarService == null) {
                mStatusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService("statusbar"));
            }
            return mStatusBarService;
        }
    }

    // Check to see if device is WiFi only
    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    // Check if device is connected to Wi-Fi
    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi.isConnected();
    }

    // Check to see if a package is installed
    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    // Check if device is connected to the internet
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return wifi.isConnected() || mobile.isConnected();
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        return isPackageInstalled(context, pkg, true);
    }

    // Check to see if device supports the Fingerprint scanner
    public static boolean hasFingerprintSupport(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        return context.getApplicationContext().checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                (fingerprintManager != null && fingerprintManager.isHardwareDetected());
    }

    // Check to see if device not only supports the Fingerprint scanner but also if is enrolled
    public static boolean hasFingerprintEnrolled(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        return context.getApplicationContext().checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                (fingerprintManager != null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints());
    }

    // Check to see if device has a camera
    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // Check to see if device supports NFC
    public static boolean hasNFC(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    // Check to see if device supports Wifi
    public static boolean hasWiFi(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    // Check to see if device supports Bluetooth
    public static boolean hasBluetooth(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    // Returns today's passed time in Millisecond
    public static long getTodayMillis() {
        final long passedMillis;
        Time time = new Time();
        time.set(System.currentTimeMillis());
        passedMillis = ((time.hour * 60 * 60) + (time.minute * 60) + time.second) * 1000;
        return passedMillis;
    }

    // Check to see if device supports an alterative ambient display package
    public static boolean hasAltAmbientDisplay(Context context) {
        return context.getResources().getBoolean(com.android.internal.R.bool.config_alt_ambient_display);
    }

    // Check to see if device supports A/B (seamless) system updates
    public static boolean isABdevice(Context context) {
        return SystemProperties.getBoolean("ro.build.ab_update", false);
    }

    // Check for Chinese language
    public static boolean isChineseLanguage() {
       return Resources.getSystem().getConfiguration().locale.getLanguage().startsWith(
               Locale.CHINESE.getLanguage());
    }

    // Method to turn off the screen
    public static void switchScreenOff(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm!= null) {
            pm.goToSleep(SystemClock.uptimeMillis());
        }
    }

    public static boolean deviceHasFlashlight(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public static void toggleCameraFlash() {
        IStatusBarService service = getStatusBarService();
        if (service != null) {
            try {
                service.toggleCameraFlash();
            } catch (RemoteException e) {
                // do nothing.
            }
        }
    }

    // Method to detect if device has DASH support.
    public static boolean isDashCharger() {
        try {
            FileReader file = new FileReader("/sys/class/power_supply/battery/fastchg_status");
            BufferedReader br = new BufferedReader(file);
            String state = br.readLine();
            br.close();
            file.close();
            return "1".equals(state);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return false;
    }

    public static void sendKeycode(int keycode) {
        long when = SystemClock.uptimeMillis();
        final KeyEvent evDown = new KeyEvent(when, when, KeyEvent.ACTION_DOWN, keycode, 0,
                0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        final KeyEvent evUp = KeyEvent.changeAction(evDown, KeyEvent.ACTION_UP);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                InputManager.getInstance().injectInputEvent(evDown,
                        InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputManager.getInstance().injectInputEvent(evUp,
                        InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }
        }, 20);
    }

    public static void takeScreenshot(boolean full) {
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            wm.sendCustomAction(new Intent(full? INTENT_SCREENSHOT : INTENT_REGION_SCREENSHOT));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPackageAvailable(Context context, String packageName) {
        final PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            int enabled = pm.getApplicationEnabledSetting(packageName);
            return enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                enabled != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    // Check if device has a notch
    public static boolean hasNotch(Context context) {
        int result = 0;
        int resid;
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        resid = context.getResources().getIdentifier("config_fillMainBuiltInDisplayCutout",
                "bool", "android");
        if (resid > 0) {
            return context.getResources().getBoolean(resid);
        }
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = 24 * (metrics.densityDpi / 160f);
        return result > Math.round(px);
    }

    public static void updateSwitchStyle(IOverlayManager om, int userId, int switchStyle) {
        if (switchStyle == 0) {
            stockSwitchStyle(om, userId);
        } else {
            try {
                om.setEnabled(SWITCH_THEMES[switchStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change switch theme", e);
            }
        }
    }

    public static void stockSwitchStyle(IOverlayManager om, int userId) {
        for (int i = 0; i < SWITCH_THEMES.length; i++) {
            String switchtheme = SWITCH_THEMES[i];
            try {
                om.setEnabled(switchtheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs tile style to user selected.
    public static void updateTileStyle(IOverlayManager om, int userId, int qsTileStyle) {
        if (qsTileStyle == 0) {
            stockTileStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_TILE_THEMES[qsTileStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs tile icon", e);
            }
        }
    }

    // Switches qs tile style back to stock.
    public static void stockTileStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_TILE_THEMES.length; i++) {
            String qstiletheme = QS_TILE_THEMES[i];
            try {
                om.setEnabled(qstiletheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs clock style to user selected.
    public static void updateClockStyle(IOverlayManager om, int userId, int qsClockStyle) {
        if (qsClockStyle == 0) {
            stockClockStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_CLOCK_THEMES[qsClockStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs clock icon", e);
            }
        }
    }

    // Switches qs clock style back to stock.
    public static void stockClockStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_CLOCK_THEMES.length; i++) {
            String qsclocktheme = QS_CLOCK_THEMES[i];
            try {
                om.setEnabled(qsclocktheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs analog style to user selected.
    public static void updateAnalogStyle(IOverlayManager om, int userId, int qsAnalogStyle) {
        if (qsAnalogStyle == 0) {
            stockAnalogStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_ANALOG_THEMES[qsAnalogStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs analog icon", e);
            }
        }
    }

    // Switches qs analog style back to stock.
    public static void stockAnalogStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_ANALOG_THEMES.length; i++) {
            String qsanalogtheme = QS_ANALOG_THEMES[i];
            try {
                om.setEnabled(qsanalogtheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs setting style to user selected.
    public static void updateSettingStyle(IOverlayManager om, int userId, int qsSettingStyle) {
        if (qsSettingStyle == 0) {
            stockSettingStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_SETTING_THEMES[qsSettingStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs setting icon", e);
            }
        }
    }

    // Switches qs setting style back to stock.
    public static void stockSettingStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_SETTING_THEMES.length; i++) {
            String qssettingtheme = QS_SETTING_THEMES[i];
            try {
                om.setEnabled(qssettingtheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches barheight style to user selected.
    public static void updateBarheightStyle(IOverlayManager om, int userId, int qsBarheightStyle) {
        if (qsBarheightStyle == 0) {
            stockSettingStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_BARHEIGHT_THEMES[qsBarheightStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change  statusbar height", e);
            }
        }
    }

    // Switches barheight style back to stock.
    public static void stockBarheightStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_BARHEIGHT_THEMES.length; i++) {
            String qsbarheighttheme = QS_BARHEIGHT_THEMES[i];
            try {
                om.setEnabled(qsbarheighttheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs bgmerge style to user selected.
    public static void updateMergebgStyle(IOverlayManager om, int userId, int qsMergebgStyle) {
        if (qsMergebgStyle == 0) {
            stockMergebgStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_MERGEBG_THEMES[qsMergebgStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs bg merge", e);
            }
        }
    }

    // Switches qs bgmerge style back to stock.
    public static void stockMergebgStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_MERGEBG_THEMES.length; i++) {
            String qsmergebgtheme = QS_MERGEBG_THEMES[i];
            try {
                om.setEnabled(qsmergebgtheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches navbar style to user selected.
    public static void updateNavbarStyle(IOverlayManager om, int userId, int qsNavbarStyle) {
        if (qsNavbarStyle == 0) {
            stockNavbarStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_NAVBAR_THEMES[qsNavbarStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change navbar icon", e);
            }
        }
    }

    // Switches navbar style back to stock.
    public static void stockNavbarStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 0; i < QS_NAVBAR_THEMES.length; i++) {
            String qsnavbartheme = QS_NAVBAR_THEMES[i];
            try {
                om.setEnabled(qsnavbartheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean deviceHasCompass(Context ctx) {
        SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        return sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                && sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
    }
    
    // Method to detect whether an overlay is enabled or not
    public static boolean isThemeEnabled(String packageName) {
        mOverlayService = new OverlayManager();
        try {
            List<OverlayInfo> infos = mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (infos.get(i).packageName.equals(packageName)) {
                    return infos.get(i).isEnabled();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class OverlayManager {
        private final IOverlayManager mService;

        public OverlayManager() {
            mService = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
        }

        public void setEnabled(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabled(pkg, enabled, userId);
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String target, int userId)
                throws RemoteException {
            return mService.getOverlayInfosForTarget(target, userId);
        }
    }
}
