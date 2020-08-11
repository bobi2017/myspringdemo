package com.spring.service;

import com.spring.annotation.MyService;

/**
 * @author yaodong.zhai
 */
@MyService
public class TestService {

    public String test(){
        return "success";
    }
}
