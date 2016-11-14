package com.kl.server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.aop.SpringProxy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import com.kl.util.HessionSerializerUtil;

/**
 * Created by kl on 2016/10/28.
 */
public class MQServerEndpoint implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(MQServerEndpoint.class);

    private Class<?> serviceAPI;
    private Object serviceImpl;
    private ConnectionFactory connectionFactory;
    private SimpleMessageListenerContainer listenerContainer;
    private AmqpAdmin admin;

    private int concurentConsumers;

    private String queuePrefix;

    public MQServerEndpoint() {
        setServiceAPI(findRemoteAPI(getClass()));
        setServiceImpl(this);
    }

    public MQServerEndpoint(Object serviceImpl) {
        setServiceAPI(findRemoteAPI(serviceImpl.getClass()));
        setServiceImpl(serviceImpl);
    }

    public void setServiceAPI(Class<?> serviceAPI) {
        this.serviceAPI = serviceAPI;
    }

    public void setServiceImpl(Object serviceImpl) {
        this.serviceImpl = serviceImpl;
        setServiceAPI(findRemoteAPI(serviceImpl.getClass()));

    }

    public String getQueuePrefix() {
        return queuePrefix;
    }

    public void setQueuePrefix(String prefix) {
        queuePrefix = prefix;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public int getConcurentConsumers() {
        return concurentConsumers;
    }

    public void setConcurentConsumers(int concurentConsumers) {
        this.concurentConsumers = concurentConsumers;
    }

    public void setSendCollectionType(boolean sendType) {
      HessionSerializerUtil.getSerializerFactory().setSendCollectionType(sendType);
    }

    private Class<?> findRemoteAPI(Class<?> implClass) {
        if (implClass == null) {
            return null;
        }
        Class<?>[] interfaces = implClass.getInterfaces();
        if (interfaces.length > 0 && !interfaces[0].equals(SpringProxy.class)) {
            return interfaces[0];
        }
        return findRemoteAPI(implClass.getSuperclass());
    }

    private String getRequestQueueName(Class<?> cls) {
        String requestQueue = cls.getSimpleName();
        if (queuePrefix != null) {
            requestQueue = queuePrefix + "." + requestQueue;
        }
        return requestQueue;
    }

    private void createQueue(AmqpAdmin admin, String name) {
        Queue requestQueue = new Queue(name, false, false, false);
        admin.declareQueue(requestQueue);
    }

    public void run() {
        logger.debug("Launching endpoint for service : " + serviceAPI.getSimpleName());
        // 添加连接监听初始化队列
        connectionFactory.addConnectionListener(new ConnectionListener() {
            public void onCreate(Connection connection) {
                createQueue(admin, getRequestQueueName(serviceAPI));
            }
            public void onClose(Connection connection) {
            }

        });
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(ListenerMethod.newInstance(serviceAPI, serviceImpl));
        listenerAdapter.setMessageConverter(null);
        listenerAdapter.setMandatoryPublish(false);

        listenerContainer = new SimpleMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.setQueueNames(getRequestQueueName(serviceAPI));
        listenerContainer.setMessageListener(listenerAdapter);
        if (this.concurentConsumers > 0) {
            listenerContainer.setConcurrentConsumers(concurentConsumers);
        }
        listenerContainer.start();
    }

    public void afterPropertiesSet() throws Exception {
        if (this.connectionFactory == null) {
            throw new IllegalArgumentException("Property 'connectionFactory' is required");
        }
        this.admin = new RabbitAdmin(connectionFactory);
        this.run();
    }

    /**
     * 关闭监听容器.
     */
    @Override
    public void destroy() {
        this.listenerContainer.destroy();
    }


}
