package com.microsoftTeams.bot;
import java.time.ZonedDateTime;

/**
 * Used to store userInfo of who locked some component
 */

public class UserInfo {
    private String userId;
    private String userName;
    private ZonedDateTime time;

    public UserInfo(String userId, ZonedDateTime time) {
        this.userId = userId;
        this.time = time;
    }

    public UserInfo(String userId, String userName, ZonedDateTime time) {
        this.userId = userId;
        this.userName = userName;
        this.time = time;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public void setTime(ZonedDateTime time) {
        this.time = time;
    }
}
