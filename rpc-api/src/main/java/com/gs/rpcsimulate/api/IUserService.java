package com.gs.rpcsimulate.api;

import com.gs.rpcsimulate.pojo.User;

public interface IUserService {
    User getById(int id);
}
