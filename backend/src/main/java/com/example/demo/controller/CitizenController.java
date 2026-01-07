package com.example.demo.controller;

import com.example.demo.entity.Citizen;
import com.example.demo.entity.Complaint;
import com.example.demo.payload.CitizenComplaintResponse;
import com.example.demo.payload.CitizenLoginResponse;
import com.example.demo.payload.CitizenSignupRequest;
import com.example.demo.payload.LoginRequest;
import com.example.demo.repositories.CitizenRepository;
import com.example.demo.security.JwtUtils;
import com.example.demo.service.CitizenService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/citizen")
@RequiredArgsConstructor
@CrossOrigin
public class CitizenController {

    private final CitizenService citizenService;
    private final CitizenRepository citizenRepository;
    private final JwtUtils jwtUtils;

    // ================== HELPER ==================
    private Citizen getCitizenFromRequest(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtUtils.extractEmail(token);

        Citizen citizen = citizenRepository.findByEmail(email);
        if (citizen == null) {
            throw new RuntimeException("Citizen not found");
        }
        return citizen;
    }

    // ================== AUTH ==================
    @PostMapping("/signup")
    public String signup(@RequestBody CitizenSignupRequest request) {
        return citizenService.signup(request);
    }

    @PostMapping("/login")
    public CitizenLoginResponse login(@RequestBody LoginRequest request) {
        return citizenService.login(request);
    }

    // ================== COMPLAINTS ==================

    // ✅ GET MY COMPLAINTS (DTO)
    @GetMapping("/complaints/my")
    public List<CitizenComplaintResponse> getMyComplaints(HttpServletRequest request) {
        Citizen citizen = getCitizenFromRequest(request);
        return citizenService.getMyComplaints(citizen.getId());
    }

    // ✅ GET SINGLE COMPLAINT (DTO)
    @GetMapping("/complaints/{complaintId}")
    public CitizenComplaintResponse getComplaintDetails(
            @PathVariable Long complaintId,
            HttpServletRequest request
    ) {
        Citizen citizen = getCitizenFromRequest(request);
        return citizenService.getComplaintDetails(citizen.getId(), complaintId);
    }

    // ✅ SUBMIT COMPLAINT (DTO)
    @PostMapping("/complaints")
    public CitizenComplaintResponse submitComplaint(
            @RequestBody Complaint complaint,
            HttpServletRequest request
    ) {
        Citizen citizen = getCitizenFromRequest(request);
        return citizenService.submitComplaint(citizen.getId(), complaint);
    }
}
