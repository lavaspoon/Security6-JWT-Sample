package devlava.jwt.controller;

import devlava.jwt.dto.EventAvailableResponse;
import devlava.jwt.dto.EventHistoryResponse;
import devlava.jwt.dto.EventResultResponse;
import devlava.jwt.dto.EventSaveRequest;
import devlava.jwt.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event")
public class EventController {

    @Autowired
    EventService eventService;

    @GetMapping("/check")
    public ResponseEntity<EventAvailableResponse> isAvailable(@AuthenticationPrincipal UserDetails userDetails) {
        EventAvailableResponse result = eventService.isAvailable(userDetails.getUsername());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save")
    public ResponseEntity<EventResultResponse> saveEvent(@RequestBody EventSaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        EventResultResponse result = eventService.save(request.isDraw(), userDetails.getUsername());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<EventHistoryResponse>> getEventHistory() {
        List<EventHistoryResponse> history = eventService.getEventHistory();
        return ResponseEntity.ok(history);
    }
}
