package org.sirenia.agent;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LastModCache {
    private ConcurrentHashMap<String,CacheObj> cache = new ConcurrentHashMap<>();

    public static class CacheObj{
        public long lastMod;
        public Object obj;
    }
    public interface OnExpire{
        Object apply(Object prev) throws Exception;
    }
    public Object get(String filename,OnExpire onExpire) throws Exception {
        File file = new File(filename);
        long lastMod = file.lastModified();
        CacheObj cacheObj = cache.get(filename);
        if(cacheObj == null || cacheObj.lastMod < lastMod){
            cacheObj = new CacheObj();
            cacheObj.obj = onExpire.apply(cacheObj);
            cacheObj.lastMod = lastMod;
            cache.put(filename,cacheObj);
        }
        return cacheObj.obj;
    }
    public void clear(){
        cache.clear();
    }
}