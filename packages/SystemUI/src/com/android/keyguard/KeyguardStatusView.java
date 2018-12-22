/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2020 Ancient OS
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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings;
import android.provider.Settings.System;
import android.provider.Settings.Secure;

import androidx.core.graphics.ColorUtils;

import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.omni.CurrentWeatherView;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.tuner.TunerService;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.TimeZone;

public class KeyguardStatusView extends GridLayout implements
        ConfigurationController.ConfigurationListener,
        TunerService.Tunable {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";
    private static final int MARQUEE_DELAY_MS = 2000;

    private final LockPatternUtils mLockPatternUtils;
    private final IActivityManager mIActivityManager;

    private LinearLayout mStatusViewContainer;
    private TextView mLogoutView;
    private KeyguardClockSwitch mClockView;
    private TextView mOwnerInfo;
    private KeyguardSliceView mKeyguardSlice;
    private View mNotificationIcons;
    private Runnable mPendingMarqueeStart;
    private Handler mHandler;

    private boolean mPulsing;
    private float mDarkAmount = 0;
    private int mTextColor;
    private CurrentWeatherView mWeatherView;
    private boolean mShowWeather;
    private boolean mOmniStyle;

    /**
     * Bottom margin that defines the margin between bottom of smart space and top of notification
     * icons on AOD.
     */
    private int mIconTopMargin;
    private int mIconTopMarginWithHeader;
    private boolean mShowingHeader;

    private int mClockSelection;
    private int mLockClockFontStyle;
    private int mDateSelection;

    // Date styles paddings
    private int mDateVerPadding;
    private int mDateHorPadding;
    private int mLockClockFontSize;
    private int mLockDateFontSize;
    private int mOwnerInfoSize;

    private int mOwnerInfoFontStyle = FONT_HEADLINE;
    private static final int FONT_HEADLINE = 0;
    private static final int FONT_BODY = 1;
    private static final int FONT_NORMAL = 3;
    private static final int FONT_BOLD = 2;  
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
    private static final int FONT_SEDGWICKAVE = 34;
    private static final int FONT_SNOWSTORM = 35;
    private static final int FONT_THEMEABLECLOCK = 36;
    private static final int FONT_UNIONFONT = 37;
    private static final int FONT_VIBUR = 38;
    private static final int FONT_VOLTAIRE = 39;

    private static final String LOCK_CLOCK_FONT_STYLE =
            "system:" + Settings.System.LOCK_CLOCK_FONT_STYLE;
    private static final String LOCKSCREEN_DATE_SELECTION =
            "system:" + Settings.System.LOCKSCREEN_DATE_SELECTION;
    private static final String LOCK_CLOCK_FONT_SIZE =
            "system:" + Settings.System.LOCK_CLOCK_FONT_SIZE;
    private static final String LOCK_DATE_FONT_SIZE =
            "system:" + Settings.System.LOCK_DATE_FONT_SIZE;
    private static final String LOCKOWNER_FONT_SIZE =
            "system:" + Settings.System.LOCKOWNER_FONT_SIZE;
    private static final String LOCK_OWNERINFO_FONTS =
            "system:" + Settings.System.LOCK_OWNERINFO_FONTS;

    private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            refreshTime();
            refreshLockFont();
            refreshLockDateFont();
        }

        @Override
        public void onTimeZoneChanged(TimeZone timeZone) {
            updateTimeZone(timeZone);
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refreshTime();
                updateOwnerInfo();
                updateLogoutView();
                refreshLockDateFont();
                updateSettings();
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refreshFormat();
            updateOwnerInfo();
            updateLogoutView();
            refreshLockDateFont();
            updateSettings();
        }

        @Override
        public void onLogoutEnabledChanged() {
            updateLogoutView();
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mIActivityManager = ActivityManager.getService();
        mLockPatternUtils = new LockPatternUtils(getContext());
        mHandler = new Handler(Looper.myLooper());
        final TunerService tunerService = Dependency.get(TunerService.class);
        tunerService.addTunable(this, LOCKSCREEN_DATE_SELECTION);
	tunerService.addTunable(this, LOCK_CLOCK_FONT_SIZE);
	tunerService.addTunable(this, LOCK_DATE_FONT_SIZE);
	tunerService.addTunable(this, LOCKOWNER_FONT_SIZE);
        tunerService.addTunable(this, LOCK_OWNERINFO_FONTS);
        onDensityOrFontScaleChanged();
    }

    /**
     * If we're presenting a custom clock of just the default one.
     */
    public boolean hasCustomClock() {
        return mClockView.hasCustomClock();
    }

    private int getLockClockFont() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_CLOCK_FONT_STYLE, 23);
    }

    /**
     * Set whether or not the lock screen is showing notifications.
     */
    public void setHasVisibleNotifications(boolean hasVisibleNotifications) {
        mClockView.setHasVisibleNotifications(hasVisibleNotifications);
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, "Schedule setEnableMarquee: " + (enabled ? "Enable" : "Disable"));
        if (enabled) {
            if (mPendingMarqueeStart == null) {
                mPendingMarqueeStart = () -> {
                    setEnableMarqueeImpl(true);
                    mPendingMarqueeStart = null;
                };
                mHandler.postDelayed(mPendingMarqueeStart, MARQUEE_DELAY_MS);
            }
        } else {
            if (mPendingMarqueeStart != null) {
                mHandler.removeCallbacks(mPendingMarqueeStart);
                mPendingMarqueeStart = null;
            }
            setEnableMarqueeImpl(false);
        }
    }

    private void setEnableMarqueeImpl(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mStatusViewContainer = findViewById(R.id.status_view_container);
        mLogoutView = findViewById(R.id.logout);
        mNotificationIcons = findViewById(R.id.clock_notification_icon_container);
        if (mLogoutView != null) {
            mLogoutView.setOnClickListener(this::onLogoutClicked);
        }

        mClockView = findViewById(R.id.keyguard_clock_container);
        mClockView.setShowCurrentUserTime(true);
        if (KeyguardClockAccessibilityDelegate.isNeeded(mContext)) {
            mClockView.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(mContext));
        }
        mOwnerInfo = findViewById(R.id.owner_info);
        mKeyguardSlice = findViewById(R.id.keyguard_status_area);

        mWeatherView = (CurrentWeatherView) findViewById(R.id.weather_container);
        updateSettings();

        mTextColor = mClockView.getCurrentTextColor();

        refreshLockDateFont();
        mKeyguardSlice.setContentChangeListener(this::onSliceContentChanged);
        onSliceContentChanged();

        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refreshFormat();
        updateOwnerInfo();
        updateLogoutView();
        updateDark();
        updateSettings();
    }

    /**
     * Moves clock, adjusting margins when slice content changes.
     */
    private void onSliceContentChanged() {
        final boolean hasHeader = mKeyguardSlice.hasHeader();
        mClockView.setKeyguardShowingHeader(hasHeader);
        if (mShowingHeader == hasHeader) {
            return;
        }
        mShowingHeader = hasHeader;
        if (mNotificationIcons != null) {
            // Update top margin since header has appeared/disappeared.
            MarginLayoutParams params = (MarginLayoutParams) mNotificationIcons.getLayoutParams();
            params.setMargins(params.leftMargin,
                    hasHeader ? mIconTopMarginWithHeader : mIconTopMargin,
                    params.rightMargin,
                    params.bottomMargin);
            mNotificationIcons.setLayoutParams(params);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        layoutOwnerInfo();
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        if (mClockView != null) {
            setFontSize(mClockView, mLockClockFontSize);
            setFontStyle(mClockView, mLockClockFontStyle);
            refreshLockFont();
            refreshLockDateFont();
        }
        if (mOwnerInfo != null) {
            setOwnerInfoSize(mOwnerInfoSize);
            setOwnerInfoFontStyle(mOwnerInfoFontStyle);
        }

        if (mWeatherView != null) {
            mWeatherView.onDensityOrFontScaleChanged();
        }

	if (mKeyguardSlice != null) {
            mKeyguardSlice.setDateSize(mLockDateFontSize);
        }   
	    
        switch (mDateSelection) {
            case 0: // default
            default:
                try {
                    mKeyguardSlice.setViewBackgroundResource(0);
                    mDateVerPadding = 0;
                    mDateHorPadding = 0;
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
		    mKeyguardSlice.setViewsTextStyles(0.05f, false);
                } catch (Exception e) {
                }
                break;
            case 1: // semi-transparent box
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.date_box_str_border));
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_box_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_box_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
		    mKeyguardSlice.setViewsTextStyles(0.05f, false);
                } catch (Exception e) {
                }
                break;
            case 2: // semi-transparent box (round)
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.date_str_border));
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_box_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_box_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
		    mKeyguardSlice.setViewsTextStyles(0.05f, false);
                } catch (Exception e) {
                }
                break;
	    case 3: // Q-Now Playing background
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.ambient_indication_pill_background));
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.q_nowplay_pill_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.q_nowplay_pill_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
		    mKeyguardSlice.setViewsTextStyles(0.05f, false);
                } catch (Exception e) {
                }
                break;
            case 4: // accent box
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.date_str_accent));
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
                    mKeyguardSlice.setViewsTextStyles(0.15f, true);
                } catch (Exception e) {
                }
                break;
            case 5: // accent box transparent
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.date_str_accent), 160);
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
                    mKeyguardSlice.setViewsTextStyles(0.15f, true);
                } catch (Exception e) {
                }
                break;
            case 6: // gradient box
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.date_str_gradient));
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
                    mKeyguardSlice.setViewsTextStyles(0.15f, true);
                } catch (Exception e) {
                }
                break;
            case 7: // Dark Accent border
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.date_str_borderacc));
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
                    mKeyguardSlice.setViewsTextStyles(0.08f, true);
                } catch (Exception e) {
                }
                break;
            case 8: // Dark Gradient border
                try {
                    mKeyguardSlice.setViewBackground(getResources().getDrawable(R.drawable.date_str_bordergrad));
                    mDateHorPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_hor),getResources().getDisplayMetrics()));
                    mDateVerPadding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.widget_date_accent_box_padding_ver),getResources().getDisplayMetrics()));
                    mKeyguardSlice.setViewPadding(mDateHorPadding,mDateVerPadding,mDateHorPadding,mDateVerPadding);
                    mKeyguardSlice.setViewsTextStyles(0.08f, true);
                } catch (Exception e) {
                }
                break;
        }

        loadBottomMargin();
    }

    public void dozeTimeTick() {
        refreshTime();
        mKeyguardSlice.refresh();
    }

    private void refreshTime() {
        mClockView.refresh();

        if (mClockSelection == 2) {
            mClockView.setFormat12Hour(Patterns.clockView12);
            mClockView.setFormat24Hour(Patterns.clockView24);
        } else if (mClockSelection == 3) {
            mClockView.setFormat12Hour(Html.fromHtml("<strong>h</strong>:mm"));
            mClockView.setFormat24Hour(Html.fromHtml("<strong>kk</strong>:mm"));
        } else if (mClockSelection == 4) {
	        mClockView.setFormat12Hour(Html.fromHtml("<strong>h:mm</strong>"));
            mClockView.setFormat24Hour(Html.fromHtml("<strong>kk:mm</strong>"));
        } else if (mClockSelection == 5) {
            mClockView.setFormat12Hour(Html.fromHtml("<font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">h:mm</font>"));
            mClockView.setFormat24Hour(Html.fromHtml("<font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">kk:m</font>"));
        } else if (mClockSelection == 6) {
            mClockView.setFormat12Hour(Html.fromHtml("<font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">h</font>:mm"));
            mClockView.setFormat24Hour(Html.fromHtml("<font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">kk</font>:mm"));
        } else if (mClockSelection == 7) {
            mClockView.setFormat12Hour(Html.fromHtml("h<font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">:mm</font>"));
            mClockView.setFormat24Hour(Html.fromHtml("kk<font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">:mm</font>"));
        } else if (mClockSelection == 8) {
            mClockView.setFormat12Hour("hh\nmm");
            mClockView.setFormat24Hour("kk\nmm");
        } else if (mClockSelection == 10) {
            mClockView.setFormat12Hour(Html.fromHtml("hh<br><font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">mm</font>"));
            mClockView.setFormat24Hour(Html.fromHtml("kk<br><font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">mm</font>"));
        } else if (mClockSelection == 11) {
            mClockView.setFormat12Hour(Html.fromHtml("<font color='#454545'>hh</font><br><font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">mm</font>"));
            mClockView.setFormat24Hour(Html.fromHtml("<font color='#454545'>kk</font><br><font color=" + getResources().getColor(R.color.accent_tint_clock_selector) + ">mm</font>"));
        } else {
            mClockView.setFormat12Hour(Html.fromHtml("<strong>hh</strong><br>mm"));
            mClockView.setFormat24Hour(Html.fromHtml("<strong>kk</strong><br>mm"));
        }
    }

    private void updateTimeZone(TimeZone timeZone) {
        mClockView.onTimeZoneChanged(timeZone);
    }

    private int getLockDateFont() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_DATE_FONTS, 1);
    }

    private void refreshFormat() {
        Patterns.update(mContext);
        mClockView.setFormat12Hour(Patterns.clockView12);
        mClockView.setFormat24Hour(Patterns.clockView24);
    }

    public int getLogoutButtonHeight() {
        if (mLogoutView == null) {
            return 0;
        }
        return mLogoutView.getVisibility() == VISIBLE ? mLogoutView.getHeight() : 0;
    }

    public float getClockTextSize() {
        return mClockView.getTextSize();
    }

    private void refreshLockDateFont() {
        mKeyguardSlice.setTextDateFont(getDateFont(getLockDateFont()));
        mClockView.setTextDateFont(getDateFont(getLockDateFont()));
    }

    private Typeface getDateFont(int userSelection) {
        Typeface tf;
        switch (userSelection) {
            case 0:
            default:
                return Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_headline), Typeface.NORMAL);
            case 1:
                return Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_body), Typeface.NORMAL);
            case 2:
                return Typeface.create("sans-serif", Typeface.BOLD);
            case 3:
                return Typeface.create("sans-serif", Typeface.NORMAL);
            case 4:
                return Typeface.create("sans-serif", Typeface.ITALIC);
            case 5:
                return Typeface.create("sans-serif", Typeface.BOLD_ITALIC);
            case 6:
                return Typeface.create("sans-serif-light", Typeface.NORMAL);
            case 7:
                return Typeface.create("sans-serif-thin", Typeface.NORMAL);
            case 8:
                return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            case 9:
                return Typeface.create("sans-serif-condensed", Typeface.ITALIC);
            case 10:
                return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            case 11:
                return Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);
            case 12:
                return Typeface.create("sans-serif-medium", Typeface.NORMAL);
            case 13:
                return Typeface.create("sans-serif-medium", Typeface.ITALIC);
            case 14:
                return Typeface.create("abelreg", Typeface.NORMAL);
            case 15:
                return Typeface.create("adamcg-pro", Typeface.NORMAL);
            case 16:
                return Typeface.create("adventpro", Typeface.NORMAL);
            case 17:
                return Typeface.create("alien-league", Typeface.NORMAL);
            case 18:
                return Typeface.create("archivonar", Typeface.NORMAL);
            case 19:
                return Typeface.create("autourone", Typeface.NORMAL);
            case 20:
                return Typeface.create("badscript", Typeface.NORMAL);
            case 21:
                return Typeface.create("bignoodle-regular", Typeface.NORMAL);
            case 22:
                return Typeface.create("biko", Typeface.NORMAL);
            case 23:
                return Typeface.create("cherryswash", Typeface.NORMAL);
            case 24:
                return Typeface.create("ginora-sans", Typeface.NORMAL);
            case 25:
                return Typeface.create("googlesans-sys", Typeface.NORMAL);
            case 26:
                return Typeface.create("ibmplex-mono", Typeface.NORMAL);
            case 27:
                return Typeface.create("inkferno", Typeface.NORMAL);
            case 28:
                return Typeface.create("instruction", Typeface.NORMAL);
            case 29:
                return Typeface.create("jack-lane", Typeface.NORMAL);
            case 30:
                return Typeface.create("kellyslab", Typeface.NORMAL);
            case 31:
                return Typeface.create("monad", Typeface.NORMAL);
            case 32:
                return Typeface.create("noir", Typeface.NORMAL);
            case 33:
                return Typeface.create("outrun-future", Typeface.NORMAL);
            case 34:
                return Typeface.create("pompiere", Typeface.NORMAL);
            case 35:
                return Typeface.create("reemkufi", Typeface.NORMAL);
            case 36:
                return Typeface.create("riviera", Typeface.NORMAL);
            case 37:
                return Typeface.create("the-outbox", Typeface.NORMAL);
            case 38:
                return Typeface.create("themeable-date", Typeface.NORMAL);
            case 39:
                return Typeface.create("vibur", Typeface.NORMAL);
            case 40:
                return Typeface.create("voltaire", Typeface.NORMAL);
        }
    }

    /**
     * Returns the preferred Y position of the clock.
     *
     * @param totalHeight The height available to position the clock.
     * @return Y position of clock.
     */
    public int getClockPreferredY(int totalHeight) {
        return mClockView.getPreferredY(totalHeight);
    }

    private void updateLogoutView() {
        if (mLogoutView == null) {
            return;
        }
        mLogoutView.setVisibility(shouldShowLogout() ? VISIBLE : GONE);
        // Logout button will stay in language of user 0 if we don't set that manually.
        mLogoutView.setText(mContext.getResources().getString(
                com.android.internal.R.string.global_action_logout));
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String info = mLockPatternUtils.getDeviceOwnerInfo();
        if (info == null) {
            // Use the current user owner information if enabled.
            final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                    KeyguardUpdateMonitor.getCurrentUser());
            if (ownerInfoEnabled) {
                info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
        }
        mOwnerInfo.setText(info);
        updateDark();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
        Dependency.get(ConfigurationController.class).addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
        Dependency.get(ConfigurationController.class).removeCallback(this);
    }

    @Override
    public void onLocaleListChanged() {
        refreshFormat();
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case LOCKSCREEN_DATE_SELECTION:
                    mDateSelection = TunerService.parseInteger(newValue, 0);
                onDensityOrFontScaleChanged();
                break;
	    case LOCK_CLOCK_FONT_SIZE:
                    mLockClockFontSize = TunerService.parseInteger(newValue, 58);
                onDensityOrFontScaleChanged();
                break;
	    case LOCK_DATE_FONT_SIZE:
                    mLockDateFontSize = TunerService.parseInteger(newValue, 18);
                onDensityOrFontScaleChanged();
                break;
	    case LOCKOWNER_FONT_SIZE:
                    mOwnerInfoSize = TunerService.parseInteger(newValue, 18);
                onDensityOrFontScaleChanged();
                break;
            case LOCK_OWNERINFO_FONTS:
                    mOwnerInfoFontStyle = TunerService.parseInteger(newValue, 4);
                onDensityOrFontScaleChanged();
                break;
            default:
                break;
        }
    }

    private void refreshLockFont() {
		setFontStyle(mClockView, getLockClockFont());
    }

    private void setFontStyle(KeyguardClockSwitch view, int fontstyle) {
    	if (view != null) {
    		switch (fontstyle) {
    			case 0:
    			default:
    				view.setTextFont(Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_headline_medium), Typeface.NORMAL));
    				break;
    			case 1:
    				view.setTextFont(Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_body_medium), Typeface.NORMAL));
    				break;
    			case 2:
    				view.setTextFont(Typeface.create("sans-serif", Typeface.BOLD));
    				break;
    			case 3:
    				view.setTextFont(Typeface.create("sans-serif", Typeface.NORMAL));
    				break;
    			case 4:
    				view.setTextFont(Typeface.create("sans-serif", Typeface.ITALIC));
    				break;
    			case 5:
    				view.setTextFont(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
    				break;
    			case 6:
    				view.setTextFont(Typeface.create("sans-serif-light", Typeface.NORMAL));
    				break;
    			case 7:
    				view.setTextFont(Typeface.create("sans-serif-thin", Typeface.NORMAL));
    				break;
    			case 8:
    				view.setTextFont(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
    				break;
    			case 9:
    				view.setTextFont(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
    				break;
    			case 10:
    				view.setTextFont(Typeface.create("sans-serif-condensed", Typeface.BOLD));
    				break;
    			case 11:
    				view.setTextFont(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
    				break;
    			case 12:
    				view.setTextFont(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    				break;
    			case 13:
    				view.setTextFont(Typeface.create("sans-serif-medium", Typeface.ITALIC));
    				break;
                        case 14:
                                view.setTextFont(Typeface.create("abelreg", Typeface.NORMAL));
                                break;
                        case 15:
                                view.setTextFont(Typeface.create("adventpro", Typeface.NORMAL));
                                break;
                        case 16:
                                view.setTextFont(Typeface.create("alien-league", Typeface.NORMAL));
                                break;
                        case 17:
                                view.setTextFont(Typeface.create("bignoodle-italic", Typeface.NORMAL));
                                break;
                        case 18:
                                view.setTextFont(Typeface.create("biko", Typeface.NORMAL));
                                break;
                        case 19:
                                view.setTextFont(Typeface.create("blern", Typeface.NORMAL));
                                break;
                        case 20:
                                view.setTextFont(Typeface.create("cherryswash", Typeface.NORMAL));
                                break;
                        case 21:
                                view.setTextFont(Typeface.create("codystar", Typeface.NORMAL));
                                break;
                        case 22:
                                view.setTextFont(Typeface.create("ginora-sans", Typeface.NORMAL));
                                break;
                        case 23:
                                view.setTextFont(Typeface.create("gobold-light-sys", Typeface.NORMAL));
                                break;
                        case 24:
                                view.setTextFont(Typeface.create("googlesans-sys", Typeface.NORMAL));
                                break;
                        case 25:
                                view.setTextFont(Typeface.create("inkferno", Typeface.NORMAL));
                                break;
                        case 26:
                                view.setTextFont(Typeface.create("jura-reg", Typeface.NORMAL));
                                break;
                        case 27:
                                view.setTextFont(Typeface.create("kellyslab", Typeface.NORMAL));
                                break;
                        case 28:
                                view.setTextFont(Typeface.create("metropolis1920", Typeface.NORMAL));
                                break;
                        case 29:
                                view.setTextFont(Typeface.create("neonneon", Typeface.NORMAL));
                                break;
                        case 30:
                                view.setTextFont(Typeface.create("pompiere", Typeface.NORMAL));
                                break;
                        case 31:
                                view.setTextFont(Typeface.create("reemkufi", Typeface.NORMAL));
                                break;
                        case 32:
                                view.setTextFont(Typeface.create("riviera", Typeface.NORMAL));
                                break;
                        case 33:
                                view.setTextFont(Typeface.create("roadrage-sys", Typeface.NORMAL));
                                break;
                        case 34:
                                view.setTextFont(Typeface.create("sedgwick-ave", Typeface.NORMAL));
                                break;
                        case 35:
                                view.setTextFont(Typeface.create("snowstorm-sys", Typeface.NORMAL));
                                break;
                        case 36:
                                view.setTextFont(Typeface.create("themeable-clock", Typeface.NORMAL));
                                break;
                        case 37:
                                view.setTextFont(Typeface.create("unionfont", Typeface.NORMAL));
                                break;
                        case 38:
                                view.setTextFont(Typeface.create("vibur", Typeface.NORMAL));
                                break;
                        case 39:
                                view.setTextFont(Typeface.create("voltaire", Typeface.NORMAL));
                                break;
    		}
    	}
    }

    private void setFontSize(KeyguardClockSwitch view, int size) {
        switch (size) {
            case 54:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_54));
                break;
            case 55:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_55));
                break;
            case 56:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_56));
                break;
            case 57:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_57));
                break;
            case 58:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_58));
                break;
            case 59:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_59));
                break;
            case 60:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_60));
                break;
            case 61:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_61));
                break;
            case 62:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_62));
                break;
            case 63:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_63));
                break;
            case 64:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_64));
                break;
            case 65:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_65));
                break;
            case 66:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_66));
                break;
            case 67:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_67));
                break;
            case 68:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_68));
                break;
            case 69:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_69));
                break;
            case 70:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_70));
                break;
            case 71:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_71));
                break;
            case 72:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_72));
                break;
            case 73:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_73));
                break;
            case 74:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_74));
                break;
            case 75:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_75));
                break;
            case 76:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_76));
                break;
            case 77:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_77));
                break;
            case 78:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_78));
                break;
            case 79:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_79));
                break;
            case 80:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_80));
                break;
            case 81:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_81));
                break;
            case 82:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_82));
                break;
            case 83:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_83));
                break;
            case 84:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_84));
                break;
            case 85:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_85));
                break;
            case 86:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_86));
                break;
            case 87:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_87));
                break;
            case 88:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_88));
                break;
            case 89:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_89));
                break;
            case 90:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_90));
                break;
            case 91:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_91));
                break;
            case 92:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_92));
                break;
            case 93:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_93));
                break;
            case 94:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_94));
                break;
            case 95:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_95));
                break;
            case 96:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_96));
                break;
            case 97:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_97));
                break;
            case 98:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_98));
                break;
            case 99:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_99));
                break;
            case 100:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_100));
                break;
            case 101:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_101));
                break;
            case 102:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_102));
                break;
            case 103:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_103));
                break;
            case 104:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_104));
                break;
            case 105:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_105));
                break;
            case 106:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_106));
                break;
            case 107:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_107));
                break;
            case 108:
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_108));
                break;
            default:
                break;
        }
    }

    private void setOwnerInfoSize(int size) {
        switch (size) {
            case 10:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_10));
                break;
            case 11:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_11));
                break;
            case 12:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_12));
                break;
            case 13:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_13));
                break;
            case 14:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14));
                break;
            case 15:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_15));
                break;
            case 16:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_16));
                break;
            case 17:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_17));
                break;
            case 18:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_18));
                break;
            case 19:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_19));
                break;
            case 20:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_20));
                break;
            case 21:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21));
                break;
            case 22:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_22));
                break;
            case 23:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_23));
                break;
            case 24:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_24));
                break;
            case 25:
                mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_25));
                break;
        }
    }

    private void setOwnerInfoFontStyle(int fontstyle) {
        switch (fontstyle) {
            case FONT_HEADLINE:
            default:
                mOwnerInfo.setTypeface(Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_headline_medium), Typeface.NORMAL));
                break;
            case FONT_BODY:
                mOwnerInfo.setTypeface(Typeface.create(mContext.getResources().getString(R.string.clock_sysfont_body_medium), Typeface.NORMAL));
                break;
            case FONT_NORMAL:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                break;
            case FONT_BOLD:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                break;    
            case FONT_ITALIC:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                break;
            case FONT_BOLD_ITALIC:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
                break;                
            case FONT_LIGHT:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                break;         
            case FONT_THIN:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                break;                
            case FONT_CONDENSED:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                break;
            case FONT_CONDENSED_ITALIC:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
                break;
            case FONT_CONDENSED_BOLD:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                break;
            case FONT_CONDENSED_BOLD_ITALIC:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
                break;
            case FONT_MEDIUM:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                break;
            case FONT_MEDIUM_ITALIC:
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
                break;
            case FONT_ALBELREG:
                mOwnerInfo.setTypeface(Typeface.create("abelreg", Typeface.NORMAL));
                break;
            case FONT_ADVENTPRO:
                mOwnerInfo.setTypeface(Typeface.create("adventpro", Typeface.NORMAL));
                break;
            case FONT_ALIENLEAGUE:
                mOwnerInfo.setTypeface(Typeface.create("alien-league", Typeface.NORMAL));
                break;
            case FONT_BIGNOODLEITALIC:
                mOwnerInfo.setTypeface(Typeface.create("bignoodle-italic", Typeface.NORMAL));
                break;
            case FONT_BIKO:
                mOwnerInfo.setTypeface(Typeface.create("biko", Typeface.NORMAL));
                break;
            case FONT_BLERN:
                mOwnerInfo.setTypeface(Typeface.create("blern", Typeface.NORMAL));
                break;
            case FONT_CHERRYSWASH:
                mOwnerInfo.setTypeface(Typeface.create("cherryswash", Typeface.NORMAL));
                break;
            case FONT_CODYSTAR:
                mOwnerInfo.setTypeface(Typeface.create("codystar", Typeface.NORMAL));
                break;
            case FONT_GINORASANS:
                mOwnerInfo.setTypeface(Typeface.create("ginora-sans", Typeface.NORMAL));
                break;
            case FONT_GOBOLDLIGHT:
                mOwnerInfo.setTypeface(Typeface.create("gobold-light-sys", Typeface.NORMAL));
                break;
            case FONT_GOOGLESANS:
                mOwnerInfo.setTypeface(Typeface.create("googlesans-sys", Typeface.NORMAL));
                break;
            case FONT_INKFERNO:
                mOwnerInfo.setTypeface(Typeface.create("inkferno", Typeface.NORMAL));
                break;
            case FONT_JURAREG:
                mOwnerInfo.setTypeface(Typeface.create("jura-reg", Typeface.NORMAL));
                break;
            case FONT_KELLYSLAB:
                mOwnerInfo.setTypeface(Typeface.create("kellyslab", Typeface.NORMAL));
                break;
            case FONT_METROPOLIS:
                mOwnerInfo.setTypeface(Typeface.create("metropolis1920", Typeface.NORMAL));
                break;
            case FONT_NEONNEON:
                mOwnerInfo.setTypeface(Typeface.create("neonneon", Typeface.NORMAL));
                break;
             case FONT_POMPIERE:
                mOwnerInfo.setTypeface(Typeface.create("pompiere", Typeface.NORMAL));
                break;
             case FONT_REEMKUFI:
                mOwnerInfo.setTypeface(Typeface.create("reemkufi", Typeface.NORMAL));
                break;
             case FONT_RIVIERA:
                mOwnerInfo.setTypeface(Typeface.create("riviera", Typeface.NORMAL));
                break;
             case FINT_ROADRAGE:
                mOwnerInfo.setTypeface(Typeface.create("roadrage-sys", Typeface.NORMAL));
                break;
             case FONT_SEDGWICKAVE:
                mOwnerInfo.setTypeface(Typeface.create("sedgwick-ave", Typeface.NORMAL));
                break;
             case FONT_SNOWSTORM:
                mOwnerInfo.setTypeface(Typeface.create("snowstorm-sys", Typeface.NORMAL));
                break;
             case FONT_THEMEABLECLOCK:
                mOwnerInfo.setTypeface(Typeface.create("themeable-clock", Typeface.NORMAL));
                break;
             case FONT_UNIONFONT:
                mOwnerInfo.setTypeface(Typeface.create("unionfont", Typeface.NORMAL));
                break;
             case FONT_VIBUR:
                mOwnerInfo.setTypeface(Typeface.create("vibur", Typeface.NORMAL));
                break;
             case FONT_VOLTAIRE:
                mOwnerInfo.setTypeface(Typeface.create("voltaire", Typeface.NORMAL));
                break;
        }
    }
    
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("KeyguardStatusView:");
        pw.println("  mOwnerInfo: " + (mOwnerInfo == null
                ? "null" : mOwnerInfo.getVisibility() == VISIBLE));
        pw.println("  mPulsing: " + mPulsing);
        pw.println("  mDarkAmount: " + mDarkAmount);
        pw.println("  mTextColor: " + Integer.toHexString(mTextColor));
        if (mLogoutView != null) {
            pw.println("  logout visible: " + (mLogoutView.getVisibility() == VISIBLE));
        }
        if (mClockView != null) {
            mClockView.dump(fd, pw, args);
        }
        if (mKeyguardSlice != null) {
            mKeyguardSlice.dump(fd, pw, args);
        }
    }

    private void loadBottomMargin() {
        mIconTopMargin = getResources().getDimensionPixelSize(R.dimen.widget_vertical_padding);
        mIconTopMarginWithHeader = getResources().getDimensionPixelSize(
                R.dimen.widget_vertical_padding_with_header);
    }

    private void updateSettings() {
        final ContentResolver resolver = getContext().getContentResolver();

        mClockSelection = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.LOCKSCREEN_CLOCK_SELECTION, 2, UserHandle.USER_CURRENT);

        mClockView = findViewById(R.id.keyguard_clock_container);

        // Set smaller Clock, Date and OwnerInfo text size if the user selects the small clock type
	    if (mClockSelection == 4) {
    	    mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.widget_clock_small_font_size));
    	} else {
	        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
	    }

        switch (mClockSelection) {
            case 1: // hidden
                mClockView.setVisibility(View.GONE);
                break;
            case 2: // default
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 3: // default (bold)
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 4: // default (small font)
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 5: // default (accent)
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 6: // default (accent hr)
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 7: // default (accent min)
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 8: // sammy
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 9: // sammy (bold)
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 10: // sammy (accent)
                mClockView.setVisibility(View.VISIBLE);
                break;
            case 11: // sammy (accent alt)
                mClockView.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void updateAll() {
        updateSettings();
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }

    public void setDarkAmount(float darkAmount) {
        if (mDarkAmount == darkAmount) {
            return;
        }
        mDarkAmount = darkAmount;
        mClockView.setDarkAmount(darkAmount);
        updateDark();
    }

    private void updateDark() {
        boolean dark = mDarkAmount == 1;
        if (mLogoutView != null) {
            mLogoutView.setAlpha(dark ? 0 : 1);
        }

        if (mOwnerInfo != null) {
            boolean hasText = !TextUtils.isEmpty(mOwnerInfo.getText());
            mOwnerInfo.setVisibility(hasText ? VISIBLE : GONE);
            layoutOwnerInfo();
        }

        final int blendedTextColor = ColorUtils.blendARGB(mTextColor, Color.WHITE, mDarkAmount);
        mKeyguardSlice.setDarkAmount(mDarkAmount);
        mClockView.setTextColor(blendedTextColor);
    }

    private void layoutOwnerInfo() {
        if (mOwnerInfo != null && mOwnerInfo.getVisibility() != GONE) {
            // Animate owner info during wake-up transition
            mOwnerInfo.setAlpha(1f - mDarkAmount);

            float ratio = mDarkAmount;
            // Calculate how much of it we should crop in order to have a smooth transition
            int collapsed = mOwnerInfo.getTop() - mOwnerInfo.getPaddingTop();
            int expanded = mOwnerInfo.getBottom() + mOwnerInfo.getPaddingBottom();
            int toRemove = (int) ((expanded - collapsed) * ratio);
            setBottom(getMeasuredHeight() - toRemove);
            if (mNotificationIcons != null) {
                // We're using scrolling in order not to overload the translation which is used
                // when appearing the icons
                mNotificationIcons.setScrollY(toRemove);
            }
        } else if (mNotificationIcons != null){
            mNotificationIcons.setScrollY(0);
        }
    }

    public void setPulsing(boolean pulsing) {
        if (mPulsing == pulsing) {
            return;
        }
        mPulsing = pulsing;
    }

    private boolean shouldShowLogout() {
        return KeyguardUpdateMonitor.getInstance(mContext).isLogoutEnabled()
                && KeyguardUpdateMonitor.getCurrentUser() != UserHandle.USER_SYSTEM;
    }

    private void onLogoutClicked(View view) {
        int currentUserId = KeyguardUpdateMonitor.getCurrentUser();
        try {
            mIActivityManager.switchUser(UserHandle.USER_SYSTEM);
            mIActivityManager.stopUser(currentUserId, true /*force*/, null);
        } catch (RemoteException re) {
            Log.e(TAG, "Failed to logout user", re);
        }
    }

    private void updateSettings() {
        final ContentResolver resolver = getContext().getContentResolver();
        final Resources res = getContext().getResources();
        mShowWeather = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKSCREEN_WEATHER_ENABLED, 0,
                UserHandle.USER_CURRENT) == 1;

        mOmniStyle = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKSCREEN_WEATHER_STYLE, 1,
                UserHandle.USER_CURRENT) == 0;

        if (mWeatherView != null) {
            if (mShowWeather && mOmniStyle) {
                mWeatherView.setVisibility(View.VISIBLE);
                mWeatherView.enableUpdates();
            }
            if (!mShowWeather || !mOmniStyle) {
                mWeatherView.setVisibility(View.GONE);
                mWeatherView.disableUpdates();
            }
        }
    }
}
