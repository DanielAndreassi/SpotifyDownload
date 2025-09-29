package com.spotifyweb.service;

import com.spotifyweb.entity.User;
import com.spotifyweb.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findBySpotifyId(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getSpotifyId(),
                "", // Password não é usado para OAuth
                getAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findBySpotifyId(String spotifyId) {
        return userRepository.findBySpotifyId(spotifyId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createOrUpdateUser(String spotifyId, String displayName, String email, String accessToken, String refreshToken, String profileImageUrl) {
        Optional<User> existingUser = findBySpotifyId(spotifyId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            user.setProfileImageUrl(profileImageUrl);
            return saveUser(user);
        } else {
            User newUser = new User(spotifyId, displayName, email, accessToken);
            newUser.setRefreshToken(refreshToken);
            newUser.setProfileImageUrl(profileImageUrl);
            return saveUser(newUser);
        }
    }
}