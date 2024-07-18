/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.androidchannel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DashboardFragment extends Fragment implements Intents {

    private RelativeLayout m_throttleLayout;
    private TextView m_throttleMessage;
    private TextView m_throttleTitle;
    private TextView m_throttleIcon;
    private TextView m_throttleInstallMessage;

    private RelativeLayout m_networkError;
    private RelativeLayout m_sendError;

    private RelativeLayout m_pausedLayout;
    private RelativeLayout m_activeLayout;
    private RelativeLayout m_dozeWarning;

    private DashboardReceiver m_receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dashboard, container, false);

        m_throttleLayout = (RelativeLayout)view.findViewById(R.id.throttle);
        m_throttleIcon = (TextView)view.findViewById(R.id.throttle_icon);
        m_throttleTitle = (TextView)view.findViewById(R.id.throttle_title);
        m_throttleMessage = (TextView)view.findViewById(R.id.throttle_message);
        m_throttleInstallMessage = (TextView)view.findViewById(R.id.throttle_install_message);

        m_networkError = (RelativeLayout)view.findViewById(R.id.network_error);
        m_sendError = (RelativeLayout)view.findViewById(R.id.send_error);
        m_dozeWarning = (RelativeLayout)view.findViewById(R.id.doze_warning);

        m_pausedLayout = (RelativeLayout) view.findViewById(R.id.status_paused);
        m_activeLayout = (RelativeLayout)view.findViewById(R.id.status_active);

        m_dozeWarning.setVisibility(View.VISIBLE);

        return view;
    }

    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
        m_receiver = new DashboardReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.UPDATE_COUNTS);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        activity.registerReceiver(m_receiver, filter);

        RapidPro.broadcastUpdatedCounts(activity);
    }

    public void updateDashboardWidgets(Intent intent){
        // no longer attached, no-op
        if (getActivity() == null){
            return;
        }

        boolean isPaused = intent.getBooleanExtra(Intents.IS_PAUSED, false);

        m_activeLayout.setVisibility(isPaused ? View.GONE : View.VISIBLE);
        m_pausedLayout.setVisibility(!isPaused ? View.GONE : View.VISIBLE);



        int sent = intent.getIntExtra(Intents.SENT_EXTRA, 0);
        int capacity = intent.getIntExtra(Intents.CAPACITY_EXTRA, 0);
        int minutes = 30;

        // show our throttle warning
        m_throttleLayout.setVisibility(isPaused ? View.GONE : View.VISIBLE);

        String title = getResources().getString(R.string.throttle_title_ok);
        String text = getResources().getString(R.string.throttle_ok);
        String icon = getResources().getString(R.string.icon_throttle_okay);

        int background = R.drawable.active_background;
        int color = R.color.font_green;
        if (sent >= capacity){
            title = getResources().getString(R.string.throttle_title_error);
            text = getResources().getString(R.string.throttle_error);
            icon = getResources().getString(R.string.icon_throttle_danger);
            background = R.drawable.error_background;
            color = R.color.font_red;
        } else if (sent * 2 >= capacity){
            title = getResources().getString(R.string.throttle_title_warning);
            text = getResources().getString(R.string.throttle_warning);
            icon = getResources().getString(R.string.icon_throttle_warning);
            background = R.drawable.warning_background;
            color = R.color.font_orange;
        }

        text = text.replace("SENT", "" + sent);
        text = text.replace("CAPACITY", "" + capacity);
        text = text.replace("MINUTES", "" + minutes);

        m_throttleTitle.setText(title);
        m_throttleTitle.setTextColor(getResources().getColor(color));

        m_throttleIcon.setText(icon);
        m_throttleIcon.setTextColor(getResources().getColor(color));

        m_throttleMessage.setText(Html.fromHtml(text));
        m_throttleLayout.setBackgroundDrawable(getResources().getDrawable(background));

        boolean networkUp = intent.getBooleanExtra(Intents.CONNECTION_UP_EXTRA, true);
        m_networkError.setVisibility(networkUp ? View.GONE : View.VISIBLE);


        int retryCount = intent.getIntExtra(Intents.RETRY_EXTRA, 0);
        m_sendError.setVisibility((retryCount > 0) ? View.VISIBLE : View.GONE);


        // how many packs do they have remaining
        int packsRemaining = 11 - RapidPro.get().getInstalledPacks().size();
        String installText = getResources().getString(R.string.throttle_packs_remaining);
        if (packsRemaining == 0) {
            installText = getResources().getString(R.string.throttle_no_packs_remaining);
        } else {
            installText = installText.replace("PACKS_REMAINING", "" + packsRemaining);
            installText = installText.replace("PACKS_PLURAL", packsRemaining != 1 ? "s" : "");
        }
        m_throttleInstallMessage.setText(Html.fromHtml(installText));

        if (packsRemaining < 1) {
            m_throttleInstallMessage.setVisibility(View.GONE);
        } else {
            m_throttleInstallMessage.setVisibility(View.VISIBLE);
        }

    }

    public void onDetach(){
        super.onDetach();
        getActivity().unregisterReceiver(m_receiver);
    }

    public void onResume(){
        super.onResume();
        RapidPro.broadcastUpdatedCounts(getActivity());
    }

    class DashboardReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intents.UPDATE_COUNTS)){
                updateDashboardWidgets(intent);
            }
        }
    }
}
