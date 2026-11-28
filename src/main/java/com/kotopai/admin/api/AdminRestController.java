package com.kotopai.admin.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kotopai.admin.dto.AdminSystemStats;
import com.kotopai.admin.dto.AdminUserView;
import com.kotopai.admin.dto.ChangeRoleRequest;
import com.kotopai.admin.service.AdminService;
import com.kotopai.auth.service.KotopaiUserDetails;

import jakarta.validation.Valid;

/**
 * REST API controller for administrative monitoring and user role management.
 * Protected by Spring Security and AdminService @PreAuthorize("hasRole('ADMIN')").
 */
@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    private final AdminService adminService;

    public AdminRestController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminSystemStats> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserView>> listUsers(@RequestParam(value = "q", required = false) String query) {
        return ResponseEntity.ok(adminService.listUsers(query));
    }

    @PostMapping("/users/{id}/role")
    public ResponseEntity<Map<String, String>> changeUserRole(
            @AuthenticationPrincipal KotopaiUserDetails currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangeRoleRequest request) {
        Long adminId = currentUser != null ? currentUser.getId() : null;
        adminService.changeUserRole(adminId, id, request.role());
        return ResponseEntity.ok(Map.of("message", "User role updated successfully to " + request.role()));
    }
}

