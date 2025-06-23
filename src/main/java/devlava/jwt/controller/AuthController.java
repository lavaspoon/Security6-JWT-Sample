package devlava.jwt.controller;

import devlava.jwt.config.JwtUtils;
import devlava.jwt.dto.AuthRequest;
import devlava.jwt.dto.AuthResponse;
import devlava.jwt.dto.RefreshTokenRequest;
import devlava.jwt.entity.Token;
import devlava.jwt.entity.Member;
import devlava.jwt.entity.Role;
import devlava.jwt.entity.Role.RoleType;
import devlava.jwt.repository.TokenRepository;
import devlava.jwt.repository.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final MemberRepository memberRepository;
    private final TokenRepository tokenRepository;
    private final JwtUtils jwtUtils;

    public AuthController(MemberRepository memberRepository,
            TokenRepository tokenRepository,
            JwtUtils jwtUtils) {
        this.memberRepository = memberRepository;
        this.tokenRepository = tokenRepository;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        // ID 중복 체크
        if (memberRepository.findById(request.getId()).isPresent()) {
            return ResponseEntity.badRequest().body("ID already exists");
        }
        // Username 중복 체크
        if (memberRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        Member member = new Member();
        member.setId(request.getId());
        member.setUsername(request.getUsername());
        member.setName(request.getName());

        // Role 설정 (선택적)
        if (StringUtils.hasText(request.getRoleName())) {
            try {
                RoleType roleType = RoleType.valueOf(request.getRoleName().toUpperCase());
                Role role = new Role();
                role.setId(request.getId());
                role.setRoleName(roleType);
                role.setMember(member);
                member.setRole(role);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid role name. Allowed values are: " +
                        String.join(", ", java.util.Arrays.stream(RoleType.values())
                                .map(Enum::name)
                                .toList()));
            }
        }

        memberRepository.save(member);

        return ResponseEntity.ok("Member registered successfully");
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        // ID로 로그인
        Member member = memberRepository.findByIdWithRole(request.getId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        String accessToken = jwtUtils.generateToken(member.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(member.getUsername());

        // 기존 토큰이 있다면 삭제
        tokenRepository.deleteByMemberId(member.getId());

        // 새로운 리프레시 토큰 저장
        Token token = new Token();
        token.setMember(member);
        token.setRefreshToken(refreshToken);
        token.setExpiryDate(Instant.now().plusMillis(jwtUtils.getRefreshExpiration()));
        tokenRepository.save(token);

        RoleType roleType = member.getRole() != null ? member.getRole().getRoleName() : null;
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, roleType));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        return tokenRepository.findByRefreshTokenWithMemberAndRole(request.getRefreshToken())
                .map(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        tokenRepository.delete(token);
                        return ResponseEntity.badRequest().body("Refresh token expired");
                    }

                    String accessToken = jwtUtils.generateToken(token.getMember().getUsername());
                    RoleType roleType = token.getMember().getRole() != null ? token.getMember().getRole().getRoleName()
                            : null;
                    return ResponseEntity.ok(new AuthResponse(accessToken, request.getRefreshToken(), roleType));
                })
                .orElse(ResponseEntity.badRequest().body("Refresh token not found"));
    }
}
