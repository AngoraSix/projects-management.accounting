package com.angorasix.projects.management.accounting.infrastructure.security

import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 *
 *
 * All Spring Security configuration.
 *
 *
 * @author rozagerardo
 */
class ProjectManagementIntegrationsSecurityConfiguration private constructor() {

    companion object {
        /**
         *
         *
         * Security Filter Chain setup.
         *
         *
         * @param http Spring's customizable ServerHttpSecurity bean
         * @return fully configured SecurityWebFilterChain
         */
        fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
            http.authorizeExchange { exchanges: ServerHttpSecurity.AuthorizeExchangeSpec ->
                exchanges
                    .pathMatchers(
                        HttpMethod.GET,
                        "/management-accounting/**",
                    ).permitAll()
                    .anyExchange().authenticated()
            }.oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            }
            return http.build()
        }
    }
}
