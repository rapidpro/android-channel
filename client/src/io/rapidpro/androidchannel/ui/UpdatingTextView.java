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
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import io.rapidpro.androidchannel.R;
import io.rapidpro.androidchannel.RapidPro;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UpdatingTextView extends TextView {

    private long m_frequency = -1;
    private String m_updateMethod = null;
    private Handler m_updateHandler = new Handler();

    public UpdatingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UpdatingTextView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UpdatingTextView);

        int index = a.getIndexCount();
        for (int i = 0; i < index; ++i) {
            int attr = a.getIndex(i);
            switch (attr){
                case R.styleable.UpdatingTextView_onUpdateText:
                    m_updateMethod = a.getString(attr);
                    break;
                case R.styleable.UpdatingTextView_updateFrequency:
                    m_frequency = a.getInt(attr, -1);
                    break;
            }
        }
        a.recycle();

        updateText();
    }

    private Runnable m_timer = new Runnable() {
        @Override
        public void run() {
            updateText();
            if (isShown()) {
                m_updateHandler.postDelayed(this, m_frequency);
            }
        }
    };

    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        m_updateHandler.removeCallbacks(m_timer);
        if (visibility == VISIBLE) {
            startUpdating();
        }
    }

    public void startUpdating() {
        if (m_frequency != -1) {
            m_updateHandler.postDelayed(m_timer, m_frequency);
        }
    }

    protected void updateText() {
        if (m_updateMethod != null) {
            try {
                Method method = getContext().getClass().getMethod(m_updateMethod, UpdatingTextView.class);
                method.invoke(getContext(), this);
            } catch (Throwable t) {
                RapidPro.LOG.e("Error executing handler: " + m_updateMethod, t);
            }
        }
    }
}
