package com.icinfo.taskmanagement.security;

import com.icinfo.taskmanagement.common.ErrorCode;
import com.icinfo.taskmanagement.exception.BusinessException;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        HOLDER.set(currentUser);
    }

    public static CurrentUser get() {
        CurrentUser currentUser = HOLDER.get();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
