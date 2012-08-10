/*******************************************************************************
 * Copyright (c) 2012 Pieter Pareit.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Pieter Pareit - initial API and implementation
 ******************************************************************************/
package be.ppareit.hidebar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * When this receiver is called, it will toggle the visibility of the systembar
 */
public class ToggleSystembarReceiver extends BroadcastReceiver {

    private static final String TAG = ToggleSystembarReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");
        Device dev = Device.getInstance();
        if (dev.isSystembarVisible() == true) {
            // we need to hide the statusbar
            BackgroundService.showBar(false);
            // start the restore systembar service
            context.startService(new Intent(context, RestoreSystembarService.class));
        } else {
            // TODO: improve on this, the RestoreSystembarService could be implemented
            // with an broadcastreceiver for ACTION_BARHIDDEN
            BackgroundService.stop(context);
            BackgroundService.start(context, false);
        }
    }
}