package com.example.demo.controller;

import com.example.demo.entity.Complaint;
import com.example.demo.entity.ComplaintStage;
import com.example.demo.entity.Officer;
import com.example.demo.payload.ComplaintAssignRequestDto;
import com.example.demo.payload.ComplaintStatusUpdateRequestDto;
import com.example.demo.payload.OfficerWorkloadResponse;
import com.example.demo.service.AdminComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/complaints")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final AdminComplaintService adminComplaintService;

    // ---------------- LIST ----------------
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Complaint> listComplaints(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return adminComplaintService.listAllComplaints(search, status, priority);
    }

    // ---------------- GET ----------------
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Complaint getComplaint(@PathVariable Long id) {
        return adminComplaintService.getComplaintDetails(id);
    }

    // ---------------- ASSIGN OFFICER ----------------
    @PostMapping("/{id}/assign-officer")
    @PreAuthorize("hasRole('ADMIN')")
    public Complaint assignOfficer(
            @PathVariable Long id,
            @RequestBody ComplaintAssignRequestDto request) {

        Complaint complaint = adminComplaintService.assignOfficer(id, request);
        Officer fullOfficer = adminComplaintService.getOfficerById(request.getOfficerId());
        complaint.setAssignedOfficer(fullOfficer);
        return complaint;
    }

    // ---------------- UPDATE STATUS ----------------
    @PostMapping("/{id}/update-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Complaint updateStatus(
            @PathVariable Long id,
            @RequestBody ComplaintStatusUpdateRequestDto request) {
        return adminComplaintService.updateStatus(id, request);
    }

    // ---------------- UPDATE STAGE ----------------
    @PostMapping("/{id}/update-stage")
    @PreAuthorize("hasRole('ADMIN')")
    public Complaint updateStage(
            @PathVariable Long id,
            @RequestParam ComplaintStage stage) {
        return adminComplaintService.updateStage(id, stage);
    }

    // ---------------- UPDATE PRIORITY ----------------
    @PostMapping("/{id}/update-priority")
    @PreAuthorize("hasRole('ADMIN')")
    public Complaint updatePriority(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String priority = body.get("priority");
        return adminComplaintService.updatePriority(id, priority);
    }

    // ---------------- DELETE ----------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteComplaint(@PathVariable Long id) {
        adminComplaintService.deleteComplaint(id);
        return "✅ Complaint deleted successfully";
    }

    // ---------------- GET OFFICER WORKLOAD ----------------
    @GetMapping("/officers/workload")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OfficerWorkloadResponse> getOfficersWorkload() {
        return adminComplaintService.getAllOfficersWorkload();
    }
}
