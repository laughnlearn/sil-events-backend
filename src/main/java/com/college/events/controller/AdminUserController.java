package com.college.events.controller;

import com.college.events.dto.admin.AdminUserResponse;
import com.college.events.dto.admin.CreateAdmin2Request;
import com.college.events.dto.admin.CreateAdmin2Response;
import com.college.events.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN1')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping
    public ResponseEntity<CreateAdmin2Response> create(@Valid @RequestBody CreateAdmin2Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createAdmin2(request));
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> list() {
        return ResponseEntity.ok(adminUserService.getAdminUsers());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        adminUserService.deleteAdminUser(id);
        return ResponseEntity.noContent().build();
    }
}
