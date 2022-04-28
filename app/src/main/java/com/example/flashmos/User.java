package com.example.flashmos;

import javax.annotation.Nullable;

public class User {

    public String uid; // user id
    public String uuid; // BLE uid
    public String notif_token; // notif token for firebase
    public String displayName;

    public User(){

    }

    public User(String uid,String uuid,String notif_token,String displayName){
        this.uid = uid;
        this.uuid = uuid;
        this.notif_token = notif_token;
        this.displayName = displayName;
    }
}
