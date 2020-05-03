/*
 * Copyright (C) 2014-2015 The MoKee OpenSource Project
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

package com.android.systemui.ancient.carrierlabel;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.internal.util.ancient.Utils;
import com.android.internal.telephony.TelephonyIntents;

import com.android.systemui.Dependency;
import com.android.systemui.ancient.carrierlabel.SpnOverride;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.tuner.TunerService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.android.systemui.R;

public class CarrierLabel extends TextView implements DarkReceiver, TunerService.Tunable {

    private Context mContext;
    private boolean mAttached;
    private static boolean isCN;

    private int mShowCarrierLabel;
    private int mCarrierLabelFontStyle = FONT_NORMAL;
    private int mCarrierColor = 0xffffffff;
    private int mTintColor = Color.WHITE;

    private static final int FONT_HEADLINE = 0;
    private static final int FONT_BODY = 1;
    private static final int FONT_BOLD = 2;
    private static final int FONT_NORMAL = 3;
    private static final int FONT_ITALIC = 4;
    private static final int FONT_BOLD_ITALIC = 5;
    private static final int FONT_LIGHT = 6;    
    private static final int FONT_THIN = 7;    
    private static final int FONT_CONDENSED = 8;
    private static final int FONT_CONDENSED_ITALIC = 9;    
    private static final int FONT_CONDENSED_BOLD = 10;
    private static final int FONT_CONDENSED_BOLD_ITALIC = 11;
    private static final int FONT_MEDIUM = 12;
    private static final int FONT_MEDIUM_ITALIC = 13;
    private static final int FONT_ALBELREG = 14;
    private static final int FONT_ADVENTPRO = 15;
    private static final int FONT_ALIENLEAGUE = 16;
    private static final int FONT_BIGNOODLEITALIC = 17;
    private static final int FONT_BIKO = 18;
    private static final int FONT_BLERN = 19;
    private static final int FONT_CHERRYSWASH = 20;
    private static final int FONT_CODYSTAR = 21;
    private static final int FONT_GINORASANS = 22;
    private static final int FONT_GOBOLDLIGHT = 23;
    private static final int FONT_GOOGLESANS = 24;
    private static final int FONT_INKFERNO = 25;
    private static final int FONT_JURAREG = 26;
    private static final int FONT_KELLYSLAB = 27;
    private static final int FONT_METROPOLIS = 28;   
    private static final int FONT_NEONNEON = 29;
    private static final int FONT_POMPIERE = 30;
    private static final int FONT_REEMKUFI = 31;
    private static final int FONT_RIVIERA = 32;
    private static final int FINT_ROADRAGE = 33;
    private static final int FONT_SEDGWICK = 34;
    private static final int FONT_SNOWSTORM = 35;
    private static final int FONT_THEMEABLECLOCK = 36;
    private static final int FONT_UNIONFONT = 37;
    private static final int FONT_VIBUR = 38;
    private static final int FONT_VOLTAIRE = 39;

    private static final String STATUS_BAR_SHOW_CARRIER =
            "system:" + Settings.System.STATUS_BAR_SHOW_CARRIER;
    private static final String STATUS_BAR_CARRIER_FONT_STYLE =
            "system:" + Settings.System.STATUS_BAR_CARRIER_FONT_STYLE;
    private static final String STATUS_BAR_CARRIER_COLOR =
            "system:" + Settings.System.STATUS_BAR_CARRIER_COLOR;

    public CarrierLabel(Context context) {
        this(context, null);
    }

    public CarrierLabel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarrierLabel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        updateNetworkName(true, null, false, null);
        /* Force carrier label to the lockscreen. This helps us avoid
        the carrier label on the statusbar if for whatever reason
        the user changes notch overlays */
        if (Utils.hasNotch(mContext)) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_CARRIER, 1);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
            filter.addAction(Intent.ACTION_CUSTOM_CARRIER_LABEL_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
            Dependency.get(TunerService.class).addTunable(this,
                STATUS_BAR_SHOW_CARRIER,
                STATUS_BAR_CARRIER_FONT_STYLE,
                STATUS_BAR_CARRIER_COLOR);

        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
        if (mAttached) {
            Dependency.get(TunerService.class).removeTunable(this);
            mContext.unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        mTintColor = DarkIconDispatcher.getTint(area, this, tint);
        if (mCarrierColor == 0xFFFFFFFF) {
            setTextColor(mTintColor);
        } else {
            setTextColor(mCarrierColor);
        }
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case STATUS_BAR_SHOW_CARRIER:
                mShowCarrierLabel =
                        TunerService.parseInteger(newValue, 0);
                setCarrierLabel();
                break;
            case STATUS_BAR_CARRIER_FONT_STYLE:
                mCarrierLabelFontStyle =
                        TunerService.parseInteger(newValue, 12);
                setCarrierLabel();
                break;
            case STATUS_BAR_CARRIER_COLOR:
                mCarrierColor =
                        TunerService.parseInteger(newValue, 0xFFFFFFFF);
                setCarrierLabel();
                break;
            default:
                break;
        }
    }

    public void getFontStyle(int font) {
        switch (font) {
            case FONT_HEADLINE:
            default:
                setTypeface(Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_headline_medium), Typeface.NORMAL));
                break;
            case FONT_BODY:
                setTypeface(Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_body_medium), Typeface.NORMAL));
                break;
            case FONT_BOLD:
                setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                break;
            case FONT_NORMAL:
                setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                break;
            case FONT_ITALIC:
                setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                break;
            case FONT_BOLD_ITALIC:
                setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
                break;                
            case FONT_LIGHT:
                setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                break;         
            case FONT_THIN:
                setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                break;                
            case FONT_CONDENSED:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                break;
            case FONT_CONDENSED_ITALIC:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
                break;
            case FONT_CONDENSED_BOLD:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                break;
            case FONT_CONDENSED_BOLD_ITALIC:
                setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                break;            
            case FONT_MEDIUM:
                setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                break;
            case FONT_MEDIUM_ITALIC:
                setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                break;                    
            case FONT_ALBELREG:
                setTypeface(Typeface.create("abelreg", Typeface.NORMAL));
                break;
            case FONT_ADVENTPRO:
                setTypeface(Typeface.create("adventpro", Typeface.NORMAL));
                break;
            case FONT_ALIENLEAGUE:
                setTypeface(Typeface.create("alien-league", Typeface.NORMAL));
                break;
            case FONT_BIGNOODLEITALIC:
                setTypeface(Typeface.create("bignoodle-italic", Typeface.NORMAL));
                break;
            case FONT_BIKO:
                setTypeface(Typeface.create("biko", Typeface.NORMAL));
                break;
            case FONT_BLERN:
                setTypeface(Typeface.create("blern", Typeface.NORMAL));
                break;
            case FONT_CHERRYSWASH:
                setTypeface(Typeface.create("cherryswash", Typeface.NORMAL));
                break;
            case FONT_CODYSTAR:
                setTypeface(Typeface.create("codystar", Typeface.NORMAL));
                break;
            case FONT_GINORASANS:
                setTypeface(Typeface.create("ginora-sans", Typeface.NORMAL));
                break;
            case FONT_GOBOLDLIGHT:
                setTypeface(Typeface.create("gobold-light-sys", Typeface.NORMAL));
                break;
            case FONT_GOOGLESANS:
                setTypeface(Typeface.create("googlesans-sys", Typeface.NORMAL));
                break;
            case FONT_INKFERNO:
                setTypeface(Typeface.create("inkferno", Typeface.NORMAL));
                break;
            case FONT_JURAREG:
                setTypeface(Typeface.create("jura-reg", Typeface.NORMAL));
                break;
            case FONT_KELLYSLAB:
                setTypeface(Typeface.create("kellyslab", Typeface.NORMAL));
                break;
            case FONT_METROPOLIS:
                setTypeface(Typeface.create("metropolis1920", Typeface.NORMAL));
                break;
            case FONT_NEONNEON:
                setTypeface(Typeface.create("neonneon", Typeface.NORMAL));
                break;
             case FONT_POMPIERE:
                setTypeface(Typeface.create("pompiere", Typeface.NORMAL));
                break;
             case FONT_REEMKUFI:
                setTypeface(Typeface.create("reemkufi", Typeface.NORMAL));
                break;
             case FONT_RIVIERA:
                setTypeface(Typeface.create("riviera", Typeface.NORMAL));
                break;
             case FINT_ROADRAGE:
                setTypeface(Typeface.create("roadrage-sys", Typeface.NORMAL));
                break;
             case FONT_SEDGWICK:
                setTypeface(Typeface.create("sedgwick-ave", Typeface.NORMAL));
                break;
             case FONT_SNOWSTORM:
                setTypeface(Typeface.create("snowstorm-sys", Typeface.NORMAL));
                break;
             case FONT_THEMEABLECLOCK:
                setTypeface(Typeface.create("themeable-clock", Typeface.NORMAL));
                break;
             case FONT_UNIONFONT:
                setTypeface(Typeface.create("unionfont", Typeface.NORMAL));
                break;
             case FONT_VIBUR:
                setTypeface(Typeface.create("vibur", Typeface.NORMAL));
                break;
             case FONT_VOLTAIRE:
                setTypeface(Typeface.create("voltaire", Typeface.NORMAL));
                break;
        }
    }

    private void setCarrierLabel() {
        if (mShowCarrierLabel == 2 || mShowCarrierLabel == 3) {
            setVisibility(View.VISIBLE);
            getFontStyle(mCarrierLabelFontStyle);
            if (mCarrierColor == 0xFFFFFFFF) {
                setTextColor(mTintColor);
            } else {
                setTextColor(mCarrierColor);
            }
        } else {
            setVisibility(View.GONE);
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)
                    || Intent.ACTION_CUSTOM_CARRIER_LABEL_CHANGED.equals(action)) {
                        updateNetworkName(intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, true),
                        intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                        intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
                isCN = Utils.isChineseLanguage();
            }
        }
    };

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        final String str;
        final boolean plmnValid = showPlmn && !TextUtils.isEmpty(plmn);
        final boolean spnValid = showSpn && !TextUtils.isEmpty(spn);
        if (spnValid) {
            str = spn;
        } else if (plmnValid) {
            str = plmn;
        } else {
            str = "";
        }
        String customCarrierLabel = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL, UserHandle.USER_CURRENT);
        if (!TextUtils.isEmpty(customCarrierLabel)) {
            setText(customCarrierLabel);
        } else {
            setText(TextUtils.isEmpty(str) ? getOperatorName() : str);
        }
        setCarrierLabel();
    }

    private String getOperatorName() {
        String operatorName = getContext().getString(R.string.quick_settings_wifi_no_network);
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        if (isCN) {
            String operator = telephonyManager.getNetworkOperator();
            if (TextUtils.isEmpty(operator)) {
                operator = telephonyManager.getSimOperator();
            }
            SpnOverride mSpnOverride = new SpnOverride();
            operatorName = mSpnOverride.getSpn(operator);
        } else {
            operatorName = telephonyManager.getNetworkOperatorName();
        }
        if (TextUtils.isEmpty(operatorName)) {
            operatorName = telephonyManager.getSimOperatorName();
        }
        return operatorName;
    }
}
