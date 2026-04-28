package com.smartcity.backend.controller;

import com.smartcity.backend.entity.User;
import com.smartcity.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private Environment environment;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_EXPIRATION_MILLIS = Duration.ofMinutes(5).toMillis();

    private static class OtpEntry {
        private final String otp;
        private final long expiresAt;

        public OtpEntry(String otp, long expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        public String getOtp() {
            return otp;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }

    // GET all users
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // REGISTER user
    @PostMapping("/register")
    public User register(@RequestBody User user) {

        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser != null) {
            throw new RuntimeException("User already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // LOGIN user
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {

        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser == null || !checkPasswordAndUpgrade(existingUser, user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        return ResponseEntity.ok(existingUser);
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody User user) {
        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser == null || !checkPasswordAndUpgrade(existingUser, user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String otp = generateOtp();
        otpStore.put(existingUser.getUsername(), new OtpEntry(otp, System.currentTimeMillis() + OTP_EXPIRATION_MILLIS));
        boolean emailSent = sendOtpEmail(existingUser.getUsername(), otp);

        if (emailSent) {
            return ResponseEntity.ok(Map.of("message", "OTP code sent to your email."));
        }
        return ResponseEntity.ok(Map.of("message", "OTP code generated and logged to the server console."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String otp = request.get("otp");

        if (username == null || otp == null) {
            return ResponseEntity.badRequest().body("Missing username or OTP");
        }

        OtpEntry entry = otpStore.get(username);
        if (entry == null || System.currentTimeMillis() > entry.getExpiresAt()) {
            return ResponseEntity.status(401).body("OTP expired or invalid");
        }

        if (!entry.getOtp().equals(otp.trim())) {
            return ResponseEntity.status(401).body("Invalid OTP");
        }

        otpStore.remove(username);
        User existingUser = userRepository.findByUsername(username);

        if (existingUser == null) {
            return ResponseEntity.status(401).body("Invalid user");
        }

        return ResponseEntity.ok(existingUser);
    }

    private boolean checkPasswordAndUpgrade(User existingUser, String rawPassword) {
        String storedPassword = existingUser.getPassword();
        if (storedPassword == null) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        if (storedPassword.equals(rawPassword)) {
            existingUser.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(existingUser);
            return true;
        }
        return false;
    }

    private String generateOtp() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private boolean sendOtpEmail(String to, String otp) {
        String host = environment.getProperty("spring.mail.host");
        if (mailSender == null || host == null || host.isBlank()) {
            System.out.println("OTP for " + to + ": " + otp);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("SmartCity Login OTP");
            message.setText("Your SmartCity one-time password is: " + otp + "\nThis code expires in 5 minutes.");
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("Unable to send OTP email: " + e.getMessage());
            System.out.println("OTP for " + to + ": " + otp);
            return false;
        }
    }
}