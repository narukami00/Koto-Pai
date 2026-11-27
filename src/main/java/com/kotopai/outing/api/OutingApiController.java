package com.kotopai.outing.api;

import com.kotopai.outing.api.dto.OutingDtos.CreateOutingRequest;
import com.kotopai.outing.api.dto.OutingDtos.OutingContentsRequest;
import com.kotopai.outing.api.dto.OutingDtos.OutingResponse;
import com.kotopai.outing.api.dto.OutingDtos.OutingSummary;
import com.kotopai.outing.api.dto.OutingDtos.PreviewResponse;
import com.kotopai.outing.port.CurrentUserProvider;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thin REST layer: parse and validate, ask who the user is, delegate, return. All rules live in the services. */
@RestController
@RequestMapping("/api/outings")
public class OutingApiController {

    private final OutingApiFacade outings;
    private final CurrentUserProvider currentUser;

    public OutingApiController(OutingApiFacade outings, CurrentUserProvider currentUser) {
        this.outings = outings;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<OutingResponse> create(@Valid @RequestBody CreateOutingRequest request) {
        OutingResponse created = outings.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/outings/" + created.id())).body(created);
    }

    /** Live split preview: calculates unsaved input, saves nothing. Incomplete input is a normal 200 with issues. */
    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody OutingContentsRequest request) {
        return outings.preview(currentUser.currentUserId(), request);
    }

    @GetMapping
    public List<OutingSummary> list(@RequestParam(defaultValue = "20") int limit) {
        return outings.list(currentUser.currentUserId(), limit);
    }

    @GetMapping("/{id}")
    public OutingResponse get(@PathVariable Long id) {
        return outings.get(currentUser.currentUserId(), id);
    }

    @PutMapping("/{id}")
    public OutingResponse update(@PathVariable Long id, @Valid @RequestBody OutingContentsRequest request) {
        return outings.updateContents(currentUser.currentUserId(), id, request);
    }

    @PostMapping("/{id}/submit")
    public OutingResponse submit(@PathVariable Long id) {
        return outings.submit(currentUser.currentUserId(), id);
    }

    @PostMapping("/{id}/cancel")
    public OutingResponse cancel(@PathVariable Long id) {
        return outings.cancel(currentUser.currentUserId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        outings.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
