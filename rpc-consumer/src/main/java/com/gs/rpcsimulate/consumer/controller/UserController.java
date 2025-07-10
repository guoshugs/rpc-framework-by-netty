package com.gs.rpcsimulate.consumer.controller;

import com.gs.rpcsimulate.api.IUserService;
import com.gs.rpcsimulate.consumer.anno.RpcClientProxyReference;
import com.gs.rpcsimulate.pojo.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @RpcClientProxyReference // 这仅是标注userSerice使用了代理类的引用，但还没有把代理真正注入进来，所以还要让该注解有打上引用自动注入代理类的功能
    IUserService userService;

    @RequestMapping("/getUserById")
    public User getUserById(int id) {
        return userService.getById(id);
    }
}
