package com.xue.demo.netty;

import com.xue.demo.SpringUtil;
import com.xue.demo.enums.MsgActionEnum;
import com.xue.demo.service.UserService;
import com.xue.demo.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/** 自定义handler
 *  其中的泛型使用：
 *      http -> HttpObject
 *      websocket -> TextWebSocketFrame（该frame在netty中是专门处理文本的对象载体）
 * Created by Mingway on 2019/5/1.
 */
@Slf4j
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 用于记录和管理所有的客户端channel
    public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 接受及返回消息处理方法
     * @param channelHandlerContext
     * @param textWebSocketFrame
     * @throws Exception
     */
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel currenChannel = channelHandlerContext.channel();

        // 1.获取客户端传输过来的消息  // String content = textWebSocketFrame.text();
        DataContent dataContent = JsonUtils.jsonToPojo(textWebSocketFrame.text(), DataContent.class);
        Integer action = dataContent.getAction();

        // 2.判断消息类型，根据不同的消息类型处理不同的业务

        if (action == MsgActionEnum.CONNECT.type) {
            //  2.1 当第一次websocket在前端open时，初始化channel，并将channel和userid一对一关联
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId, currenChannel);

        } else if (action == MsgActionEnum.CHAT.type) {
            //  2.2 聊天类型的消息，把聊天记录存入数据库中，同时标记消息的签收状态
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgText = chatMsg.getMsg();
            String senderId = chatMsg.getSenderId();
            String receiverId = chatMsg.getReceiverId();

            // 保存信息到数据库
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);    // 用户客户端签收

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);

            // 发送消息给客户端
            // 从全局用户Channel关系中获取接受方的channel
            Channel receiverChannel = UserChannelRel.get(receiverId);
            if (receiverChannel == null) {
                // TODO 表示用户已离线，一般使用第三方平台推送消息
            } else {
                // 当receiverChannel不为null时，需要从ChannelGroup中判断channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null) {
                    // 用户在线
                    receiverChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContentMsg)));
                } else {
                    // TODO 用户离线，推送
                }
            }

        } else if (action == MsgActionEnum.SIGNED.type) {
            //  2.3 签收消息类型，即修改数据库中消息的签收状态（这里的签收指的是客户端接收到服务端的消息后，响应的签收）
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            // 拓展字段在此代表需要去签收的消息id，使用","分隔
            String msgIdsStr = dataContent.getExtand();
            String[] msgIds = msgIdsStr.split(",");
            List<String> msgIdsList = new ArrayList<>();
            for (String msgId : msgIds) {
                if (StringUtils.isNotBlank(msgId)) {
                    msgIdsList.add(msgId);
                }
            }
            System.out.println(msgIdsList.toString());

            if (msgIdsList.size() > 0 && !msgIdsList.isEmpty()) {
                // 批量签收
                userService.updateMsgSigned(msgIdsList);
            }

        } else if (action == MsgActionEnum.KEEPALIVE.type) {
            //  2.4 心跳类型的消息 TODO
            System.out.println("收到来自channel为[" + currenChannel + "]的心跳包...");
        }


    }

    /**
     * 当客户端获取连接后，此时获取客户端的channel，并把它放在ChannelGroup管理
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        users.add(ctx.channel());
    }

    /**
     * 客户端断开连接后的操作
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // 下列代码可注释，因为当触发handlerRemoved时，ChannelGroup会自动移除
        users.remove(ctx.channel());
//        log.info("客户端断开，channel对应的长id：" + ctx.channel().id().asLongText());
//        log.info("客户端断开，channel对应的短id：" + ctx.channel().id().asShortText());
    }

    /**
     * 发生异常时的操作
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 打印异常信息
        cause.printStackTrace();

        // 发送异常后，关闭连接，并将channel在ChannelGroup中移除
        ctx.channel().close();
        users.remove(ctx.channel());
    }
}
