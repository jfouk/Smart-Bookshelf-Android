package com.jonfouk.bookshelf.simplehttp;

import android.util.Log;

import java.util.Map;

/**
 * Handles HTTP operations asynchronously.
 * @author <a href="https://github.com/joecheatham">joecheatham</a>
 * @version 1.0
 */
public class SimpleHttp {
  public static final String TAG = "SimpleHttp";
  public static void get(String url, SimpleHttpResponseHandler callback) {
    Log.d(TAG, "GET called to: " + url);
    new HttpGetTask(callback).execute(url);
  }

  public static void post(String url, Map<String, String> parameters, SimpleHttpResponseHandler
      callback) {
    Log.d(TAG, "POST called to: " + url);
    new HttpPostTask(parameters, callback).execute(url);
  }
}
