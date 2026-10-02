package com.longobuccodev.app_adm_obras.core.security;

import com.longobuccodev.app_adm_obras.core.domain.User;

public interface AuthenticationGateway {

    User authenticate(String email, String rawPassword);
}
