package com.kotopai.outing.api.dto;

import com.kotopai.outing.domain.Guest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class GuestDtos {

    private GuestDtos() {
    }

    public record CreateGuestRequest(@NotBlank @Size(max = 80) String name) {
    }

    public record GuestResponse(Long id, String name) {

        public static GuestResponse from(Guest guest) {
            return new GuestResponse(guest.getId(), guest.getName());
        }
    }
}
