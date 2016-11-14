
package com.kl.client;

import com.kl.util.HessionSerializerUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeoutException;


/**
 * Created by kl on 2016/10/27.
 */
public class MQClientProxy implements InvocationHandler {
    private MQClientProxyFactory _factory;
    MQClientProxy(MQClientProxyFactory factory) {
        _factory = factory;
    }
    /**
     * 执行代理方法,发送消息到QueruyName为服务名的队列
     * @throws Throwable
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ReflectionUtils.isEqualsMethod(method)) {
            Object value = args[0];
            if (value == null || !Proxy.isProxyClass(value.getClass()))return Boolean.FALSE;
            MQClientProxy handler = (MQClientProxy) Proxy.getInvocationHandler(value);
            return _factory.equals(handler._factory);
        } else if (ReflectionUtils.isHashCodeMethod(method)) {
            return _factory.hashCode();
        } else if (ReflectionUtils.isToStringMethod(method)) {
            return "[HessianProxy " + proxy.getClass() + "]";
        }

        RabbitTemplate template = this._factory.getTemplate();
        byte[] payload = HessionSerializerUtil.clientRequestBody(method, args,_factory.isCompressed());

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("x-application/hessian");
        messageProperties.setContentEncoding("deflate");
        Message message = new Message(payload, messageProperties);
        Message response = template.sendAndReceive(
                _factory.getRequestExchangeName(),
                _factory.getRequestQueueName(),
                message);
        if (response == null)throw new TimeoutException("RPC服务响应超时");
        MessageProperties props = response.getMessageProperties();
        boolean compressed = "deflate".equals(props.getContentEncoding());//是否使用defate算法压缩消息

        return HessionSerializerUtil.clientResponseBody(response.getBody(),method,compressed);
    }

}
