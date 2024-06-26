package argolo.tech.springsecurity6.controller;

import argolo.tech.springsecurity6.controller.dto.LoginRequest;
import argolo.tech.springsecurity6.controller.dto.LoginResponse;
import argolo.tech.springsecurity6.entities.Role;
import argolo.tech.springsecurity6.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Collectors;

@CrossOrigin(maxAge = 3600)
@RestController
public class LoginController {

    private final JwtEncoder jwtEncoder;

    private  final UserRepository userRepository;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public LoginController(JwtEncoder jwtEncoder, UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest){
        var user = (userRepository.findByUserName(loginRequest.username())).isPresent()?
                userRepository.findByUserName(loginRequest.username()) :
                userRepository.findUserByCpf(loginRequest.username());
        if(user.isEmpty() || !user.get().isLoginCorrect(loginRequest, bCryptPasswordEncoder)){
            throw new BadCredentialsException("user or password invalid");
        }

        var now = Instant.now();
        var expiresIn = 30000L;

        var scopes = user.get().getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        var claims = JwtClaimsSet.builder()
                .issuer("mybackend")
                .subject(user.get().getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scopes)
                .build();

        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return ResponseEntity.ok(new LoginResponse(user, jwtValue, expiresIn, scopes));



    }


}
