package org.example;

import java.util.*;

public class InMemoryDBImpl implements InMemoryDB {
    private final Map<String, Map<String, FieldValueProperties>> db;
    private final TreeMap<Integer, Map<String, Map<String, FieldValueProperties>>> backups;

    public InMemoryDBImpl() {
        this.db = new HashMap<>();
        this.backups = new TreeMap<>();
    }

    static class FieldValueProperties {
        String value;
        int timestamp;
        int ttl;

        FieldValueProperties(String value, int timestamp, int ttl) {
            this.value = value;
            this.timestamp = timestamp;
            this.ttl = ttl;
        }

        boolean isExpired(int currentTimestamp) {
            return ttl > 0 && currentTimestamp >= (timestamp + ttl);
        }

        int getRemainingTTL(int backupTimestamp) {
            return ttl > 0 ? Math.max(0, (timestamp + ttl) - backupTimestamp) : 0;
        }
    }

    @Override
    public void set(String key, String field, String value) {
        setAtWithTtl(key, field, value, 0, 0);
    }

    @Override
    public Optional<String> get(String key, String field) {
        return getAt(key, field, Integer.MAX_VALUE);
    }

    @Override
    public boolean delete(String key, String field) {
        return deleteAt(key, field, Integer.MAX_VALUE);
    }

    @Override
    public List<String> scan(String key) {
        return scanAt(key, Integer.MAX_VALUE);
    }

    @Override
    public List<String> scanAt(String key, int timestamp) {
        if (!db.containsKey(key)) {
            return Collections.emptyList();
        }

        Map<String, FieldValueProperties> fieldsMap = db.get(key);
        List<String> resultList = new ArrayList<>();

        fieldsMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isExpired(timestamp))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> resultList.add(entry.getKey() + "(" + entry.getValue().value + ")"));

        return resultList;
    }

    @Override
    public List<String> scanByPrefix(String key, String prefix) {
        return scanByPrefixAt(key, prefix, Integer.MAX_VALUE);
    }

    public List<String> scanByPrefixAt(String key, String prefix, int timestamp) {
        if (!db.containsKey(key)) {
            return Collections.emptyList();
        }

        Map<String, FieldValueProperties> fieldsMap = db.get(key);
        List<String> resultList = new ArrayList<>();

        fieldsMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix) && !entry.getValue().isExpired(timestamp))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> resultList.add(entry.getKey() + "(" + entry.getValue().value + ")"));

        return resultList;
    }

    @Override
    public void setAt(String key, String field, String value, int timestamp) {
        setAtWithTtl(key, field, value, timestamp, 0);
    }

    @Override
    public void setAtWithTtl(String key, String field, String value, int timestamp, int ttl) {
        db.computeIfAbsent(key, k -> new HashMap<>())
                .put(field, new FieldValueProperties(value, timestamp, ttl));
    }

    @Override
    public Optional<String> getAt(String key, String field, int timestamp) {
        if (!db.containsKey(key) || !db.get(key).containsKey(field)) return Optional.empty();
        FieldValueProperties fieldValueProperties = db.get(key).get(field);
        return fieldValueProperties.isExpired(timestamp) ? Optional.empty() : Optional.of(fieldValueProperties.value);
    }

    @Override
    public boolean deleteAt(String key, String field, int timestamp) {
        if (!db.containsKey(key) || !db.get(key).containsKey(field)) return false;
        if (db.get(key).get(field).isExpired(timestamp)) return false;
        db.get(key).remove(field);
        if (db.get(key).isEmpty()) db.remove(key);
        return true;
    }

    @Override
    public int backup(int timestamp) {
        Map<String, Map<String, FieldValueProperties>> backupState = new HashMap<>();
        for (var entry : db.entrySet()) {
            String key = entry.getKey();
            Map<String, FieldValueProperties> fieldMap = new HashMap<>();
            for (var fieldEntry : entry.getValue().entrySet()) {
                FieldValueProperties field = fieldEntry.getValue();
                int remainingTTL = field.getRemainingTTL(timestamp);
                if (remainingTTL > 0) {
                    fieldMap.put(fieldEntry.getKey(), new FieldValueProperties(field.value, timestamp, remainingTTL));
                }
            }
            if (!fieldMap.isEmpty()) backupState.put(key, fieldMap);
        }
        backups.put(timestamp, backupState);
        return backupState.size();
    }

    @Override
    public void restore(int timestamp, int timestampToRestore) {
        Integer closestBackup = backups.floorKey(timestampToRestore);
        if (closestBackup == null) return;
        db.clear();
        db.putAll(backups.get(closestBackup));
    }
}

