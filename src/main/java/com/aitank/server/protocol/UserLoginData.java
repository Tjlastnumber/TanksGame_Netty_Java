package com.aitank.server.protocol;

public class UserLoginData {

    private String UserId;
    private String UserName;
    private String PassWord;
	private String roomId;

	public String getUserName() {
		return UserName;
	}
	public void setUserName(String userName) {
		UserName = userName;
	}
	public String getPassWord() {
		return PassWord;
	}
	public void setPassWord(String passWord) {
		PassWord = passWord;
	}
	public String getUserId() {
		return UserId;
	}
	public void setUserId(String userId) {
		UserId = userId;
	}
	public String getRoomId() { return roomId; }
	public void setRoomId(String value) { roomId = value; }
}
