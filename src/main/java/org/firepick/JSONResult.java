package org.firepick;

import org.json.simple.parser.*;
import org.json.simple.*;

/**
 * Fluent wrapper for JSON result returned by FireREST
 */
public class JSONResult {
  Object value;

  protected JSONResult(Object value) {
    this.value = value;
  }

  /**
   * Parse given JSON string and set current JSON value
   */
  public JSONResult(String json) {
    try {
      JSONParser parser = new JSONParser();
      value = (JSONObject) parser.parse(json);
    } catch (Exception e) {
      throw new FireRESTException("Could not parse: " + json, e);
    }
  }

  /**
   * Advance current JSON value to specified element in JSON array.
   * Set current JSON value to null otherwise.
   * 
   * @param index zero-based JSON array index
   * @return JSONResult for fluent method chaining
   */
  public JSONResult get(int index) {
    if (value instanceof JSONArray) {
      JSONArray array = (JSONArray) value;
      Object result = array.get(index);
      return new JSONResult(result);
    }
    return new JSONResult(null);
  }

  /**
   * Advance current JSON value to specified value of current JSON object.
   * Set current JSON value to null otherwise.
   * 
   * @param key element key
   * @return JSONResult for fluent method chaining
   */
  public JSONResult get(String key) {
    if (value instanceof JSONObject) {
      JSONObject obj = (JSONObject) value;
      Object result = obj.get(key);
      return new JSONResult(result);
    }
    return new JSONResult(null);
  }

  private FireRESTException createException(String msg) {
    if (value instanceof JSONArray) {
      return new FireRESTException(msg + ((JSONArray) value).toJSONString());
    }
    if (value instanceof JSONObject) {
      return new FireRESTException(msg + ((JSONObject) value).toJSONString());
    }
    if (value == null) {
      return new FireRESTException(msg + "null");
    }
    throw new FireRESTException(msg + value.toString());
  }

  /**
   * Return false if current element exists
   */
  public boolean isNull() {
    return value == null;
  }

  /**
   * Return an integer for current JSON value, parsing string values as required.
   */
  public Integer getInt() {
    return getInt(null);
  }

  /**
   * Return an integer for current JSON value, parsing string values as required.
   */
  public Integer getInt(Integer defaultValue) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      String s = (String) value;
      return Integer.parseInt(s);
    }
    if (value == null) {
      return defaultValue;
    }
    throw createException("Expected integer:");
  }

  /**
   * Return a double number for current JSON value, parsing string values as required.
   */
  public Double getDouble() {
    return getDouble(null);
  }

  /**
   * Return a double number for current JSON value, parsing string values as required.
   */
  public Double getDouble(Double defaultValue) {
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    if (value instanceof String) {
      String s = (String) value;
      return Double.parseDouble(s);
    }
    if (value == null) {
      return defaultValue;
    }
    throw createException("Expected number:");
  }

  /**
   * Return string value for current JSON value
   */
  public String getString() {
    return getString(null);
  }

  /**
   * Return string value for current JSON value
   */
  public String getString(String defaultValue) {
    if (value instanceof String || value instanceof Number) {
      return value.toString();
    }
    if (value == null) {
      return null;
    }
    if (value instanceof JSONArray) {
      return ((JSONArray) value).toJSONString();
    }
    if (value instanceof JSONObject) {
      return ((JSONObject) value).toJSONString();
    }
    if (value == null) {
      return defaultValue;
    }
    throw createException("Expected string:");
  }

}
