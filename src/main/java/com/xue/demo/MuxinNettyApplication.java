package com.xue.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
// 扫描mybatis mapper文件路径（注意，这里使用的是：tk.mybatis.spring.annotation.MapperScan）
@MapperScan(basePackages = {"com.xue.demo.mapper"})
@ComponentScan(basePackages = {"com.xue", "org.n3r.idworker"})
public class MuxinNettyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MuxinNettyApplication.class, args);
	}

	/**
	 * 向spring容器中注入该工具类
	 * @return
	 */
	@Bean
	public SpringUtil springUtil() {
		return new SpringUtil();
	}
}
