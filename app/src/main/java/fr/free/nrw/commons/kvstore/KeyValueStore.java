package fr.free.nrw.commons.kvstore;

public interface KeyValueStore {

  String getString(String key);

  boolean getBoolean(String key);

  long getLong(String key);

  int getInt(String key);

  String getString(String key, String defaultValue);

  boolean getBoolean(String key, boolean defaultValue);

  long getLong(String key, long defaultValue);

  int getInt(String key, int defaultValue);

  void putString(String key, String value);

  void putBoolean(String key, boolean value);

  void putLong(String key, long value);

  void putInt(String key, int value);

  boolean contains(String key);

  void remove(String key);

  void clearAll();

  void clearAllWithVersion();
}
