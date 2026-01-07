package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.payload.*;
import com.example.demo.repositories.CitizenRepository;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitizenService {

    private final CitizenRepository citizenRepository;
    private final ComplaintRepository complaintRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SimpMessagingTemplate messagingTemplate;

    // ================= SIGNUP =================
    public String signup(CitizenSignupRequest req) {
        if (citizenRepository.findByEmail(req.getEmail()) != null) {
            throw new RuntimeException("Email already registered");
        }

        Citizen citizen = Citizen.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.CITIZEN)
                .build();

        citizenRepository.save(citizen);
        System.out.println("✅ Citizen registered: " + citizen.getEmail());
        return "Citizen registered successfully";
    }

    // ================= LOGIN =================
    public CitizenLoginResponse login(LoginRequest request) {
        Citizen citizen = citizenRepository.findByEmail(request.getEmail());

        if (citizen == null || !passwordEncoder.matches(request.getPassword(), citizen.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(citizen);
        System.out.println("✅ Citizen logged in: " + citizen.getEmail());

        return new CitizenLoginResponse(
                "Login successful",
                token,
                citizen.getRole().name()
        );
    }

    // ================= SUBMIT COMPLAINT =================
    public CitizenComplaintResponse submitComplaint(Long citizenId, Complaint complaint) {

        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        complaint.setCitizen(citizen);
        complaint.setSubmissionDate(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.PENDING);

        Complaint savedComplaint = complaintRepository.save(complaint);
        System.out.println("📦 Complaint saved: ID=" + savedComplaint.getId());

        NotificationDto payload = new NotificationDto(
                savedComplaint.getId(),
                savedComplaint.getStatus().name()
        );

        String normalizedEmail = citizen.getEmail().toLowerCase();

        // Send WebSocket notification to the citizen
        messagingTemplate.convertAndSendToUser(
                normalizedEmail,
                "/queue/notify",
                payload
        );

        // Notify all admins
        messagingTemplate.convertAndSend(
                "/topic/admin/complaints",
                payload
        );

        return mapToCitizenResponse(savedComplaint);
    }

    // ================= GET MY COMPLAINTS =================
    public List<CitizenComplaintResponse> getMyComplaints(Long citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        List<Complaint> complaints = complaintRepository.findByCitizen(citizen);

        return complaints.stream()
                .map(this::mapToCitizenResponse)
                .toList();
    }

    // ================= GET COMPLAINT DETAILS =================
    public CitizenComplaintResponse getComplaintDetails(Long citizenId, Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new RuntimeException("Access denied");
        }

        return mapToCitizenResponse(complaint);
    }

    // ================= HELPER: MAP COMPLAINT TO DTO =================
    private CitizenComplaintResponse mapToCitizenResponse(Complaint c) {
        return CitizenComplaintResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .complaintStage(c.getComplaintStage() != null ? c.getComplaintStage().name() : null)
                .officerRemark(c.getOfficerRemark())
                .adminRemark(c.getAdminRemark())
                .officerEvidenceUrl(c.getOfficerEvidenceUrl())
                .expectedCompletionDate(c.getExpectedCompletionDate())
                .submissionDate(c.getSubmissionDate())
                .build();
    }
}
