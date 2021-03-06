package com.spring.annotation;

import java.lang.annotation.*;

/**
 * @author yaodong.zhai
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequestMapping {
    /**
     * value
     *
     * @return
     */
    String value() default "";
}
