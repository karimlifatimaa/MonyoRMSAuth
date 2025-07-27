package com.example.monyormsauth.service;

import com.example.monyormsauth.dto.*;
import com.example.monyormsauth.exception.DuplicateException;
import com.example.monyormsauth.exception.InvalidCredentialsException;
import com.example.monyormsauth.exception.UserNotFoundException;
import com.example.monyormsauth.model.entity.AppUser;
import com.example.monyormsauth.model.entity.PasswordResetToken;
import com.example.monyormsauth.model.entity.RefreshToken;
import com.example.monyormsauth.model.enumerator.ERole;
import com.example.monyormsauth.repository.PasswordResetTokenRepository;
import com.example.monyormsauth.repository.RefreshTokenRepository;
import com.example.monyormsauth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository, RefreshTokenService refreshTokenService, PasswordResetTokenService passwordResetTokenService, PasswordResetTokenRepository passwordResetTokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    @Transactional

    public void updateUserRole(Long userId, ERole role) {
        log.info("Updating user role");
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        log.info("User found with id: " + userId);
        user.setRoles(new HashSet<>(Set.of(role)));
        log.info("set role: ");

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        refreshTokenRepository.deleteByUser_Id(userId);
        passwordResetTokenRepository.deleteByUser_Id(userId);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        userRepository.delete(user);
    }

    public AuthResponse register(RegisterRequest registerRequest) {

        log.info("Registering new user with username {}", registerRequest.getUsername());
        userRepository.findByUsername(registerRequest.getUsername()).ifPresent(user -> {
            log.warn("Username {} already exists", registerRequest.getUsername());
            throw new DuplicateException("Username already exists");
        });

        userRepository.findByEmail(registerRequest.getEmail()).ifPresent(user -> {
            log.warn("Email {} already exists", registerRequest.getEmail());
            throw new DuplicateException("Email already exists");
        });

        AppUser user = AppUser.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .roles(Set.of(ERole.USER))
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .build();

        userRepository.save(user);

        log.info("User {} registered successfully", user.getUsername());
        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
       // RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }

    public AuthResponse login(LoginRequest request) {

        log.info("User login attempt with identifier {}", request.getIdentifier());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getIdentifier(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Username or password is incorrect");
        }

        AppUser user = userRepository.findByUsername(request.getIdentifier())
                .or(() -> userRepository.findByEmail(request.getIdentifier()))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        log.info("User {} logged in successfully", user.getUsername());
        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        //RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {

        log.info("Refreshing access token using refresh token {}", request.getRefreshToken());
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UserNotFoundException("Refresh token not found or invalid."));

        refreshTokenService.verifyExpiration(refreshToken);

        AppUser user = userRepository.findById(refreshToken.getUser().getId())
                .orElseThrow(() -> new UserNotFoundException("User not found for refresh token."));

        String newAccessToken = jwtService.generateToken(user.getUsername());

        log.info("Access token refreshed for user {}", user.getUsername());
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())  // Refresh token eyni qalır
                .build();
    }

    public void logout(String username) {
        log.info("Logging out user {}", username);
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        refreshTokenService.deleteByUserId(user.getId());
        log.info("User {} logged out successfully", username);
    }



    @Transactional
    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {

        log.info("Password reset requested for email {}", forgotPasswordRequest.getEmail());
        AppUser user = userRepository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User with this email not found"));

        var token = passwordResetTokenService.createToken(user.getUsername());

        String resetLink = "http://localhost:8080/api/auth/reset-password?token=" + token.getToken();

        String emailBody = "<p>Salam,</p>"
                + "<p>Şifrəni yeniləmək üçün linkə klikləyin:</p>"
                + "<a href=\"" + resetLink + "\">Şifrəni Yenilə</a>"
                + "<p>Bu link 1 saat ərzində aktiv olacaq.</p>";

        emailService.sendEmail(user.getEmail(), "Şifrə Yeniləmə Linki", emailBody);

        log.info("Password reset email sent to {}", user.getEmail());

    }

    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        log.info("Resetting password using token {}", resetPasswordRequest.getToken());
        PasswordResetToken resetToken = passwordResetTokenService.findByToken(resetPasswordRequest.getToken())
                .orElseThrow(() -> new UserNotFoundException("Invalid or expired password reset token"));

        passwordResetTokenService.verifyExpiration(resetToken);

        AppUser user = userRepository.findByUsername(resetToken.getUser().getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepository.save(user);

        // Reset token istifadə olunduğu üçün silirik
        passwordResetTokenService.deleteByUserId(user.getId());
        log.info("Password reset successful for user {}", user.getUsername());

    }
}
