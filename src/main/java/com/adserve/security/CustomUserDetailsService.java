package com.adserve.security;

import com.adserve.entity.Advertiser;
import com.adserve.entity.User;
import com.adserve.repository.AdvertiserRepository;
import com.adserve.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdvertiserRepository advertiserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Long advertiserId = advertiserRepository.findByUserId(user.getId())
                .map(Advertiser::getId)
                .orElse(null);

        return UserPrincipal.create(user, advertiserId);
    }
}
