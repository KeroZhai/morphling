package io.github.kerozhai.morphling.mapper;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.kerozhai.morphling.annotation.Mapping.ValueStrategy;
import lombok.Getter;

public class Context {

    @Getter
    private Class<?>[] ignoreGroups;

    @Getter
    private ValueStrategy valueStrategy;

    private Map<Object, Object> cache = new IdentityHashMap<>();

    public void put(Object source, Object target) {
        cache.put(source, target);
    }

    public Object get(Object source) {
        return cache.get(source);
    }

    public void init(Class<?>[] ignoreGroups, ValueStrategy valueStrategy) {
        this.ignoreGroups = ignoreGroups;
        this.valueStrategy = valueStrategy;
    }

    public void reset() {
        this.ignoreGroups = null;
        this.valueStrategy = null;
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
