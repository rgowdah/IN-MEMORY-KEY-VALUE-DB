package org.example;

import java.util.List;
import java.util.Optional;

public interface InMemoryDB {
    void set(String key, String field, String value);

    Optional<String> get(String key, String field);

    boolean delete(String key, String field);

    List<String> scan(String key);

    List<String> scanAt(String key, int timestamp);

    List<String> scanByPrefix(String key, String prefix);

    void setAt(String key, String field, String value, int timestamp);

    void setAtWithTtl(String key, String field, String value, int timestamp, int ttl);

    Optional<String> getAt(String key, String field, int timestamp);

    boolean deleteAt(String key, String field, int timestamp);

    int backup(int timestamp);

    void restore(int timestamp, int timestampToRestore);
}
