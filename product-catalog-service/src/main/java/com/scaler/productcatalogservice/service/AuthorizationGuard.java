package com.scaler.productcatalogservice.service;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.productcatalogservice.exception.AccessDeniedException;

public class AuthorizationGuard {
    public static void requireRole(String role) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        if (user == null || user.getRoles() == null || !user.getRoles().contains(role)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}