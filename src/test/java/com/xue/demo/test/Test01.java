package com.xue.demo.test;

import org.junit.Test;

import java.io.File;

/**
 * Created by Mingway on 2019/5/6.
 */
public class Test01 {

    @Test
    public void test01() {
        String url = "c:\\190502HHTGMN7X1Puserface64.png";
        File file = new File(url);
        System.out.println(file);
    }
}
