package com.example.demo.controller;

import com.example.demo.entity.Complaint;
import com.example.demo.entity.ComplaintStage;
import com.example.demo.entity.ComplaintStatus;
import com.example.demo.entity.Officer;
import com.example.demo.payload.LoginRequest;
import com.example.demo.payload.LoginResponse;
import com.example.demo.payload.OfficerComplaintResponse;
import com.example.demo.payload.OfficerWorkloadResponse;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.OfficerRepository;
import com.example.demo.service.OfficerComplaintService;
import com.example.demo.service.OfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfficerComplaintController {

    private final OfficerComplaintService officerComplaintService;
    private final OfficerService officerService;
    private final OfficerRepository officerRepository;
    private final ComplaintRepository complaintRepository;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return officerService.login(request);
    }

    @GetMapping("/complaints")
    public ResponseEntity<List<OfficerComplaintResponse>> getAssignedComplaints(Authentication authentication) {
        Officer officer = (Officer) authentication.getPrincipal();
        return ResponseEntity.ok(officerComplaintService.getAssignedComplaintResponses(officer));
    }

    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<OfficerComplaintResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String status
    ) {
        Officer officer = (Officer) authentication.getPrincipal();
        ComplaintStatus complaintStatus;
        try { complaintStatus = ComplaintStatus.valueOf(status.toUpperCase()); } 
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }

        Complaint updated = officerComplaintService.updateStatus(officer, id, complaintStatus);
        return ResponseEntity.ok(officerComplaintService.mapToOfficerResponse(updated));
    }

    @PutMapping("/complaints/{id}/stage")
    public ResponseEntity<OfficerComplaintResponse> updateStage(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String stage
    ) {
        Officer officer = (Officer) authentication.getPrincipal();
        ComplaintStage complaintStage;
        try { complaintStage = ComplaintStage.valueOf(stage.toUpperCase()); } 
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }

        Complaint updated = officerComplaintService.updateStage(officer, id, complaintStage);
        return ResponseEntity.ok(officerComplaintService.mapToOfficerResponse(updated));
    }

    @PutMapping("/complaints/{id}/remark")
    public ResponseEntity<OfficerComplaintResponse> addRemark(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String remark
    ) {
        if (remark == null || remark.isBlank()) return ResponseEntity.badRequest().build();
        Officer officer = (Officer) authentication.getPrincipal();
        Complaint updated = officerComplaintService.addRemark(officer, id, remark);
        return ResponseEntity.ok(officerComplaintService.mapToOfficerResponse(updated));
    }

    @PutMapping("/complaints/{id}/expected-completion")
    public ResponseEntity<OfficerComplaintResponse> updateExpectedCompletion(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String expectedDateTime
    ) {
        Officer officer = (Officer) authentication.getPrincipal();
        LocalDateTime expectedDate;
        try { expectedDate = LocalDate.parse(expectedDateTime).atStartOfDay(); } 
        catch (Exception e) { return ResponseEntity.badRequest().build(); }

        Complaint updated = officerComplaintService.updateExpectedCompletionDate(officer, id, expectedDate);
        return ResponseEntity.ok(officerComplaintService.mapToOfficerResponse(updated));
    }

    @PostMapping("/complaints/{id}/evidence")
    public ResponseEntity<OfficerComplaintResponse> uploadEvidence(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().build();
        Officer officer = (Officer) authentication.getPrincipal();
        Complaint updated = officerComplaintService.uploadEvidence(officer, id, file);
        return ResponseEntity.ok(officerComplaintService.mapToOfficerResponse(updated));
    }

    @GetMapping("/workload")
    public ResponseEntity<List<OfficerWorkloadResponse>> getOfficerWorkload() {
        List<OfficerWorkloadResponse> workload = officerRepository.findAll().stream().map(officer -> {
            long active = complaintRepository.countActiveComplaintsByOfficer(officer.getId());
            String status = active == 0 ? "AVAILABLE" : active <= 5 ? "BUSY" : "OVERLOADED";
            return OfficerWorkloadResponse.builder()
                    .id(officer.getId())
                    .name(officer.getName())
                    .email(officer.getEmail())
                    .department(officer.getDepartment())
                    .activeComplaints(active)
                    .status(status)
                    .build();
        }).toList();
        return ResponseEntity.ok(workload);
    }
}
