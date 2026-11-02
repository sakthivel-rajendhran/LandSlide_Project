package com.slopeguard.service;

import java.util.Map;

public interface PasswordResetService {

    Map<String, Object> requestReset(String email);

    Map<String, Object> resetPassword(String token, String newPassword);
}
