package com.kotopai.admin.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.kotopai.admin.dto.AdminSystemStats;
import com.kotopai.admin.dto.AdminUserView;
import com.kotopai.user.domain.Role;

/**
 * Administrative operations contract protected by method-level security.
 */
public interface AdminService {

    /**
     * Calculates system-wide metrics across all modules.
     */
    @PreAuthorize("hasRole('ADMIN')")
    AdminSystemStats getSystemStats();

    /**
     * Lists all registered users.
     */
    @PreAuthorize("hasRole('ADMIN')")
    List<AdminUserView> listUsers();

    /**
     * Updates the role of a user, preventing self-demotion if they are the sole admin.
     */
    @PreAuthorize("hasRole('ADMIN')")
    void changeUserRole(Long currentAdminId, Long targetUserId, Role newRole);
}

