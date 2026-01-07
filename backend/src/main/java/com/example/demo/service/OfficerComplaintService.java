package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.exception.AccessDeniedException;
import com.example.demo.exception.ComplaintNotFoundException;
import com.example.demo.payload.NotificationDto;
import com.example.demo.payload.OfficerComplaintResponse;
import com.example.demo.repositories.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OfficerComplaintService {

    private final ComplaintRepository complaintRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final String UPLOAD_DIR = "uploads/officer/";
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final List<String> ALLOWED_FILE_TYPES = List.of("image/png", "image/jpeg", "image/jpg");

    // -------------------- GET ASSIGNED COMPLAINTS --------------------
    public List<OfficerComplaintResponse> getAssignedComplaintResponses(Officer officer) {
        List<Complaint> complaints = complaintRepository.findByAssignedOfficer(officer);
        return complaints.stream().map(this::mapToOfficerResponse).toList();
    }

    // -------------------- UPDATE STATUS --------------------
   public Complaint updateStatus(Officer officer, Long complaintId, ComplaintStatus status) {
    Complaint complaint = getOfficerComplaint(officer, complaintId);

    complaint.setStatus(status);

    // ✅ SET RESOLUTION DATE AUTOMATICALLY
    if (status == ComplaintStatus.RESOLVED) {
        complaint.setResolutionDate(LocalDateTime.now());
        System.out.println("🔥 OFFICER UPDATE STATUS HIT: " + status);

    }
    Complaint saved = complaintRepository.save(complaint);

    notifyAdmins("Complaint status updated by officer: " + saved.getId() + " -> " + saved.getStatus());
    notifyCitizen(saved, "Your complaint status updated: " + saved.getStatus());

    return saved;
}

    // -------------------- UPDATE STAGE --------------------
    public Complaint updateStage(Officer officer, Long complaintId, ComplaintStage stage) {
        Complaint complaint = getOfficerComplaint(officer, complaintId);
        complaint.setComplaintStage(stage);
        Complaint saved = complaintRepository.save(complaint);

        notifyAdmins("Complaint stage updated by officer: " + saved.getId() + " -> " + saved.getComplaintStage());
        notifyCitizen(saved, "Your complaint stage updated: " + saved.getComplaintStage());

        return saved;
    }

    // -------------------- ADD REMARK --------------------
    public Complaint addRemark(Officer officer, Long complaintId, String remark) {
        Complaint complaint = getOfficerComplaint(officer, complaintId);
        complaint.setOfficerRemark(remark);
        Complaint saved = complaintRepository.save(complaint);

        notifyAdmins("Officer added remark to complaint: " + saved.getId());
        notifyCitizen(saved, "Officer added a remark to your complaint.");

        return saved;
    }

    // -------------------- UPDATE EXPECTED COMPLETION --------------------
    public Complaint updateExpectedCompletionDate(Officer officer, Long complaintId, LocalDateTime expectedDate) {
        Complaint complaint = getOfficerComplaint(officer, complaintId);
        complaint.setExpectedCompletionDate(expectedDate);
        Complaint saved = complaintRepository.save(complaint);

        notifyAdmins("Expected completion date updated for complaint: " + saved.getId());
        notifyCitizen(saved, "Expected completion date updated for your complaint.");

        return saved;
    }

    // -------------------- UPLOAD EVIDENCE --------------------
    public Complaint uploadEvidence(Officer officer, Long complaintId, MultipartFile file) {
        Complaint complaint = getOfficerComplaint(officer, complaintId);

        if (file == null || file.isEmpty()) throw new RuntimeException("File is empty");
        if (!ALLOWED_FILE_TYPES.contains(file.getContentType()))
            throw new RuntimeException("Invalid file type. Only PNG/JPG allowed.");
        if (file.getSize() > MAX_FILE_SIZE) throw new RuntimeException("File size exceeds 5MB");

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String filename = UUID.randomUUID() + ext;
            Path target = Paths.get(UPLOAD_DIR + filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            complaint.setOfficerEvidenceUrl("/uploads/officer/" + filename);
            Complaint saved = complaintRepository.save(complaint);

            notifyAdmins("Officer uploaded evidence for complaint: " + saved.getId());
            notifyCitizen(saved, "Officer uploaded evidence for your complaint. Please check.");

            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Evidence upload failed: " + e.getMessage());
        }
    }

    // -------------------- MAP ENTITY TO DTO --------------------
    public OfficerComplaintResponse mapToOfficerResponse(Complaint c) {
        String locationText = c.getLocation();
        if (locationText == null || locationText.isBlank()) {
            locationText = "Coordinates: " + c.getLatitude() + ", " + c.getLongitude();
        }

        Officer assignedOfficer = c.getAssignedOfficer();

        return OfficerComplaintResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .category(c.getCategory() != null ? c.getCategory().name() : null)
                .priority(c.getPriority() != null ? c.getPriority().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .description(c.getDescription())
                .location(locationText)
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .submissionDate(c.getSubmissionDate())
                .imageUrl(c.getImageUrl() != null ? c.getImageUrl() : c.getOfficerEvidenceUrl())
                .resolutionDate(c.getResolutionDate())//resolution date added
                .complaintStage(c.getComplaintStage() != null ? c.getComplaintStage().name() : null)
                .assignedOfficerName(assignedOfficer != null ? assignedOfficer.getName() : null)
                .assignedOfficerStatus(assignedOfficer != null ? assignedOfficer.getStatus().name() : null)
                .assignedOfficerDepartment(assignedOfficer != null ? assignedOfficer.getDepartment() : null)
                .assignedOfficerActiveComplaints(assignedOfficer != null ? assignedOfficer.getActiveComplaints() : 0L)
                .officerRemark(c.getOfficerRemark())
                .assignedDate(c.getAssignedDate())
                .expectedCompletionDate(c.getExpectedCompletionDate())
                .build();
    }

    // -------------------- SECURITY CHECK --------------------
    private Complaint getOfficerComplaint(Officer officer, Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with ID: " + complaintId));

        if (complaint.getAssignedOfficer() == null || !complaint.getAssignedOfficer().getId().equals(officer.getId())) {
            throw new AccessDeniedException("You are not allowed to modify this complaint");
        }

        return complaint;
    }

    // -------------------- HELPER: NOTIFY ADMINS --------------------
    private void notifyAdmins(String message) {
        messagingTemplate.convertAndSend("/topic/admin/complaints", message);
    }

    // -------------------- HELPER: NOTIFY CITIZEN --------------------
    private void notifyCitizen(Complaint complaint, String message) {
        String email = complaint.getCitizen().getEmail().toLowerCase();
        NotificationDto payload = new NotificationDto(complaint.getId(), message);
        messagingTemplate.convertAndSendToUser(email, "/queue/notify", payload);
    }
}
