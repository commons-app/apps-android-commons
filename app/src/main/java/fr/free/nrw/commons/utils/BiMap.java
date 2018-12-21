package fr.free.nrw.commons.utils;

import java.util.HashMap;
import java.util.Set;

/**
 * HashMap that can be searched in both the forward and reverse directions.
 */
public class BiMap<K, V> {

    private HashMap<K, V> map = new HashMap<>();
    private HashMap<V, K> inversedMap = new HashMap<>();

    public void put(K k, V v) {
        map.put(k, v);
        inversedMap.put(v, k);
    }

    public V get(K k) {
        return map.get(k);
    }

    public K getKey(V v) {
        return inversedMap.get(v);
    }

    public Set<V> getEntrySet(){
        return inversedMap.keySet();
    }

    public void remove(K k){
        inversedMap.remove(map.remove(k));
    }


    public boolean containsKey(V v){
        return inversedMap.containsKey(v);
    }

}

