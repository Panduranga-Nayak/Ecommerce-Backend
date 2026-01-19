package com.scaler.ecommerce.common.security;

public class RequestContext {
    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(AuthenticatedUser user) {
        CURRENT_USER.set(user);
    }

    public static AuthenticatedUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
