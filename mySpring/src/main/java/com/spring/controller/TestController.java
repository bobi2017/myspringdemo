package com.spring.controller;

import com.spring.annotation.MyAutowired;
import com.spring.annotation.MyController;
import com.spring.annotation.MyRequestMapping;
import com.spring.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

@MyController
@MyRequestMapping("/test")
public class TestController {

    @MyAutowired
    private TestService testService;

    @MyRequestMapping("/test")
    public String test(HttpServletRequest req, HttpServletResponse resp) {
        return testService.test();
    }

    public static void main(String[] args) {
        List<String> list = null;
        System.out.println(getActualType(list,0));
    }
    public static String getActualType(Object o,int index) {
        Type clazz = o.getClass().getGenericSuperclass();
        ParameterizedType pt = (ParameterizedType)clazz;

        return pt.getActualTypeArguments()[index].toString();
    }
}
