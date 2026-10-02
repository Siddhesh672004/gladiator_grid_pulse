package com.examly.springapp.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

// Admin or SuperAdmin only; wrong role -> AccessDeniedException -> 403 (GlobalExceptionHandler)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('Admin', 'SuperAdmin')")
public @interface AdminOnly {
}
