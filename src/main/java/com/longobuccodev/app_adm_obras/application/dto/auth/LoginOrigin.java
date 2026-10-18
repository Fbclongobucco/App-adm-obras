package com.longobuccodev.app_adm_obras.application.dto.auth;

/** De onde veio o login: IP e cabecalho User-Agent da requisicao. Os dois podem faltar. */
public record LoginOrigin(String ipAddress, String userAgent) {

    public static final LoginOrigin UNKNOWN = new LoginOrigin(null, null);
}
