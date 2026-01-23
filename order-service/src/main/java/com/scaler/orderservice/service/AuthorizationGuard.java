package com.scaler.orderservice.service;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.orderservice.exception.AccessDeniedException;

public class AuthorizationGuard {
    public static boolean isAdmin() {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        return user != null && user.getRoles() != null && user.getRoles().contains("ADMIN");
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Access denied");
        }
    }
}