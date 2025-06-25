package devlava.jwt.controller;

import devlava.jwt.dto.EventAvailableResponse;
import devlava.jwt.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
