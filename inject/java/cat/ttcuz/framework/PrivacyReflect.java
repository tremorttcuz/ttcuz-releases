package cat.ttcuz.framework;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class PrivacyReflect {
    PrivacyReflect() {
    }

    static Class<?> type(String str) throws Exception {
        return Class.forName(str, false, PrivacyReflect.class.getClassLoader());
    }

    static Field field(Class<?> cls, String str) throws Exception {
        while (cls != null) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException unused) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(str);
    }

    static Object get(Object obj, String str) throws Exception {
        return field(obj.getClass(), str).get(obj);
    }

    static Object stat(String str, String str2) throws Exception {
        return field(type(str), str2).get(null);
    }

    static Object call(Object obj, String str) throws Exception {
        return obj.getClass().getMethod(str, null).invoke(obj, null);
    }

    static Object invoke(Method method, Object obj, Object[] objArr) throws Throwable {
        try {
            return method.invoke(obj, objArr);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static final class Patch {
        final Field field;
        final Object owner;
        final Object previous;
        final Object replacement;

        Patch(Object obj, Field field, Object obj2) throws Exception {
            this(obj, field, obj2, field.get(obj));
        }

        Patch(Object obj, Field field, Object obj2, Object obj3) throws Exception {
            this.owner = obj;
            this.field = field;
            this.previous = obj3;
            this.replacement = obj2;
            field.set(obj, obj2);
            if (field.get(obj) == obj2) {
                return;
            }
            throw new IllegalStateException("write rejected: " + field.getName());
        }

        void restore() throws Exception {
            if (this.field.get(this.owner) == this.replacement) {
                this.field.set(this.owner, this.previous);
            }
        }
    }
}
