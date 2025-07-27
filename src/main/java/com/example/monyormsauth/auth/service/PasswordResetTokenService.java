package com.example.monyormsauth.service;

import com.example.monyormsauth.exception.UserNotFoundException;
import com.example.monyormsauth.model.entity.AppUser;
import com.example.monyormsauth.model.entity.PasswordResetToken;
import com.example.monyormsauth.repository.PasswordResetTokenRepository;
import com.example.monyormsauth.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }


    public PasswordResetToken createToken(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Əvvəlki token varsa sil
        tokenRepository.deleteByUser_Id(user.getId());

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(3600)) // 1 saat müddət
                .build();

        return tokenRepository.save(token);
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public void verifyExpiration(PasswordResetToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new UserNotFoundException("Password reset token expired");
        }
    }

    public void deleteByUserId(Long userId) {
        tokenRepository.deleteByUser_Id(userId);
    }
}
