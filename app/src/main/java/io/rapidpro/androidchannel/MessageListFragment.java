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

import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.*;
import io.rapidpro.androidchannel.contentprovider.DBCommandContentProvider;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.MOTextMessage;
import io.rapidpro.androidchannel.payload.MTTextMessage;
import io.rapidpro.androidchannel.ui.IconTextView;

import java.text.DateFormat;
import java.util.Date;

public class MessageListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private SimpleCursorAdapter m_adapter;
    private MessageObserver m_observer;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        m_adapter = new SimpleCursorAdapter(getActivity(), R.layout.command, null,
            new String[]{ DBCommandHelper.COL_CMD, DBCommandHelper.COL_CREATED, DBCommandHelper.COL_TITLE, DBCommandHelper.COL_BODY,
                          DBCommandHelper.COL_STATE, DBCommandHelper.COL_STATE, DBCommandHelper.COL_STATE, DBCommandHelper.COL_STATE  },
            new int[]{ R.id.commandIcon, R.id.commandDate, R.id.commandTitle, R.id.commandBody,
                       R.id.small1, R.id.small2 },
            Adapter.NO_SELECTION);

        m_adapter.setViewBinder(new DBCommandViewBinder());

        setListAdapter(m_adapter);
        setListShown(false);
    }

    class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intents.UPDATE_COUNTS)){
                getLoaderManager().restartLoader(0, null, MessageListFragment.this);
            }
        }
    }

    private UpdateReceiver m_receiver;

    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);

        m_receiver = new UpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        activity.registerReceiver(m_receiver, filter);

        getLoaderManager().initLoader(0, null, this);

        // listen to changes to our content provider
        Handler handler = new Handler(activity.getMainLooper());
        m_observer = new MessageObserver(handler);

        ContentResolver cr = activity.getContentResolver();
        cr.registerContentObserver(DBCommandContentProvider.CONTENT_URI, true, m_observer);
    }

    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(m_receiver);

        ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(m_observer);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Bundle args = getArguments();
        int statusId = args != null ? args.getInt(Intents.STATUS_ID_EXTRA, -1) : -1;

        StringBuilder match = new StringBuilder();
        match.append("hidden = ?");
        String[] values = new String[] { "" + DBCommandHelper.VISIBLE };

        return new CursorLoader(getActivity(),
                DBCommandContentProvider.CONTENT_URI,
                DBCommandHelper.ALL_COLS, match.toString(), values, "_id desc");
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        m_adapter.swapCursor(cursor);
        setListShownNoAnimation(true);
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        m_adapter.swapCursor(null);
    }

    private class DBCommandViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int colIndex) {

            // phone number
            if (colIndex == DBCommandHelper.CMD_IDX){
                String cmd = cursor.getString(colIndex);
                int commandState = cursor.getInt(DBCommandHelper.STATE_IDX);
                boolean complete = commandState == DBCommandHelper.COMPLETE ||
                        commandState == MTTextMessage.DELIVERED_SYNCED ||
                        commandState == MTTextMessage.SENT_SYNCED ||
                        commandState == MTTextMessage.FAILED_SYNCED;

                boolean mt_failed = cmd.equals("mt_sms") &&
                        (commandState == MTTextMessage.FAILED || commandState == MTTextMessage.FAILED_SYNCED);

                TextView img = (TextView) view;

                if (mt_failed){
                    img.setText(getString(R.string.icon_bubble_bang));
                    img.setTextColor(getResources().getColor(R.color.font_red));
                } else if (cmd.equals("call")){
                        img.setText(getString(R.string.icon_call));
                        img.setTextColor(getResources().getColor(R.color.font_green));
                } else if (cmd.equals("mo_sms")){
                    img.setText(getString(R.string.icon_bubble_person));
                    img.setTextColor(getResources().getColor(R.color.font_green));
                } else if (cmd.equals("mt_sms")){
                    img.setText(getString(R.string.icon_bubble_right));
                    img.setTextColor(getResources().getColor(R.color.font_green));
                } else {
                    img.setText(getString(R.string.icon_cloud));
                    img.setTextColor(getResources().getColor(R.color.font_green));
                }

                if (!complete){
                    img.setTextColor(getResources().getColor(R.color.font_orange));
                }
            }

            // date/time
            else if (colIndex == DBCommandHelper.CREATED_IDX){
                Date created = new Date(cursor.getLong(colIndex));
                TextView textView = (TextView) view;
                textView.setText(DateFormat.getDateTimeInstance().format(created));
            }

            // body
            else if (colIndex == DBCommandHelper.BODY_IDX){
                String body = cursor.getString(colIndex);
                TextView textView = (TextView) view;
                textView.setText(body);
            }

            // title
            else if (colIndex == DBCommandHelper.TITLE_IDX){
                String title = cursor.getString(colIndex);
                TextView textView = (TextView) view;
                textView.setText(title);
            }

            // state
            else if (colIndex == DBCommandHelper.STATE_IDX){
                RelativeLayout parent = (RelativeLayout) view.getParent();

                int state = cursor.getInt(colIndex);
                String title = cursor.getString(DBCommandHelper.BODY_IDX);

                String type = cursor.getString(DBCommandHelper.CMD_IDX);

                IconTextView v1 = (IconTextView) parent.findViewById(R.id.small1);
                IconTextView v2 = (IconTextView) parent.findViewById(R.id.small2);

                v2.setText(getResources().getString(R.string.icon_bubble_check));

                if (type.equals(MTTextMessage.CMD)){
                    v1.setVisibility(View.VISIBLE);
                    v2.setVisibility(View.VISIBLE);

                    if (state == DBCommandHelper.BORN){
                        v1.setIconColor(R.color.font_light_grey);
                        v2.setIconColor(R.color.font_light_grey);
                    }
                    else if (state == MTTextMessage.PENDING){
                        v1.setIconColor(R.color.font_orange);
                        v2.setIconColor(R.color.font_light_grey);
                    }
                    else if (state == MTTextMessage.SENT){
                        v1.setIconColor(R.color.font_blue);
                        v2.setIconColor(R.color.font_light_grey);
                    }
                    else if (state == MTTextMessage.FAILED){
                        v2.setText(getResources().getString(R.string.icon_bubble_bang));
                        v2.setIconColor(R.color.font_blue);
                        v1.setVisibility(View.GONE);
                    }
                    else if (state == MTTextMessage.FAILED_SYNCED){
                        v2.setText(getResources().getString(R.string.icon_bubble_bang));
                        v2.setIconColor(R.color.font_green);
                        v1.setVisibility(View.GONE);
                    }
                    else if (state == MTTextMessage.SENT_SYNCED){
                        v1.setIconColor(R.color.font_green);
                        v2.setIconColor(R.color.font_light_grey);
                    }
                    else if (state == MTTextMessage.DELIVERED){
                        v1.setIconColor(R.color.font_green);
                        v2.setIconColor(R.color.font_blue);
                    }
                    else if (state == MTTextMessage.DELIVERED_SYNCED){
                        v1.setIconColor(R.color.font_green);
                        v2.setIconColor(R.color.font_green);
                    }
                    else if (state == MTTextMessage.RETRY){
                        v1.setIconColor(R.color.font_red);
                        v2.setIconColor(R.color.font_light_grey);
                    }
                    else {
                        v1.setIconColor(R.color.font_light_grey);
                        v2.setIconColor(R.color.font_light_grey);
                    }
                } else if (type.equals(MOTextMessage.CMD)){
                    v1.setVisibility(View.GONE);
                    v2.setText(getResources().getString(R.string.icon_bubble_person));
                    if (state == DBCommandHelper.BORN){
                        v2.setIconColor(R.color.font_blue);
                    } else {
                        v2.setIconColor(R.color.font_green);
                    }
                } else {
                    v1.setVisibility(View.GONE);
                    v2.setText(getResources().getString(R.string.icon_cloud));
                    if (state == DBCommandHelper.BORN){
                        v2.setIconColor(R.color.font_blue);
                    } else {
                        v2.setIconColor(R.color.font_green);
                    }
                }
            }

            // nothing should be set by default view setter
            return true;
        }

    }

    class MessageObserver extends ContentObserver {
        public MessageObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (getActivity() != null){
                getLoaderManager().restartLoader(0, null, MessageListFragment.this);
            }
        }
    }
}
