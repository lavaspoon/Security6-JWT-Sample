package devlava.jwt.controller;

import devlava.jwt.dto.MemberResponse;
import devlava.jwt.entity.Member;
import devlava.jwt.repository.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRepository memberRepository;

    public MemberController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAllMembers(@AuthenticationPrincipal UserDetails userDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        List<MemberResponse> members = memberRepository.findAll().stream()
                .map(member -> new MemberResponse(
                        member.getId(),
                        member.getUsername(),
                        member.getName(),
                        member.getUsername().equals(currentUsername)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(members);
    }
}