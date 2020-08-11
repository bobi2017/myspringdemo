package com.spring.service.impl;

import com.spring.annotation.MyService;
import com.spring.service.TestService;

/**
 * @author yaodong.zhai
 */
@MyService
public class TestServiceImpl implements TestService {

    @Override
    public String test(){
        return "success";
    }
}
