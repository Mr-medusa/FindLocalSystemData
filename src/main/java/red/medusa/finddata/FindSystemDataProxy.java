package red.medusa.finddata;


import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import red.medusa.pojo.FindSystemDataTest;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class FindSystemDataProxy {
    static Map<Class<?>, Map<Method, SystemDataInfo>> CLASS_TO_METHOD_FIELD = new ConcurrentHashMap<>();
    private final Class<?> clazz;
    private SystemDataInfo info;

    public FindSystemDataProxy(Class<?> clazz) {
        this.clazz = clazz;
        if (!CLASS_TO_METHOD_FIELD.containsKey(clazz)) {
            try {
                Map<Method, SystemDataInfo> methodToField = new HashMap<>();
                BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    try {
                        Field declaredField = clazz.getDeclaredField(propertyDescriptor.getName());
                        SystemDataInfo annotation = declaredField.getAnnotation(SystemDataInfo.class);
                        methodToField.put(propertyDescriptor.getReadMethod(), annotation);
                    } catch (NoSuchFieldException ignore) {
                    }
                }
                CLASS_TO_METHOD_FIELD.put(clazz, methodToField);
            } catch (IntrospectionException e) {
                e.printStackTrace();
            }
        }
    }

    public <T> T from(Object src) {
        T proxy = newInstance();
        try {
            BeanUtils.copyProperties(proxy, src);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return proxy;
    }

    public <T> T newInstance(Object... arguments) {
        // ??????CGLIB???????????????????????????????????????
        // ??????Enhancer??????????????????JDK???????????????Proxy???
        Enhancer enhancer = new Enhancer();
        // ?????????????????????????????????
        enhancer.setSuperclass(clazz);
        // ??????????????????
        enhancer.setCallback(new MethodInterceptor() {
            /**
             *
             * @param obj ??????????????????????????????
             * @param method ?????????????????????
             * @param objects ??????????????????????????????????????????????????????????????????????????????int-->Integer???long-Long???double-->Double
             * @param methodProxy ???????????????????????????invokeSuper?????????????????????????????????????????????
             * @return ????????????
             * @throws Throwable ??????
             */
            @Override
            public Object intercept(Object obj, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                // ?????????????????????invokeSuper?????????invoke??????????????????;
                // methodProxy.invokeSuper??????????????????????????????;
                // method.invoke???????????????????????????;
                Object result = methodProxy.invokeSuper(obj, objects);
                after(method);
                return result;
            }

            private void after(Method method) {
                if (CLASS_TO_METHOD_FIELD.get(clazz).containsKey(method)) {
                    info = CLASS_TO_METHOD_FIELD.get(clazz).get(method);
                }
            }
        });
        Class<?>[] classTypes = new Class[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            classTypes[i] = arguments[i].getClass();
        }
        // create???????????????????????????
        return (T) enhancer.create(classTypes, arguments);
    }

    public With findObjectByNumber(Supplier<String> number) {
        String fieldName = "";
        boolean requireMsg = false, requireMsgForCq = false;
        String value = number.get();
        String entityNumber = "";
        if (this.info != null) {
            fieldName = this.info.value();
            requireMsg = this.info.requireMsg();
            requireMsgForCq = this.info.requireMsgForLocalSystem();
            entityNumber = this.info.entityNumber();
            this.info = null;
        }
        if (requireMsg) {
            hasLength(value, fieldName + "???????????????");
        } else if (StringUtils.isBlank(value)) {
            return new With(null);
        }

        Object Object = FindSystemDataTest.CACHE.get(value);
        if (!requireMsgForCq && Object == null) {
            return new With(null);
        }
        notNull(Object, fieldName + "?????????????????????????????????");
        return new With(Object);
    }

    @FunctionalInterface
    public interface FindSystemDataProxyConsumer<T> {
        void accept(T t) throws Exception;
    }

    public static class With {
        public final Object value;

        public With(Object value) {
            this.value = value;
        }

        public void withNotNull(FindSystemDataProxyConsumer<Object> consumer) throws Exception {
            if (value != null) {
                consumer.accept(value);
            }
        }

        public void with(FindSystemDataProxyConsumer<Object> consumer) throws Exception {
            consumer.accept(value);
        }
    }

    // --- assert
    public static void hasLength(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
