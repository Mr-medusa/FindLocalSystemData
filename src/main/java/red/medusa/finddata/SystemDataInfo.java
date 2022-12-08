package red.medusa.finddata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface SystemDataInfo {
    String value();

    String entityNumber() default "";

    boolean requireMsg() default false;

    boolean requireMsgForLocalSystem() default false;
}