package com.kl.client;

import com.kl.api.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kl on 2016/11/14.
 * Content :
 */
@Component
public class ClientTest {
    @Autowired
    Service service;

    @PostConstruct
    public void initRun(){
        System.err.println(service.echo("kl"));;
    }

    /**
     * 变态暴力测试
     * @param args
     */
    public static void main(String[] args) {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext(
                "classpath:/applicationContext-client.xml");
        Service service = (Service) context.getBean("myService");
        new ClientTest().exec(service);
    }
    public void exec(Service service){
        ExecutorService executorService= Executors.newFixedThreadPool(30);
        for(int i=0;i<=30;i++){
            executorService.submit(new Task(service));
        }
    }
    private class Task implements Callable {
        private Service service;
        public Task(Service service){
            this.service=service;
        }
        @Override
        public Object call() throws Exception {
            for(int i=0;i<=100000;i++){
                System.out.println("servicEcho当前线程："+Thread.currentThread().getName()+"| 线程任务数"+i+"| 输出："+service.echo("Hello AMQP!"));
                System.out.println("serviceStudent当前线程："+Thread.currentThread().getName()+"| 线程任务数"+i+"| 输出："+service.getStudent(null).getName());

            }
            return null;
        }
    }
}
