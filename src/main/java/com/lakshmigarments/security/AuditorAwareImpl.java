//package com.lakshmigarments.security;
//
//import org.springframework.data.domain.AuditorAware;
//import java.util.Optional;
//import java.util.UUID;
//
//public class AuditorAwareImpl implements AuditorAware<UUID> {
//    @Override
//    public Optional<UUID> getCurrentAuditor() {
//        // 1. Get the authentication object from the security context
//        return Optional.ofNullable(S.getContext().getAuthentication())
//                // 2. Ensure the user is actually authenticated
//                .filter(auth -> auth.isAuthenticated())
//                // 3. Extract the principal (your UserDetails object)
//                .map(auth -> (MyUserDetails) auth.getPrincipal())
//                // 4. Return the unique ID of the user
//                .map(MyUserDetails::getId);
//    }
//}
