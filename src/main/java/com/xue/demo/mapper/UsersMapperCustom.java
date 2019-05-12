package com.xue.demo.mapper;

import java.util.List;

import com.xue.demo.pojo.Users;
import com.xue.demo.pojo.vo.FriendRequestVO;
import com.xue.demo.pojo.vo.MyFriendsVO;
import com.xue.demo.utils.MyMapper;

public interface UsersMapperCustom extends MyMapper<Users> {
	
	public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
	
	public List<MyFriendsVO> queryMyFriends(String userId);
	
	public void batchUpdateMsgSigned(List<String> msgIdList);
	
}