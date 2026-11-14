package com.kotopai.outing.port;

/**
 * Port: who is making the current request. Defined by the outing feature, implemented by whatever authentication the
 * application uses, so controllers never read the security context themselves.
 */
public interface CurrentUserProvider {

    /**
     * @return the id of the logged-in user
     * @throws org.springframework.security.core.AuthenticationException if nobody is logged in
     */
    Long currentUserId();
}
