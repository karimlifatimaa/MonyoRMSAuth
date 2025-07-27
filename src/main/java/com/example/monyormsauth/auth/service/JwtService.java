package com.example.monyormsauth.service;

import com.example.monyormsauth.model.entity.AppUser;
import com.example.monyormsauth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final UserRepository userRepository;
    // application.yml faylından secret və expiration vaxtlarını oxuyur
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expirationMs}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpirationMs}")
    private long refreshExpirationMs;

    public JwtService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    // Tokenin içindən istifadəçi adını çıxarır (subject hissəsi)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Tokenin expiration tarixini çıxarır
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Tokenin içindən hər hansı bir claim-i çıxarmaq üçün ümumi metod
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Tokenin içindəki bütün claim-ləri çıxarır
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token.trim())  // <-- burda trim əlavə et
                .getBody();
    }

    // Token expiration tarixi keçibsə true qaytarır
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Tokenin etibarlılığını yoxlayır (username uyğun gəlir və expiration keçməyib)
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // UserDetails yoxlaması olmadan tokeni sadəcə sintaktik yoxlayır
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Access token yaradır (sadə versiya, təkcə username ilə)
    public String generateToken(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().stream()
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toList()));

        return createToken(claims, username, jwtExpirationMs);
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObject = claims.get("roles");
        if (rolesObject instanceof List<?>) {
            return ((List<?>) rolesObject).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // Refresh token yaradır (sadə versiya)
    public String generateRefreshToken(String username) {
        return createToken(new HashMap<>(), username, refreshExpirationMs);
    }

    // Token yaratma prosesi - claim-lər, müddət və imza alqoritmi
    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Secret string-dən HMAC-SHA imza üçün istifadə olunan Key yaradır
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
