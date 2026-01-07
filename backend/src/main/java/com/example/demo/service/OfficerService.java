package com.example.demo.service;



import com.example.demo.entity.Officer;
import com.example.demo.payload.LoginRequest;
import com.example.demo.payload.LoginResponse;
import com.example.demo.repositories.OfficerRepository;
import com.example.demo.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class OfficerService {

    @Autowired
    private OfficerRepository officerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    // Officer Login
    public LoginResponse login(LoginRequest request) {
        Officer officer = officerRepository.findByEmail(request.getEmail());

        if (officer == null || !passwordEncoder.matches(request.getPassword(), officer.getPassword())) {
            return new LoginResponse("❌ Invalid email or password", null, null);
        }

        String token = jwtUtils.generateToken(officer.getEmail(), officer.getRole().name());
        return new LoginResponse("✅ Officer login successful", token, officer.getRole().name());
    }
    
    
    
    
    
    
}
