package com.prunny.reviewservice.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class UserFeignClientInterceptor implements RequestInterceptor {

    private static final String BASE64_SECRET = "OTc0MzQ3YjIxODZhZTBlODc1M2U1ODg0MTA3NzA1YWNkYWEwNzhhOWRmM2NmMTFjZTIwNTJkMmRlOGZkYWMzY2YxMjU2OWY0NDI3M2QzMTEyZjI1MDIyYjMzYTY3ZTYwOGZhYzE1YTU3NTQ1ZDIwNTY5ZTg3Mzc2ZWRiNDRhN2Y=";

    @Override
    public void apply(RequestTemplate template) {
//        SecurityUtils.getCurrentUserJWT().ifPresent(s -> template.header(AUTHORIZATION_HEADER, String.format("%s %s", BEARER, s)));
        String internalJwt = generateInternalJwt(); // See below
        System.out.println("JWT SECRET: " + internalJwt);
        template.header("Authorization", "Bearer " + internalJwt);
    }

    private String generateInternalJwt() {
        byte[] keyBytes = Base64.getDecoder().decode(BASE64_SECRET);
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA512");

        return Jwts.builder()
            .claim("auth", List.of("INTERNAL_ADMIN"))
            .setSubject("review-service")
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }
}
