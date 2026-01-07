package com.example.demo.service;

import com.example.demo.entity.Admin;
import com.example.demo.entity.Officer;
import com.example.demo.entity.Role;
import com.example.demo.payload.ForgotPasswordRequest;
import com.example.demo.payload.LoginRequest;
import com.example.demo.payload.LoginResponse;
import com.example.demo.payload.OfficerSignupRequest;
import com.example.demo.payload.ResetOfficerPasswordRequest;
import com.example.demo.payload.ResetPasswordRequest;
import com.example.demo.repositories.AdminRepository;
import com.example.demo.repositories.OfficerRepository;
import com.example.demo.security.JwtUtils;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OfficerRepository officerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService; // Inject EmailService

    // -------------------- Admin Signup --------------------
    public String signup(Admin admin) {
        if (adminRepository.findByEmail(admin.getEmail()) != null) {
            return "❌ Admin email already exists!";
        }

        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole(Role.ADMIN);
        adminRepository.save(admin);

        return "✅ Admin registered successfully!";
    }

    // -------------------- Admin Login --------------------
    public LoginResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail());

        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return new LoginResponse("❌ Invalid email or password", null, null);
        }

        String token = jwtUtils.generateToken(admin.getEmail(), admin.getRole().name());
        return new LoginResponse("✅ Login successful", token, admin.getRole().name());
    }

    // -------------------- Simplified Create Officer --------------------
    public String createOfficer(OfficerSignupRequest request) {
        if (officerRepository.findByEmail(request.getEmail()) != null) {
            return "❌ Officer email already exists!";
        }

        // Encode password provided by admin
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Officer officer = Officer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNo(request.getPhoneNo())
                .department(request.getDepartment())
                .role(Role.OFFICER)
                .password(encodedPassword) // set encoded password directly
                .build();

        officerRepository.save(officer);

        // Send credentials email to officer
        try {
            emailService.sendCredentialsEmail(request.getEmail(), request.getPassword(), "Officer");
        } catch (MessagingException e) {
            e.printStackTrace();
            return "⚠ Officer created but failed to send email!";
        }

        return "✅ Officer created successfully! Email sent with login credentials.";
    }

    // -------------------- Admin Forgot Password --------------------
    public String forgotPassword(ForgotPasswordRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail());
        if (admin == null) return "❌ Admin not found!";

        String otp = String.format("%06d", (int) (Math.random() * 900000 + 100000));
        admin.setResetToken(otp);
        admin.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        adminRepository.save(admin);

        try {
            emailService.sendOtpEmail(admin.getEmail(), otp);
        } catch (MessagingException e) {
            e.printStackTrace();
            return "❌ Failed to send OTP email. Please try again.";
        }

        return "✅ OTP sent to your registered email! (valid for 10 minutes)";
    }

    // -------------------- Reset Admin Password --------------------
    public String resetPassword(ResetPasswordRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail());
        if (admin == null) return "❌ Admin not found!";

        if (admin.getResetToken() == null || !admin.getResetToken().equals(request.getResetToken())) {
            return "❌ Invalid OTP!";
        }

        if (admin.getResetTokenExpiry() == null || admin.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            return "⏳ OTP expired! Please request a new one.";
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        admin.setResetToken(null);
        admin.setResetTokenExpiry(null);
        adminRepository.save(admin);

        return "✅ Admin password reset successful!";
    }

    // -------------------- Reset Officer Password by Admin --------------------
    public String resetOfficerPassword(ResetOfficerPasswordRequest request) {
        Officer officer = officerRepository.findByEmail(request.getOfficerEmail());
        if (officer == null) return "❌ Officer not found!";

        officer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        officerRepository.save(officer);

        return "✅ Officer password reset by Admin successfully!";
    }
}
