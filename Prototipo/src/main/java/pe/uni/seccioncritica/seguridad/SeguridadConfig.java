package pe.uni.seccioncritica.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SeguridadConfig {
    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/recuperar", "/restablecer", "/error",
                                "/*.css", "/*.js", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(f -> f
                        .loginPage("/login")
                        .usernameParameter("codigo")
                        .passwordParameter("clave")
                        .defaultSuccessUrl("/oferta")
                        .failureUrl("/login?error"))
                .rememberMe(r -> r
                        .rememberMeParameter("recordarme")
                        .tokenValiditySeconds(7 * 24 * 60 * 60))
                .logout(l -> l.logoutSuccessUrl("/login?salida"))
                // El enlace de recuperación lleva el token en la URL: no debe filtrarse a otros sitios.
                .headers(h -> h.referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    @Bean
    PasswordEncoder codificadorClaves() { return new BCryptPasswordEncoder(); }
}
