package com.kl.pojo;

import java.io.Serializable;

/**
 * Created by kl on 2016/10/29.
 */
public class Student implements Serializable {
    private String name;
    private Integer age;
    private Boolean isMan;

    public Student() {
    }

    public Student(String name, Integer age, Boolean isMan) {
        this.name = name;
        this.age = age;
        this.isMan = isMan;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getMan() {
        return isMan;
    }

    public void setMan(Boolean man) {
        isMan = man;
    }
}
