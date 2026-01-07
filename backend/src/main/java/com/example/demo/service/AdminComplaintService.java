package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.payload.*;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.OfficerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminComplaintService {

    private final ComplaintRepository complaintRepository;
    private final OfficerRepository officerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ---------------- GET OFFICER BY ID ----------------
    public Officer getOfficerById(Long officerId) {
        return officerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found"));
    }

    // ---------------- LIST ALL COMPLAINTS ----------------
    public List<Complaint> listAllComplaints(String search, String status, String priority) {
        return complaintRepository.findAll(); // Can add filters later
    }

    // ---------------- GET COMPLAINT DETAILS ----------------
    public Complaint getComplaintDetails(Long complaintId) {
        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    // ---------------- CREATE COMPLAINT ----------------
    @Transactional
    public Complaint createComplaint(Complaint complaint) {
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setSubmissionDate(LocalDateTime.now());

        Complaint saved = complaintRepository.save(complaint);

        // Notify admin dashboard
        messagingTemplate.convertAndSend(
                "/topic/admin/complaints",
                new NotificationDto(saved.getId(), "New complaint submitted: " + saved.getTitle())
        );

        return saved;
    }

    // ---------------- ASSIGN OFFICER ----------------
    @Transactional
    public Complaint assignOfficer(Long complaintId, ComplaintAssignRequestDto request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        Officer officer = officerRepository.findById(request.getOfficerId())
                .orElseThrow(() -> new RuntimeException("Officer not found"));

        complaint.setAssignedOfficer(officer);
        complaint.setAssignedDate(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);

        officer.setStatus(OfficerStatus.BUSY);
        officerRepository.save(officer);

        Complaint saved = complaintRepository.save(complaint);

        messagingTemplate.convertAndSendToUser(
                saved.getCitizen().getEmail(),
                "/queue/notify",
                new NotificationDto(saved.getId(), "Assigned to Officer: " + officer.getName())
        );

        return saved;
    }

    // ---------------- UPDATE STATUS ----------------
    @Transactional
    public Complaint updateStatus(Long complaintId, ComplaintStatusUpdateRequestDto request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        ComplaintStatus newStatus = ComplaintStatus.valueOf(request.getStatus().toUpperCase());
        complaint.setStatus(newStatus);

        Officer officer = complaint.getAssignedOfficer();

        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolutionDate(LocalDateTime.now());

            if (officer != null) {
                long activeCount = complaintRepository.countByAssignedOfficerAndStatusIn(
                        officer,
                        List.of(ComplaintStatus.PENDING, ComplaintStatus.IN_PROGRESS)
                );

                if (activeCount == 0) {
                    officer.setStatus(OfficerStatus.AVAILABLE);
                }

                officerRepository.save(officer);
            }
        }

        if (newStatus == ComplaintStatus.REOPENED && officer != null) {
            officer.setStatus(OfficerStatus.BUSY);
            officerRepository.save(officer);
        }

        Complaint saved = complaintRepository.save(complaint);

        messagingTemplate.convertAndSendToUser(
                saved.getCitizen().getEmail(),
                "/queue/notify",
                new NotificationDto(saved.getId(), "Status updated: " + newStatus.name())
        );

        return saved;
    }

    // ---------------- UPDATE STAGE ----------------
    @Transactional
    public Complaint updateStage(Long complaintId, ComplaintStage stage) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setComplaintStage(stage);

        Complaint saved = complaintRepository.save(complaint);

        messagingTemplate.convertAndSendToUser(
                saved.getCitizen().getEmail(),
                "/queue/notify",
                new NotificationDto(saved.getId(), "Stage updated: " + stage.name())
        );

        return saved;
    }

    // ---------------- UPDATE PRIORITY ----------------
    @Transactional
    public Complaint updatePriority(Long complaintId, String priorityStr) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        Priority priority = Priority.valueOf(priorityStr.toUpperCase());
        complaint.setPriority(priority);

        Complaint saved = complaintRepository.save(complaint);

        messagingTemplate.convertAndSendToUser(
                saved.getCitizen().getEmail(),
                "/queue/notify",
                new NotificationDto(saved.getId(), "Priority updated: " + priority.name())
        );

        return saved;
    }

    // ---------------- UPDATE OFFICER EVIDENCE ----------------
    @Transactional
    public Complaint updateOfficerEvidence(Long complaintId, String evidenceUrl) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setOfficerEvidenceUrl(evidenceUrl);

        Complaint saved = complaintRepository.save(complaint);

        // Notify admin dashboard
        messagingTemplate.convertAndSend(
                "/topic/admin/complaints",
                new NotificationDto(saved.getId(), "Officer uploaded evidence")
        );

        // Notify citizen
        messagingTemplate.convertAndSendToUser(
                saved.getCitizen().getEmail(),
                "/queue/notify",
                new NotificationDto(saved.getId(), "Officer uploaded evidence for your complaint")
        );

        return saved;
    }

    // ---------------- ADD ADMIN REMARK ----------------
    @Transactional
    public Complaint addAdminRemark(Long complaintId, String remark) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setAdminRemark(remark);

        Complaint saved = complaintRepository.save(complaint);

        // Notify citizen
        messagingTemplate.convertAndSendToUser(
                saved.getCitizen().getEmail(),
                "/queue/notify",
                new NotificationDto(saved.getId(), "Admin responded: " + remark)
        );

        return saved;
    }

    // ---------------- GET ALL OFFICERS WORKLOAD ----------------
    public List<OfficerWorkloadResponse> getAllOfficersWorkload() {
        List<Officer> officers = officerRepository.findAll();

        return officers.stream().map(officer -> {
            long activeComplaints = complaintRepository.countByAssignedOfficerAndStatusIn(
                    officer,
                    List.of(ComplaintStatus.PENDING, ComplaintStatus.IN_PROGRESS)
            );

            return OfficerWorkloadResponse.builder()
                    .id(officer.getId())
                    .name(officer.getName())
                    .email(officer.getEmail())
                    .department(officer.getDepartment())
                    .status(officer.getStatus() != null
                            ? officer.getStatus().name()
                            : OfficerStatus.AVAILABLE.name())
                    .activeComplaints(activeComplaints)
                    .build();
        }).toList();
    }

    // ---------------- DELETE ----------------
    @Transactional
    public void deleteComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaintRepository.delete(complaint);

        messagingTemplate.convertAndSendToUser(
                complaint.getCitizen().getEmail(),
                "/queue/notify",
                new NotificationDto(complaintId, "Complaint deleted")
        );
    }
}
