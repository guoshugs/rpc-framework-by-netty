package com.gs.rpcsimulate.provider.service;

import com.gs.rpcsimulate.api.IUserService;
import com.gs.rpcsimulate.pojo.User;
import com.gs.rpcsimulate.provider.anno.RpcService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@RpcService // 标识该服务是对外暴露的一个服务的接口
@Service
public class UserServiceImpl implements IUserService {

    Map<Object, User> userMap = new HashMap<>();

    @Override
    public User getById(int id) {
        if (userMap.size() == 0) {
            User user1 = new User();
            user1.setId(1);
            user1.setName("张三");
            User user2 = new User();
            user2.setId(2);
            user2.setName("李四");
            userMap.put(user1.getId(), user1);
            userMap.put(user2.getId(), user2);
        }
        return userMap.get(id);
    }
}
