package com.xue.demo.controller;

import com.xue.demo.enums.OperatorFriendRequestTypeEnum;
import com.xue.demo.enums.SearchFriendsStatusEnum;
import com.xue.demo.pojo.ChatMsg;
import com.xue.demo.pojo.Users;
import com.xue.demo.pojo.bo.UsersBO;
import com.xue.demo.pojo.vo.MyFriendsVO;
import com.xue.demo.pojo.vo.UsersVO;
import com.xue.demo.service.UserService;
import com.xue.demo.utils.FastDFSClient;
import com.xue.demo.utils.FileUtils;
import com.xue.demo.utils.IMoocJSONResult;
import com.xue.demo.utils.MD5Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by Mingway on 2019/5/2.
 */
@RestController
@RequestMapping("u")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("registOrLogin")
    public IMoocJSONResult registOrLogin(@RequestBody Users users) throws Exception {
        // 1.用户名/密码不能为空
        if (StringUtils.isBlank(users.getUsername()) || StringUtils.isBlank(users.getPassword())) {
            return IMoocJSONResult.errorMsg("用户名或密码不能为空");
        }

        // 2.判断用户名是否存在，存在则登录，不存在则注册
        boolean usernameIsExist = userService.queryUsernameIsExist(users.getUsername());
        Users u = null;
        if (usernameIsExist) {
            // 2.1 登录
            u = userService.queryUserForLogin(users.getUsername(), users.getPassword());
            if (u == null) {
                return IMoocJSONResult.errorMsg("用户名或密码错误...");
            }
        } else {
            // 2.2 注册
            users.setNickname(users.getUsername());
            users.setFaceImage("");
            users.setFaceImageBig("");
            users.setPassword(MD5Utils.getMD5Str(users.getPassword()));
            u = userService.saveUser(users);
        }

        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(u, usersVO);   // 复制bean信息

        return IMoocJSONResult.ok(usersVO);
    }

    /**
     * 上传头像
     * @param usersBO
     * @return
     * @throws Exception
     */
    @PostMapping("uploadFaceBase64")
    public IMoocJSONResult uploadFaceBase64(@RequestBody UsersBO usersBO) throws Exception {
        // 获取前端传过来的base64字符串，然后转换成文件对象再上传
        String base64Data = usersBO.getFaceData();
        String userFacePath = "d:\\" + usersBO.getUserId() + "userface64.png";
        FileUtils.base64ToFile(userFacePath, base64Data);

        // 上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        log.info("userface_url:" + url);

        /**
         * "aweadsadasdas.png"
         * "aweadsadasdas_80x80.png"
         */
        // 获取缩略图url
//        String thump = "_80x80";
        String[] arr = url.split("\\.");
        String thumpImgUrl = arr[0] + "_80x80." + arr[1];

        // 更新用户头像
        Users users = new Users();
        users.setId(usersBO.getUserId());
        users.setFaceImage(thumpImgUrl);
        users.setFaceImageBig(url);

        Users result = userService.updateUserInfo(users);

        return IMoocJSONResult.ok(result);
    }

    /**
     * 设置昵称
     * @param usersBO
     * @return
     * @throws Exception
     */
    @PostMapping("setNickname")
    public IMoocJSONResult setNickname(@RequestBody UsersBO usersBO) throws Exception {
        Users users = new Users();
        users.setId(usersBO.getUserId());
        users.setNickname(usersBO.getNickname());

        Users result = userService.updateUserInfo(users);
        return IMoocJSONResult.ok(result);
    }

    /**
     * 搜索好友
     * @param myUserId
     * @param friendUsername
     * @return
     */
    @PostMapping("search")
    public IMoocJSONResult searchUser(String myUserId, String friendUsername) {
        // 1.myUserId friendUsername 不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }

        /**
         * 在此应该做匹配查询（根据条件），而不是模糊查询
         * 前置条件：
         *  1）搜索的用户不存在，则返回【无此用户】
         *  2）搜索的用户为自己，则返回【不能添加自己】
         *  3）搜索的用户已经为好友，则返回【该用户已经是你的好友】
         */
        Integer status = userService.preConditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users users = userService.queryUserInfoByUsername(friendUsername);
            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(users, usersVO);
            return IMoocJSONResult.ok(usersVO);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
    }

    /**
     * 添加好友
     * @param myUserId
     * @param friendUsername
     * @return
     */
    @PostMapping("addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId, String friendUsername) {
        // 1.myUserId friendUsername 不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }

        /**
         * 在此应该做匹配查询（根据条件），而不是模糊查询
         * 前置条件：
         *  1）搜索的用户不存在，则返回【无此用户】
         *  2）搜索的用户为自己，则返回【不能添加自己】
         *  3）搜索的用户已经为好友，则返回【该用户已经是你的好友】
         */
        Integer status = userService.preConditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
        return IMoocJSONResult.ok();
    }

    /**
     * 查询添加好友记录表
     * @param userId
     * @return
     */
    @PostMapping("queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId) {
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    /**
     * 接收方  接受/忽略添加好友请求
     * @param acceptUserId
     * @param sendUserId
     * @param operType
     * @return
     */
    @PostMapping("operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId, String sendUserId, Integer operType) {
        if (StringUtils.isBlank(acceptUserId) || StringUtils.isBlank(sendUserId) || operType == null) {
            return IMoocJSONResult.errorMsg("");
        }

        // 操作类型对应枚举值为空，直接返回空错误
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IMoocJSONResult.errorMsg("");
        }

        if (operType.equals(OperatorFriendRequestTypeEnum.IGNORE.type)) {
            // 忽略请求，直接删除请求表数据
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        } else if (operType.equals(OperatorFriendRequestTypeEnum.PASS.type)) {
            // 通过请求，则各自添加好友记录，最后删除请求表数据
            userService.passFriendRequest(sendUserId, acceptUserId);
        }

        List<MyFriendsVO> myFriendsVOList = userService.queryMyFriends(acceptUserId);
        return IMoocJSONResult.ok(myFriendsVOList);
    }

    /**
     * 查询我的好友列表
     * @param userId
     * @return
     */
    @PostMapping("myFriends")
    public IMoocJSONResult myFriends(String userId) {
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }

        List<MyFriendsVO> lists = userService.queryMyFriends(userId);

        return IMoocJSONResult.ok(lists);
    }

    /**
     * 用户手机端获取未签收消息列表
     * @param acceptUserId
     * @return
     */
    @PostMapping("getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId) {
        if (StringUtils.isBlank(acceptUserId)) {
            return IMoocJSONResult.errorMsg("");
        }

        List<ChatMsg> unReadMsgList = userService.getUnReadMsgList(acceptUserId);

        return IMoocJSONResult.ok(unReadMsgList);
    }
}
