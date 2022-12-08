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
        // 通过CGLIB动态代理获取代理对象的过程
        // 创建Enhancer对象，类似于JDK动态代理的Proxy类
        Enhancer enhancer = new Enhancer();
        // 设置目标类的字节码文件
        enhancer.setSuperclass(clazz);
        // 设置回调函数
        enhancer.setCallback(new MethodInterceptor() {
            /**
             *
             * @param obj 表示要进行增强的对象
             * @param method 表示拦截的方法
             * @param objects 数组表示参数列表，基本数据类型需要传入其包装类型，如int-->Integer、long-Long、double-->Double
             * @param methodProxy 表示对方法的代理，invokeSuper方法表示对被代理对象方法的调用
             * @return 执行结果
             * @throws Throwable 异常
             */
            @Override
            public Object intercept(Object obj, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                // 注意这里是调用invokeSuper而不是invoke，否则死循环;
                // methodProxy.invokeSuper执行的是原始类的方法;
                // method.invoke执行的是子类的方法;
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
        // create方法正式创建代理类
        return (T) enhancer.create(classTypes, arguments);
    }

    public With findObjectByNumber(Supplier<String> number) {
        String fieldName = "";
        boolean requireMsg = false, requireMsgForCq = false;
        String value = number.get();
        if (this.info != null) {
            fieldName = this.info.value();
            requireMsg = this.info.requireMsg();
            requireMsgForCq = this.info.requireMsgForLocalSystem();
            this.info = null;
        }
        if (requireMsg) {
            hasLength(value, fieldName + "是必录参数");
        } else if (StringUtils.isBlank(value)) {
            return new With(null);
        }

        Object Object = FindSystemDataTest.CACHE.get(value);
        if (!requireMsgForCq && Object == null) {
            return new With(null);
        }
        notNull(Object, fieldName + "在系统未匹配到对应数据");
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
