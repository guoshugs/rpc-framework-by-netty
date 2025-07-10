package com.gs.rpcsimulate.consumer.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/*
代理类开发好之后，再使用的时候是使用注解的，所以要开发注解，并在具体的使用的类上，打上注解
 */
/**
 * 引用代理类
 */
@Target(ElementType.FIELD) // 作用于字段
@Retention(RetentionPolicy.RUNTIME) // 在运行时可以获取到
public @interface RpcClientProxyReference {//客户端代理的引用
}
