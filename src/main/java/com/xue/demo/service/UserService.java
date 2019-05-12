package com.xue.demo.service;

import com.xue.demo.netty.ChatMsg;
import com.xue.demo.pojo.Users;
import com.xue.demo.pojo.vo.FriendRequestVO;
import com.xue.demo.pojo.vo.MyFriendsVO;

import java.util.List;

/**
 * Created by Mingway on 2019/5/2.
 */
public interface UserService {
    /**
     * 判断用户名是否存在
     * @param username
     * @return
     */
    boolean queryUsernameIsExist(String username);

    /**
     * 查询用户是否存在
     * @param username
     * @param password
     * @return
     */
    Users queryUserForLogin(String username, String password) throws Exception;

    /**
     * 保存用户
     * @param users
     * @return
     */
    Users saveUser(Users users);

    /**
     * 更新用户
     * @param users
     */
    Users updateUserInfo(Users users);

    /**
     * 搜索好友的前置条件
     * @param myUserId
     * @param friendUsername
     * @return
     */
    Integer preConditionSearchFriends(String myUserId, String friendUsername);

    /**
     * 根据用户名，查询用户信息
     * @param username
     * @return
     */
    Users queryUserInfoByUsername(String username);

    /**
     * 添加好友请求记录
     * @param myUserId
     * @param friendUsername
     */
    void sendFriendRequest(String myUserId, String friendUsername);

    /**
     * 查询请求添加好友的信息记录
     * @param acceptUserId
     * @return
     */
    List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    /**
     * 删除请求表数据
     * @param sendUserId
     * @param acceptUserId
     */
    void deleteFriendRequest(String sendUserId, String acceptUserId);

    /**
     * 通过好友请求
     * @param sendUserId
     * @param acceptUserId
     */
    void passFriendRequest(String sendUserId, String acceptUserId);

    /**
     * 查询我的好友列表
     * @param userId
     * @return
     */
    List<MyFriendsVO> queryMyFriends(String userId);

    /**
     * 保存聊天记录
     * @param chatMsg
     * @return
     */
    String saveMsg(ChatMsg chatMsg);

    /**
     * 更新消息签收状态
     * @param msgIdList
     */
    void updateMsgSigned(List<String> msgIdList);

    /**
     * 获取当前用户所有未读消息
     * @param acceptUserId
     * @return
     */
    List<com.xue.demo.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);
}
