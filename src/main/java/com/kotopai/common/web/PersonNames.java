package com.kotopai.common.web;

import com.kotopai.outing.repository.GuestRepository;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Turns user and guest ids into display names for page templates ({@code ${@people.nameOf(id)}}), so pages
 * show "Tanvir" instead of "User #2". Pages only list a handful of people, so one small lookup per name is fine.
 * Unknown ids fall back to a neutral label instead of failing the page.
 */
@Component("people")
public class PersonNames {

    static final String UNKNOWN_USER = "Someone";
    static final String UNKNOWN_GUEST = "Guest";

    private final UserService users;
    private final GuestRepository guests;

    public PersonNames(UserService users, GuestRepository guests) {
        this.users = users;
        this.guests = guests;
    }

    public String nameOf(Long userId) {
        if (userId == null) {
            return UNKNOWN_USER;
        }
        UserSummary summary = users.summariesOf(List.of(userId)).get(userId);
        return summary == null || summary.displayName() == null || summary.displayName().isBlank()
            ? UNKNOWN_USER
            : summary.displayName();
    }

    public String guestNameOf(Long guestId) {
        if (guestId == null) {
            return UNKNOWN_GUEST;
        }
        return guests.findById(guestId)
            .map(g -> g.getName())
            .filter(name -> name != null && !name.isBlank())
            .orElse(UNKNOWN_GUEST);
    }
}
