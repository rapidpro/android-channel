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

package io.rapidpro.androidchannel.json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Our JSON wrapper, this gives us some utilities for reading attributes out of JSON
 * without constantly having to catch exceptions.
 */
public class JSON {

    public static class JSONException extends RuntimeException {
        private static final long serialVersionUID = 748660410077342205L;
        private String m_text;

        public JSONException(Throwable t) {
            super(t);
        }

        public JSONException(Throwable t, String text) {
            super(t);
            m_text = text;
        }

        public String getText() {
            return m_text;
        }
    }

    public JSON(JSONObject object) {
        m_o = object;
    }

    public JSON() {
        m_o = new JSONObject();
    }

    public JSON(String json) {
        try {
            m_o = new JSONObject(json);
        } catch (Throwable t) {
            // Amtrak.LOG.e("Error parsing JSON.", t);
            // Amtrak.LOG.d(json);
            throw new JSONException(t, json);
        }
    }

    public String getString(String key, String def) {
        try {
            String result = getString(key);
            if (result == null) {
                return def;
            }
            return result;
        } catch (JSONException e) {
            return def;
        }
    }

    public String getString(String key) {
        try {
            String value = m_o.getString(key);
            if (value.equals("null")) {
                return null;
            }
            return value;
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public String[] getStringArray(String key) {
        try {
            JSONArray array = m_o.getJSONArray(key);
            String[] strArray = new String[array.length()];
            for (int i = 0; i < strArray.length; i++) {
                strArray[i] = array.getString(i);
            }
            return strArray;
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public String[] getStringArrayOpt(String key) {
        try {
            JSONArray array = m_o.optJSONArray(key);
            if (array != null) {
                String[] strArray = new String[array.length()];
                for (int i = 0; i < strArray.length; i++) {
                    strArray[i] = array.getString(i);
                }
                return strArray;
            }
            return null;
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public Long[] getLongArray(String key){
        try {
            JSONArray array = m_o.getJSONArray(key);
            Long[] longArray = new Long[array.length()];
            for (int i = 0; i < longArray.length; i++) {
                longArray[i] = array.getLong(i);
            }
            return longArray;
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public Integer[] getIntegerArray(String key){
        try {
            JSONArray array = m_o.getJSONArray(key);
            Integer[] integerArray = new Integer[array.length()];
            for (int i = 0; i < integerArray.length; i++) {
                integerArray[i] = array.getInt(i);
            }
            return integerArray;
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }



    public List<JSON> getJSONList(String key) {
        try {
            if (m_o.isNull(key)) {
                return new ArrayList<JSON>();
            } else {
                JSONArray array = m_o.getJSONArray(key);
                return arrayToList(array);
            }
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public int getInt(String key) {
        try {
            return m_o.getInt(key);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public int getInt(String key, int def) {
        try {
            return m_o.getInt(key);
        } catch (Throwable t) {
            return def;
        }
    }

    public long getLong(String key) {
        try {
            return m_o.getLong(key);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public double getDouble(String key) {
        try {
            return m_o.getDouble(key);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public double getDouble(String key, double def) {
        try {
            return m_o.getDouble(key);
        } catch (Throwable t) {
            return def;
        }
    }

    public BigDecimal getDecimal(String key, BigDecimal def) {
        try {
            return new BigDecimal(m_o.getString(key));
        } catch (Throwable t) {
            return def;
        }
    }

    public boolean getBoolean(String key) {
        try {
            if (m_o.getBoolean(key)) {
                return true;
            }

            if (m_o.getString(key).equals("Y")) {
                return true;
            }

            return false;

        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public boolean getBoolean(String key, boolean def) {
        try {
            return getBoolean(key);
        } catch (JSONException e) {
            return def;
        }
    }

    public Object get(String key) {
        try {
            return m_o.get(key);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public JSON getJSONOrNull(String key) {
        try {
            return getJSON(key);
        } catch (Throwable t) {
            return null;
        }
    }

    public JSON getJSON(String key) {
        try {
            return new JSON(m_o.getJSONObject(key));
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, JSONArray val) {
        try {
            m_o.put(key, val);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, long val) {
        try {
            m_o.put(key, val);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, boolean val) {
        try {
            m_o.put(key, val);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, double val) {
        try {
            m_o.put(key, val);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, String val) {
        try {
            m_o.put(key, val);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, JSON val) {
        try {
            m_o.put(key, val.getObject());
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, String[] vals) {
        try {
            JSONArray array = new JSONArray();
            for (String val : vals) {
                array.put(array.length(), val);
            }
            m_o.put(key, array);
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }

    public void put(String key, Long[] vals){
        try {
            JSONArray array = new JSONArray();
            for (Long val: vals){
                array.put(array.length(), val);
            }
            m_o.put(key, array);
        } catch (Throwable t){
            throw new JSONException(t);
        }
    }

    public void put(String key, Integer[] vals){
        try {
            JSONArray array = new JSONArray();
            for (Integer val: vals){
                array.put(array.length(), val);
            }
            m_o.put(key, array);
        } catch (Throwable t){
            throw new JSONException(t);
        }
    }

    public JSONObject getObject() {
        return m_o;
    }

    public void remove(String name) {
        m_o.remove(name);
    }

    public boolean has(String name) {
        return m_o.has(name);
    }

    public Iterator keys() {
        return m_o.keys();
    }

    public String toString() {
        return m_o.toString();
    }

    private JSONObject m_o;

    private static ArrayList arrayToList(JSONArray jsonArray) {
        try {
            ArrayList list = new ArrayList();
            for (int i = 0; i < jsonArray.length(); i++) {
                Object o = jsonArray.get(i);
                if (o instanceof JSONArray) {
                    list.add(arrayToList((JSONArray) o));
                } else if (o instanceof JSONObject) {
                    list.add(new JSON((JSONObject) o));
                } else {
                    list.add(o);
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void put(String key, BigDecimal decimal) {
        try {
            if (decimal == null) {
                m_o.put(key, null);
            } else {
                m_o.put(key, decimal.doubleValue());
            }
        } catch (Throwable t) {
            throw new JSONException(t);
        }
    }
}
