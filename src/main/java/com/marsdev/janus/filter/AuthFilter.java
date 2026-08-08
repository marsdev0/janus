package com.marsdev.janus.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.mapper.TokenMapper;
import com.marsdev.janus.model.TokenAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * @author geyan
 * @date 2026/8/2
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter {

    private static final String NULl_VALUE = "NULL";
    private static final Duration NULL_TTL = Duration.ofSeconds(60);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TokenMapper tokenMapper;

    public Mono<TokenAuth> authenticate(String authHeader) {
        String rawKey = stripBearer(authHeader);
        String keyHash = sha256(rawKey);
        if (keyHash == null) {
            return Mono.error(new JanusException(ErrorCode.UNAUTHORIZED));
        }
        String cacheKey = "token:" + keyHash;

        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(val -> NULl_VALUE.equals(val)
                        ? Mono.error(new JanusException(ErrorCode.UNAUTHORIZED))
                        : Mono.fromCallable(() -> deserialize(val)))
                .switchIfEmpty(Mono.defer(() -> loadFromDb(keyHash, cacheKey)));
    }

    private Mono<TokenAuth> loadFromDb(String keyHash, String cacheKey) {
        // JDBC is blocking, so wrap it with fromCallable
        return Mono.fromCallable(() -> tokenMapper.findByKeyHash(keyHash))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.defer(() -> {
                    // If findByKeyHash returns null, Mono.fromCallable emits an empty Mono, not a Mono carrying null
                    // Cache penetration protection: cache a null marker with a short TTL to stop random-key bombardment hitting the DB
                    return redisTemplate.opsForValue().set(cacheKey, NULl_VALUE, NULL_TTL)
                            .then(Mono.error(new JanusException(ErrorCode.UNAUTHORIZED)));
                }))
                .flatMap(t -> {
                    TokenAuth tokenAuth = new TokenAuth(t.getId(), t.getModels(), t.getStatus(), t.getExpiresAt());
                    // Fire-and-forget writeback: does not join the main flow, so cache failures don't affect authentication (next miss re-queries the source)
                    redisTemplate.opsForValue().set(cacheKey, serialize(tokenAuth), CACHE_TTL)
                            .subscribe(
                                    v -> {},
                                    e -> log.error("token cache write failed", e));
                    return Mono.just(tokenAuth);
                });
    }

    private String stripBearer(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        String s = authHeader.trim();
        if (s.startsWith("Bearer ")) {
            s = s.substring(7).trim();
        }
        return s.isBlank() ? null : s;
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.error("sha256 error ", e);
        }
        return null;
    }

    private String serialize(TokenAuth auth) {
        try {
            return objectMapper.writeValueAsString(auth);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private TokenAuth deserialize(String json) {
        try {
            return objectMapper.readValue(json, TokenAuth.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
