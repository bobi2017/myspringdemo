package com.spring.controller;

import com.spring.annotation.MyAutowired;
import com.spring.annotation.MyController;
import com.spring.annotation.MyRequestMapping;
import com.spring.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MyController
@MyRequestMapping("/test")
public class TestController {

    @MyAutowired
    private TestService testService;

    @MyRequestMapping("/test")
    public String test(HttpServletRequest req, HttpServletResponse resp) {
        return testService.test();
    }
}
