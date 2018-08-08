package fr.free.nrw.commons.utils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by Ilgaz Er on 8/8/2018.
 */
public class BiMap<K, V> {

    HashMap<K, V> map = new HashMap<K, V>();
    HashMap<V, K> inversedMap = new HashMap<V, K>();

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

    public boolean hasKey(V v){
        return inversedMap.containsKey(v);
    }

}

