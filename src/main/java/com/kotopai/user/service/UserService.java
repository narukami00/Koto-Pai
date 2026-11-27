package com.kotopai.user.service;

import com.kotopai.user.dto.ProfileStatsView;
import com.kotopai.user.dto.UpdateProfileRequest;
import com.kotopai.user.dto.UserProfileResponse;
import com.kotopai.user.dto.UserSummary;

/**
 * Service for user profile management, lookups, and statistics.
 */
public interface UserService {

    /**
     * Looks up the public summaries of several users at once (names and avatars for pages and lists).
     * Unknown ids are simply absent from the result.
     *
     * @param userIds user ids
     * @return summaries keyed by user id
     */
    java.util.Map<Long, UserSummary> summariesOf(java.util.Collection<Long> userIds);


    /**
     * Retrieves the profile of a user by their ID.
     *
     * @param userId user ID
     * @return UserProfileResponse
     */
    UserProfileResponse getProfile(Long userId);

    /**
     * Updates profile details (display name, bio, avatar color) for a user.
     *
     * @param userId user ID
     * @param request profile update request payload
     * @return updated UserProfileResponse
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Looks up a user summary by their public handle (@userCode or userCode).
     *
     * @param userCode public user code
     * @return UserSummary
     */
    UserSummary findByCode(String userCode);

    /**
     * Calculates activity statistics (outings count, friends count, settled amount) for a user.
     *
     * @param userId user ID
     * @return ProfileStatsView
     */
    ProfileStatsView getStats(Long userId);
}

