package io.github.kerozhai.morphling.mapper;

import io.github.kerozhai.morphling.annotation.Mapping.ValueStrategy;

public abstract class GeneratedMapper implements Mapper<Object, Object> {

    protected final MapperFactory mapperFactory;

    public GeneratedMapper(MapperFactory mapperFactory) {
        this.mapperFactory = mapperFactory;
    }

    public void map(Object source, Object target, Context context) {
        if (source != null) {
            if (context.get(source) == target) {
                // already mapped
                return;
            }

            context.put(source, target);
        }

        doMap(source, target, context);
    }

    public Object map(Object source, Context context) {
        Object target = null;

        if (source != null) {
            target = context.get(source);
        }

        if (target == null) {
            target = instantiate(source);
            map(source, target, context);
        }

        return target;
    }

    @Override
    public Object map(Object source, ValueStrategy valueStrategy, Class<?>... ignoreGroups) {
        Context context = Context.Factory.INSTANCE.getContext();

        context.init(ignoreGroups, valueStrategy);

        Object target = map(source, context);

        Context.Factory.INSTANCE.release(context);

        return target;
    }

    @Override
    public void map(Object source, Object target, ValueStrategy valueStrategy, Class<?>... ignoreGroups) {
        Context context = Context.Factory.INSTANCE.getContext();
        context.init(ignoreGroups, valueStrategy);

        map(source, target, context);

        Context.Factory.INSTANCE.release(context);
    }

    @Override
    public Object map(Object source, Class<?>... ignoreGroups) {
        return map(source, ValueStrategy.DEFAULT, ignoreGroups);
    }

    @Override
    public void map(Object source, Object target, Class<?>... ignoreGroups) {
        map(source, target, ValueStrategy.DEFAULT, ignoreGroups);
    }

    @Override
    public Object map(Object source) {
        return map(source, (Class<?>[]) null);
    }

    @Override
    public void map(Object source, Object target) {
        map(source, target, (Class<?>[]) null);
    }

    protected abstract void doMap(Object source, Object target, Context context);

    protected abstract Object instantiate(Object source);

}
