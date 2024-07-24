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

package io.rapidpro.androidchannel.ui;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.rapidpro.androidchannel.R;

public class IconStatus extends RelativeLayout {
    public IconStatus(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setIncomingMessages(int messages, int minutes) {
        setIcon(R.string.icon_bubble_left, R.color.font_green);
        setMessage(Html.fromHtml("<b>" + messages + "</b> received in last " + minutes + "m"));
    }

    public void setOutgoingMessages(int messages) {
        setIcon(R.string.icon_bubble_right, R.color.font_orange);
        setMessage(Html.fromHtml("<b>" + messages + "</b> outgoing messages"));
    }

    public void setRetryMessages(int messages) {
        setIcon(R.string.icon_bubble_bang, R.color.font_red);
        setMessage(Html.fromHtml("<b>" + messages + "</b> messages to retry"));
    }

    public void setSyncMessages(int messages) {
        setIcon(R.string.icon_cloud, R.color.font_blue);
        setMessage(Html.fromHtml("<b>" + messages + "</b> messages to sync"));
    }

    public void setThrottleMessage(int used, int total) {

        int color = R.color.font_green;
        int icon = R.string.icon_throttle_okay;

        if (used > ((double)total * .5)) {
            color = R.color.font_orange;
            icon = R.string.icon_throttle_warning;
        }

        if (used > ((double)total * .8)) {
            color = R.color.font_red;
            icon = R.string.icon_throttle_danger;
        }

        setIcon(icon, color);
        setMessage(Html.fromHtml("<b>" + used + " / " + total + "</b> sent in last 30m"));

    }

    public void setIcon(int id, int color) {
        IconTextView tv = (IconTextView) findViewById(R.id.icon);
        tv.setText(id);
        tv.setTextColor(getContext().getColor(color));
    }

    private void setMessage(Spanned message) {
        ((TextView) findViewById(R.id.message)).setText(message);
    }

}
