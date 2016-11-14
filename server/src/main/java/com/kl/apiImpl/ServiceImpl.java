package com.kl.apiImpl;


import com.kl.api.Service;
import com.kl.pojo.Student;

import java.time.LocalDateTime;

/**
 * Created by kl on 2016/10/29.
 */
public class ServiceImpl implements Service {
    @Override
    public String echo(String message) {
        System.out.println(LocalDateTime.now().toLocalTime()+"===>Service.echo被调用了");
    //   String strings=message.split("f")[10];//测试异常
        return message + "你好！";
    }
    @Override
    public Student getStudent(Student student) {
        System.out.println(LocalDateTime.now().toLocalTime()+"===>Service.getStudent被调用了");

        return new Student("chen", 19, true);
    }

}
