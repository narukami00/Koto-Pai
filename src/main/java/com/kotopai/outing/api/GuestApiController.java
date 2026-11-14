package com.kotopai.outing.api;

import com.kotopai.outing.api.dto.GuestDtos.CreateGuestRequest;
import com.kotopai.outing.api.dto.GuestDtos.GuestResponse;
import com.kotopai.outing.port.CurrentUserProvider;
import com.kotopai.outing.service.GuestService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guests")
public class GuestApiController {

    private final GuestService guests;
    private final CurrentUserProvider currentUser;

    public GuestApiController(GuestService guests, CurrentUserProvider currentUser) {
        this.guests = guests;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<GuestResponse> list() {
        return guests.list(currentUser.currentUserId()).stream().map(GuestResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<GuestResponse> create(@Valid @RequestBody CreateGuestRequest request) {
        GuestResponse created = GuestResponse.from(guests.create(currentUser.currentUserId(), request.name()));
        return ResponseEntity.created(URI.create("/api/guests/" + created.id())).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        guests.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
