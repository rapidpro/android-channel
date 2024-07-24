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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class UnclaimedFragment extends Fragment {

    private ClaimCodeReceiver m_receiver;
    private TextView m_claimCode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.unclaimed, container, false);
    }

    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        m_claimCode = (TextView) getView().findViewById(R.id.claim_code);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String code = prefs.getString(SettingsActivity.RELAYER_CLAIM_CODE, "...");

        setClaimCode(code);
    }

    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
        m_receiver = new ClaimCodeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.UPDATE_RELAYER_STATE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        activity.registerReceiver(m_receiver, filter);
    }

    public void setClaimCode(String claimCode){
        if (claimCode == null){
            claimCode = "...";
        } else if (claimCode.length() > 3){
            claimCode = claimCode.substring(0,3) + " " + claimCode.substring(3,6) + " " + claimCode.substring(6,9);
        }
        m_claimCode.setText(claimCode);
    }


    public void onDetach(){
        super.onDetach();
        getActivity().unregisterReceiver(m_receiver);
    }

    class ClaimCodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intents.UPDATE_RELAYER_STATE) && intent.hasExtra(Intents.CLAIM_CODE_EXTRA)){
                String claimCode = intent.getStringExtra(Intents.CLAIM_CODE_EXTRA);
                setClaimCode(claimCode);
            }
        }
    }
}
