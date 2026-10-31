package com.course.gateway.auth;

import com.course.common.DemoLearners;
import com.course.common.JwtSupport;
import com.course.common.RoleSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final String secret;
    private final PasswordEncoder encoder;
    private final String sharedHash;
    private final Map<String, String> passwordHashes;

    public AuthService(@Value("${security.jwt.secret}") String secret, PasswordEncoder encoder) {
        this.secret = secret;
        this.encoder = encoder;
        this.sharedHash = encoder.encode(DemoLearners.DEMO_PASSWORD);
        this.passwordHashes = DemoLearners.ALL.stream()
                .collect(Collectors.toUnmodifiableMap(DemoLearners.Person::learnerId, person -> sharedHash));
    }

    public TokenResponse login(String username, String password) {
        if (username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        String id = username.trim();
        if (RoleSupport.ADMIN_USERNAME.equals(id)) {
            if (!encoder.matches(password, sharedHash)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
            }
            return token(RoleSupport.ADMIN_USERNAME, RoleSupport.ADMIN_DISPLAY_NAME, RoleSupport.ADMIN);
        }
        String hash = passwordHashes.get(id);
        DemoLearners.Person person = DemoLearners.find(id);
        if (hash == null || person == null || !encoder.matches(password, hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        return token(person.learnerId(), person.displayName(), RoleSupport.USER);
    }

    private TokenResponse token(String id, String displayName, String role) {
        return new TokenResponse(
                JwtSupport.issue(secret, id, displayName, role),
                id,
                displayName,
                JwtSupport.TOKEN_HOURS * 3600,
                role);
    }
}
