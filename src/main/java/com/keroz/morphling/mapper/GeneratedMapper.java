package com.keroz.morphling.mapper;

public abstract class GeneratedMapper implements Mapper<Object, Object> {

    public abstract void map(Object source, Object target, Class<?>[] ignoreGroups, Context context);

    public abstract Object instantiate(Object source);

    public Object map(Object source, Class<?>[] ignoreGroups, Context context) {
        Object target = context.get(source);

        if (target == null) {
            target = instantiate(source);
            context.put(source, target);
            map(source, target, ignoreGroups, context);
        }

        return target;
    }

    @Override
    public Object map(Object source, Class<?>... ignoreGroups) {
        Context context = Context.Factory.INSTANCE.getContext();

        Object target = map(source, ignoreGroups, context);

        Context.Factory.INSTANCE.release(context);

        return target;
    }

    @Override
    public void map(Object source, Object target, Class<?>... ignoreGroups) {
        Context context = Context.Factory.INSTANCE.getContext();
        map(source, target, ignoreGroups, context);
        Context.Factory.INSTANCE.release(context);
    }

    @Override
    public Object map(Object source) {
        return map(source, (Class<?>[]) null);
    }

    @Override
    public void map(Object source, Object target) {
        map(source, target, (Class<?>[]) null);
    }

}
