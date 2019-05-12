
## 一、实时通信

* **Ajax 轮询** ：每个一段时间就向服务器发起ajax请求
* **Long pull** ：一通信即保持长连接，不会自动断开
* **websocket** ：基于webscoket协议的，建立连接后，服务端主动推送

## 二、WebSocket API

* 基本使用方式
```javascript
    var socket = new WebSocket("ws://[ip]:[port]");
```

* 生命周期
```javascript
    onopen():   建立连接时触发
    onmessage():    收到消息时触发
    onerror():  出现错误时触发
    onclose():  关闭连接时触发
    socket.send():  主动发送消息
    socket.close():  主动断开连接
```

## 三、SpringBoot中使用FastDFS
1. 导入jar包
```java
<dependency>
    <groupId>com.github.tobato</groupId>
    <artifactId>fastdfs-client</artifactId>
    <version>1.26.2</version>
</dependency>
```
2. 添加配置文件
```java
fdfs: # fastdfs配置
  connect-timeout: 601
  so-timeout: 1501
  thumb-image:
    height: 80
    width: 80
  tracker-list: 192.168.43.151:22122
```
3. 导入组件
```java
@Configuration
@Import(FdfsClientConfig.class)
// 解决jmx重复注册bean的问题
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
public class FastdfsImporter {
    // 导入依赖组件
}
```

## 四、心跳机制

* **没有心跳机制会出现的问题**
当客户端连接netty服务器时，会建立连接，即此时会占用一个channel，但是当客户端断开网络或者开启飞行模式时，netty不会自动断开连接。当客户端重新联网时，此时netty是重新开启一个channel，这样就会造成资源的浪费。

* **心跳机制实现**
当客户端向netty服务器建立连接时，使用自动超时机制，即当读写空闲时，自动关闭channel；而**客户端需要定时向服务端发送心跳请求**，防止连接进入读写空闲状态。

**心跳机制相关handler**
```java
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
/**
 * @Description: 用于检测channel的心跳handler 
 * 				 继承ChannelInboundHandlerAdapter，从而不需要实现channelRead0方法
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		// 判断evt是否是IdleStateEvent（用于触发用户事件，包含 读空闲/写空闲/读写空闲 ）
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent)evt;		// 强制类型转换
			if (event.state() == IdleState.READER_IDLE) {
				System.out.println("进入读空闲...");
			} else if (event.state() == IdleState.WRITER_IDLE) {
				System.out.println("进入写空闲...");
			} else if (event.state() == IdleState.ALL_IDLE) {	// 读写空闲的handler需要close
				System.out.println("channel关闭前，users的数量为：" + ChatHandler.users.size());
				Channel channel = ctx.channel();
				// 关闭无用的channel，以防资源浪费
				channel.close();
				System.out.println("channel关闭后，users的数量为：" + ChatHandler.users.size());
			}
		}
	}
}
```
该handler还需要注册：
```java
// 针对客户端，如果在1分钟时没有向服务端发送读写心跳(ALL)，则主动断开（这里为方便测试，设置为12秒）
		pipeline.addLast(new IdleStateHandler(8, 10, 12));
		// 自定义的空闲状态检测
		pipeline.addLast(new HeartBeatHandler());
```

五、SpringBoot打包成war包项目

1. pom文件修改
```java
<!--修改默认打包方式-->
<packaging>war</packaging>

<!--排除默认内置的tomcat-->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-tomcat</artifactId>
    </exclusion>
  </exclusions>
</dependency>

<!--使用外置tomcat时，需要配置该jar-->
<dependency>
  <groupId>javax.servlet</groupId>
  <artifactId>javax.servlet-api</artifactId>
  <scope>provided</scope>
</dependency>

<!--插件修改-->
<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <configuration>
        <mainClass>com.xue.demo.MuxinNettyApplication</mainClass>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>repackage</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

2. 添加war应用启动：
```java
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
```

3. 执行打包命令即可：
```java
mvn clean package
```
