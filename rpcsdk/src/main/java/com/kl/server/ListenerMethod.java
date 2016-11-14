package com.kl.server;

import com.kl.util.HessionSerializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Created by kl on 2016/10/28.
 */
public class ListenerMethod {

    private static final Logger logger = LoggerFactory.getLogger(ListenerMethod.class);

    private static String SPRING_CORRELATION_ID = "spring_reply_correlation";

    private Class<?> serviceAPI;
    private Object serviceImpl;

    private ListenerMethod(Class<?> serviceAPI, Object serviceImpl) {
        this.serviceAPI = serviceAPI;
        this.serviceImpl = serviceImpl;
    }

    public static ListenerMethod newInstance(Class<?> serviceAPI, Object serviceImpl ) {
        return new ListenerMethod(serviceAPI,serviceImpl);
    }

    /**
     * handleMessage为MessageListenerAdapter默认的消息处理器
     * @param message
     * @return
     */
    public Message handleMessage(Message message) {
        System.out.println("调用-服务名："+serviceImpl.getClass().getName());
        logger.debug("Message received : " + message);

        MessageProperties props = message.getMessageProperties();
        boolean compressed = "deflate".equals(props.getContentEncoding());

        byte[] response;
        try {
            response = HessionSerializerUtil.serverResponseBody(message.getBody(),compressed,serviceImpl,serviceAPI);
        } catch (Throwable e) {
            logger.error("Exception occurs during method call", e);
            e.printStackTrace();
            compressed = false;
            response = HessionSerializerUtil.serverFaultBody(message.getBody(), e);
        }

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("x-application/hessian");
        // Spring correlation ID
        messageProperties.setHeader(SPRING_CORRELATION_ID,
                message.getMessageProperties().getHeaders().get(SPRING_CORRELATION_ID));
        if (compressed)  messageProperties.setContentEncoding("deflate");
        return new Message(response, messageProperties);
    }
}
