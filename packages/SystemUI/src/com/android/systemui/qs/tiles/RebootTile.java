/*
 * Copyright (C) 2019 ion-OS
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.quicksettings.Tile;
import com.android.systemui.R;
import com.android.systemui.qs.QSHost;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.statusbar.IStatusBarService;

import javax.inject.Inject;

public class RebootTile extends QSTileImpl<BooleanState> {

    private int mReboot = 0;
    private IStatusBarService mBarService;

    @Inject
    public RebootTile(QSHost host) {
        super(host);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ANCIENT_SETTINGS;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        if (mReboot == 0) {
            mReboot = 1;
        } else if (mReboot == 1) {
            mReboot = 2;
        } else if (mReboot == 2) {
            mReboot = 3;
        } else if (mReboot == 3) {
            mReboot = 4;
        } else {
            mReboot = 0;
        }
        refreshState();
    }

    @Override
    protected void handleLongClick() {
        mHost.collapsePanels();
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    if(mReboot == 1)
                        mBarService.reboot(false);
                    else if (mReboot == 2)
                        mBarService.advancedReboot(PowerManager.REBOOT_RECOVERY);
                    else if (mReboot == 3)
                        mBarService.advancedReboot(PowerManager.REBOOT_BOOTLOADER);
                    else if (mReboot == 4)
                        restartSystemUI(mContext);
                    else
                        mBarService.shutdown();
                } catch (RemoteException e) {
                }
            }
        }, 500);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public void handleSetListening(boolean listening) {
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_reboot_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mReboot == 1) {
            state.label = mContext.getString(R.string.quick_settings_reboot_label);
            state.icon = ResourceIcon.get(com.android.internal.R.drawable.ic_restart);
            state.contentDescription =  mContext.getString(
                    R.string.quick_settings_reboot_label);
        } else if (mReboot == 2) {
            state.label = mContext.getString(R.string.quick_settings_reboot_recovery_label);
            state.icon = ResourceIcon.get(R.drawable.ic_restart_recovery);
            state.contentDescription =  mContext.getString(
                    R.string.quick_settings_reboot_recovery_label);
        } else if (mReboot == 3) {
            state.label = mContext.getString(R.string.quick_settings_reboot_bootloader_label);
            state.icon = ResourceIcon.get(R.drawable.ic_restart_bootloader);
            state.contentDescription =  mContext.getString(
                    R.string.quick_settings_reboot_bootloader_label);
        } else if (mReboot == 4) {
            state.label = mContext.getString(R.string.quick_settings_reboot_ui_label);
            state.icon = ResourceIcon.get(R.drawable.ic_restart_ui);
            state.contentDescription =  mContext.getString(
                    R.string.quick_settings_reboot_ui_label);
        } else {
            state.label = mContext.getString(R.string.quick_settings_poweroff_label);
            state.icon = ResourceIcon.get(com.android.internal.R.drawable.ic_lock_power_off);
            state.contentDescription =  mContext.getString(
                    R.string.quick_settings_poweroff_label);
        }
    }

    public static void restartSystemUI(Context mContext) {
        Process.killProcess(Process.myPid());
    }
}
