package com.sincit.bid.auth;

import com.sincit.bid.common.BusinessException;

/**
 * 请求级用户上下文。Service 层不依赖 Servlet API，统一通过入参 actor 传递。
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<CurrentUser>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static CurrentUser get() {
        return CURRENT.get();
    }

    public static CurrentUser require() {
        CurrentUser user = CURRENT.get();
        if (user == null) {
            throw BusinessException.unauthorized("登录已失效，请重新登录");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
