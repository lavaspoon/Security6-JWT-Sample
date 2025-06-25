package devlava.jwt.controller;

import devlava.jwt.dto.EventAvailableResponse;
import devlava.jwt.dto.MemberResponse;
import devlava.jwt.repository.TbLmsEventRepository;
import devlava.jwt.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/event")
public class EventController {

    @Autowired
    EventService eventService;

    @GetMapping
    public ResponseEntity<EventAvailableResponse> isAvailable(@AuthenticationPrincipal UserDetails userDetails) {
        EventAvailableResponse result = eventService.isAvailable(userDetails.getUsername());
        return ResponseEntity.ok(result);
    }
}
