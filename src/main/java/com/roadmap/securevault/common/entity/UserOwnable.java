package com.roadmap.securevault.common.entity;

public interface UserOwnable<U extends BaseUser> {
    U getUser();
}