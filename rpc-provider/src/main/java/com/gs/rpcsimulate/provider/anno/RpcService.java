package com.gs.rpcsimulate.provider.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解作用是用于暴露服务接口
 * 先添加Target，表明该注解是放在类上的
 * 加Retention，表明注解在何时能获取到
 * 还要声明哪个服务需要对外暴露，UserServiceImpl，要在那个服务上打上注解
 */
@Target(ElementType.TYPE) //先添加Target，表明该注解是放在类上的
@Retention(RetentionPolicy.RUNTIME) // 表明注解在运行时能获取到
public @interface RpcService {
}
