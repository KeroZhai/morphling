package com.keroz.morphling.mapper;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class Context {

    private Map<Object, Object> cache = new IdentityHashMap<>();

    public void put(Object source, Object target) {
        cache.put(source, target);
    }

    public Object get(Object source) {
        return cache.get(source);
    }

    public void reset() {
        cache.clear();
    }

    public static class Factory {

        public static final Factory INSTANCE = new Factory();

        LinkedBlockingQueue<Context> contextQueue = new LinkedBlockingQueue<Context>();

        public Context getContext() {
            Context context = contextQueue.poll();
            if (context == null) {
                context = new Context();
            }
            return context;
        }

        public void release(Context context) {
            context.reset();
            contextQueue.offer(context);
        }

    }

}
