package com.kl.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 * Created by kl on 2016/10/27.
 */
public class MQClientProxyFactoryBean extends MQClientProxyFactory implements FactoryBean<Object> {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Override
    public void afterPropertiesSet(){
        super.afterPropertiesSet();
        if (null == this.serviceInterface || ! this.serviceInterface.isInterface()){
            throw new IllegalArgumentException("Property 'serviceInterface' is required");
        }
    }

    /**
     * 返回代理对象
     * @return
     * @throws Exception
     */
    @Override
    public Object getObject() throws Exception {
        logger.info("建立RPC客户端代理接口[{}]。", serviceInterface.getCanonicalName());
        MQClientProxy handler = new MQClientProxy(this);
        return Proxy.newProxyInstance(this.serviceInterface.getClassLoader(), new Class[]{this.serviceInterface}, handler);
    }
    @Override
    public Class<?> getObjectType() {
        return this.serviceInterface;
    }
    @Override
    public boolean isSingleton() {
        return true;
    }

}
