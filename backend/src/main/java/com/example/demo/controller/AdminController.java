package com.example.demo.controller;

import com.example.demo.entity.Admin;
import com.example.demo.payload.*;
import com.example.demo.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // -------------------- Admin Signup --------------------
    @PostMapping("/signup")
    public String signup(@RequestBody Admin admin) {
        return adminService.signup(admin);
    }

    // -------------------- Admin Login --------------------
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return adminService.login(request);
    }

    // -------------------- Create Officer --------------------
    @PostMapping("/create-officer")
    public String createOfficer(@RequestBody OfficerSignupRequest request) {
        return adminService.createOfficer(request);
    }

    // -------------------- Admin Forgot Password --------------------
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return adminService.forgotPassword(request);
    }

    // -------------------- Admin Reset Password --------------------
    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody ResetPasswordRequest request) {
        return adminService.resetPassword(request);
    }

    // -------------------- Admin Reset Officer Password --------------------
    @PostMapping("/reset-officer-password")
    public String resetOfficerPassword(@RequestBody ResetOfficerPasswordRequest request) {
        return adminService.resetOfficerPassword(request);
    }
}
