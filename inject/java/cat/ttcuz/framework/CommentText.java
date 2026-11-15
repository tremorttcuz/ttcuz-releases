package cat.ttcuz.framework;

/** Read the current comment model, never a nickname or arbitrary row label. */
final class CommentText {
    private static final String MODEL = "com.ss.android.ugc.aweme.comment.model.Comment";
    private static final String CELL = "com.ss.android.ugc.aweme.commentv2.commentlist.powercell.BaseCommentCell";
    private CommentText() {}
    /** The uid of a comment's author, read from the same model as its text; null when not found. */
    /** class#name -> its no-argument method (or a miss): every comment on screen asks the same few. */
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> methods = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    private static Object call(Object target, String name) throws Exception {
        if (target == null) return null;
        String key = target.getClass().getName() + "#" + name;
        Object m = methods.get(key);
        if (m == null) {
            try { java.lang.reflect.Method found = target.getClass().getMethod(name); found.setAccessible(true); m = found; }
            catch (NoSuchMethodException missing) { m = NONE; }
            if (methods.size() < 256) methods.put(key, m);
        }
        return m == NONE ? null : ((java.lang.reflect.Method) m).invoke(target);
    }

    static String author(Object listener) {
        try {
            Object model = call(listener, "O6");
            if (model == null || !MODEL.equals(model.getClass().getName())) return null;
            Object user = call(model, "getUser");
            Object uid = call(user, "getUid");
            return uid instanceof String && !((String) uid).isEmpty() ? (String) uid : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String from(Object listener) {
        Object model;try{model=call(listener,"O6");}catch(Throwable e){model=null;}if(model!=null){String text=find(model,new java.util.IdentityHashMap<Object,Boolean>(),0);if(text!=null)return text;}
        return find(listener,new java.util.IdentityHashMap<Object,Boolean>(),0);
    }
    private static String find(Object value, java.util.IdentityHashMap<Object,Boolean> seen, int depth) {
        if (value==null || depth>4 || seen.size()>128 || seen.put(value,Boolean.TRUE)!=null) return null;
        Class<?> type=value.getClass();
        if (MODEL.equals(type.getName())) {
            try {
                Object body=call(value,"getText");
                return body instanceof String && ((String)body).trim().length()>0 ? (String)body : null;
            } catch (Throwable ignored) {return null;}
        }
        String name=type.getName();
        if (name.startsWith("java.") || name.startsWith("android.")
                || name.contains(".profile.model.User")) return null;
        for (Class<?> owner=type;owner!=null && owner!=Object.class;owner=owner.getSuperclass()) {
            if (CELL.equals(owner.getName())) {
                for (java.lang.reflect.Method method:owner.getDeclaredMethods()) {
                    if (method.getParameterTypes().length==0 && MODEL.equals(method.getReturnType().getName())) {
                        try {method.setAccessible(true);return find(method.invoke(value),seen,depth+1);}
                        catch (Throwable ignored) { }
                    }
                }
            }
            for (java.lang.reflect.Field field:owner.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    String body=find(field.get(value),seen,depth+1);
                    if (body!=null) return body;
                } catch (Throwable ignored) { }
            }
        }
        return null;
    }
}
