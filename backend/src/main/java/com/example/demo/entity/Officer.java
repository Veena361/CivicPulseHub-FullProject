package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "officers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Officer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String phoneNo;

    private String department;   // Officer Department (Traffic, Crime, Cyber, Admin, etc.)

    @Enumerated(EnumType.STRING)
    private Role role;  // Default: Role.OFFICER

    @Enumerated(EnumType.STRING)
    private OfficerStatus status; // AVAILABLE, BUSY, OVERLOADED

    // Optional: store number of active complaints (Transient: not persisted)
    @Transient
    private Long activeComplaints;

    // ✅ Helper method to safely return active complaints
    public Long getActiveComplaints() {
        return activeComplaints != null ? activeComplaints : 0L;
    }

    // ------------------ For secure password setup/reset ------------------
    private String resetToken;               // Token for one-time password set/reset
    private LocalDateTime resetTokenExpiry;  // Expiry time for the token
}
