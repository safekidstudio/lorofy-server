package com.lorofy.server.features.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    private static final String OTP_KEY_PREFIX = "otp:register:";
    private static final String COOLDOWN_KEY_PREFIX = "otp:cooldown:";
    private static final String SIGNUP_TOKEN_KEY_PREFIX = "signup:token:";

    private final SecureRandom secureRandom = new SecureRandom();

    public void generateOtpAndSendEmail(String email) {
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;

        // check cooldown
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long expire = redisTemplate.getExpire(cooldownKey);
            throw new IllegalStateException(
                    "Please wait " + (expire != null ? expire : 60) + " seconds before sending OTP again.");
        }

        // generate otp code
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));

        // store otp code in redis with expiry time of 5 minutes
        String otpKey = OTP_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(otpKey, otpCode, Duration.ofMinutes(5));

        // set cooldown time in redis for 60 seconds
        redisTemplate.opsForValue().set(cooldownKey, "locked", Duration.ofSeconds(60));

        // send email with otp code
        emailService.sendOtpEmail(email, otpCode);
    }

    public String verifyOtp(String email, String otpCode) {
        String otpKey = OTP_KEY_PREFIX + email;
        String cachedOtpCode = (String) redisTemplate.opsForValue().get(otpKey);

        if (cachedOtpCode == null) {
            throw new IllegalArgumentException("OTP code is invalid or expired.");
        }

        if (!cachedOtpCode.equals(otpCode)) {
            throw new IllegalArgumentException("OTP code is invalid.");
        }

        redisTemplate.delete(otpKey);

        String signupToken = UUID.randomUUID().toString();
        String tokenKey = SIGNUP_TOKEN_KEY_PREFIX + signupToken;
        redisTemplate.opsForValue().set(tokenKey, email, Duration.ofMinutes(15));
        return signupToken;
    }

    public String getEmailBySignupToken(String signupToken) {
        String tokenKey = SIGNUP_TOKEN_KEY_PREFIX + signupToken;
        String email = (String) redisTemplate.opsForValue().get(tokenKey);
        if (email == null) {
            throw new IllegalArgumentException("Signup token is invalid or expired.");
        }
        return email;
    }

    public void clearSignupToken(String signupToken) {
        String tokenKey = SIGNUP_TOKEN_KEY_PREFIX + signupToken;
        redisTemplate.delete(tokenKey);
    }
}
