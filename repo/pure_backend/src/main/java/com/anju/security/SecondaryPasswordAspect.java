package com.anju.security;

import com.anju.common.BusinessException;
import com.anju.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SecondaryPasswordAspect {

    private final HttpServletRequest request;
    private final AuthService authService;

    public SecondaryPasswordAspect(HttpServletRequest request, AuthService authService) {
        this.request = request;
        this.authService = authService;
    }

    @Around("@annotation(com.anju.security.RequireSecondaryPassword)")
    public Object ensureSecondaryVerified(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("unauthorized");
        }
        String secondaryToken = request.getHeader("X-Secondary-Token");
        boolean ok = authService.validateSecondaryToken(secondaryToken, authentication.getName());
        if (!ok) {
            throw new BusinessException("secondary verification required or expired");
        }
        return joinPoint.proceed();
    }
}
