package com.xue.demo;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/** 使用外置tomcat启动应用（需要继承SpringBootServletInitializer）
 * Created by Mingway on 2019/5/12.
 */
public class WarStartApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MuxinNettyApplication.class);
    }
}
