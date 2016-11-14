package com.kl.api;

import com.kl.pojo.Student;

/**
 * Created by kl on 2016/10/29.
 */
public interface Service {
    String echo(String message);

    Student getStudent(Student student);
}
