package com.fiap.apitotvs.security;

import com.fiap.apitotvs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var playerOpt = userRepository.findByEmail(email);
        if (playerOpt.isPresent()) {
            return (UserDetails) playerOpt.get();
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}