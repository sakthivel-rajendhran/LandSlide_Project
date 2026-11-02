package com.slopeguard.dto;

public class GoogleAuthRequest {

    private String idToken;
    private String token;
    private String sub;
    private String email;
    private String name;
    private String picture;

    public GoogleAuthRequest() {
    }

    public GoogleAuthRequest(String idToken, String token, String sub, String email, String name, String picture) {
        this.idToken = idToken;
        this.token = token;
        this.sub = sub;
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}

