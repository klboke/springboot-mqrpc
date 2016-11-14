package com.kl.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kl on 2016/10/27.
 */
public class MQClientProxyFactory implements InitializingBean {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private ConnectionFactory connectionFactory;
    private RabbitTemplate template;
    private RabbitAdmin admin;

    private AtomicBoolean initializing = new AtomicBoolean(false);//保证原子操作

    protected Class<?> serviceInterface;

    private String queuePrefix; //队列name前缀

    private long readTimeout = -1; //响应超时 不设置默认5000ms

    private boolean compressed = true;//是否压缩消息 默认压缩

    public MQClientProxyFactory() {
    }

    /**
     * 创建一个请求队列和设置exchange处理机制为Direct：单播-完全匹配
     *
     * @param queueName    the name of the queue
     * @param exchangeName the name of the exchange
     */
    private void createRequestQueue(AmqpAdmin admin, String queueName, String exchangeName) {
        Queue requestQueue = new Queue(queueName, false, false, false);
        admin.declareQueue(requestQueue);
        DirectExchange requestExchange = new DirectExchange(exchangeName, false, false);
        admin.declareExchange(requestExchange);
        Binding requestBinding = BindingBuilder.bind(requestQueue).to(requestExchange).with(queueName);
        admin.declareBinding(requestBinding);
    }

    /**
     * 返回请求交换机服务的名称
     * @return
     */
    public String getRequestExchangeName() {
        String requestExchange = serviceInterface.getSimpleName();
        if (this.queuePrefix != null) {
            requestExchange = this.queuePrefix + "." + requestExchange;
        }
        return requestExchange;
    }

    /**
     * 返回服务的请求队列的名称.
     * @return
     */
    public String getRequestQueueName() {
        String requestQueue = serviceInterface.getSimpleName();
        if (this.queuePrefix != null) {
            requestQueue = this.queuePrefix + "." + requestQueue;
        }
        return requestQueue;
    }

    /**
     * 初始化队列
     */
    private void initializeQueues() {
        //使用AtomicBoolean原子类保证 只初始化一次
        if (!initializing.compareAndSet(false, true)) return;
        try {
            if (admin == null) admin = new RabbitAdmin(connectionFactory);
            this.createRequestQueue(admin, this.getRequestQueueName(), this.getRequestExchangeName());
        } finally {
            initializing.compareAndSet(true, false);
        }
    }

    public void afterPropertiesSet() {
        if (this.connectionFactory == null) {
            throw new IllegalArgumentException("Property 'connectionFactory' is required");
        }
        this.template = new RabbitTemplate(this.connectionFactory);
        if (this.readTimeout > 0) {
            this.template.setReplyTimeout(readTimeout);
            logger.debug("配置RPC消息的响应超时时间为{}", readTimeout);
        }
        this.admin = new RabbitAdmin(connectionFactory);
        this.initializeQueues();
    }

    public String getQueuePrefix() {
        return queuePrefix;
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long timeout) {
        readTimeout = timeout;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public RabbitTemplate getTemplate() {
        return template;
    }

    public Class<?> getServiceInterface() {
        return this.serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        if (null == serviceInterface || !serviceInterface.isInterface()) {
            throw new IllegalArgumentException("'serviceInterface' is null or is not an interface");
        }
        this.serviceInterface = serviceInterface;
    }
}

