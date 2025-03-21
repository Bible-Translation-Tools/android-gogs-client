package org.unfoldingword.gogsclient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;

/**
 * Created by joel on 3/29/2016.
 */
public class Util {

    /**
     * Adds a value to the json object if the value is not null
     * @param json
     * @param field
     * @param value
     * @return
     */
    public static JSONObject addToJSON(JSONObject json, String field, Object value) {
        if(value != null) {
            try {
                json.put(field, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    /**
     * Retrieves a value from a json object or returns the default if it does not exist
     * @param json the json object to inspect
     * @param field the field to return
     * @param defaultValue the default value to return if the field does not exist
     * @return
     */
    public static Object getFromJSON(JSONObject json, String field, Object defaultValue) {
        if(json.has(field)) {
            try {
                return json.get(field);
            } catch (JSONException e) {
                e.printStackTrace();
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Retrieves an array from a json object or returns the default if it does not exist
     * @param json the json object to inspect
     * @param field the field to return
     * @param defaultValue the default array value to return if the field does not exist
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] getArrayFromJSON(JSONObject json, String field, T[] defaultValue) {
        if(json.has(field)) {
            try {
                JSONArray arr = json.getJSONArray(field);

                if (arr.length() == 0) {
                    return defaultValue;
                }

                Class<?> componentType = arr.get(0).getClass();
                T[] result = (T[]) Array.newInstance(componentType, arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    result[i] = (T) arr.get(i);
                }
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public static <T> JSONArray getJSONFromArray(T[] array) {
        JSONArray json = new JSONArray();
        for(T t : array) {
            json.put(t);
        }
        return json;
    }
}
