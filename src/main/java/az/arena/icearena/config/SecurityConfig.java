// File: src/main/java/az/arena/icearena/config/SecurityConfig.java
package az.arena.icearena.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/sessions/public/**",
                                "/api/skates/**",
                                "/api/live/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/discounts/active").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/discounts/apply").permitAll()
                        .requestMatchers("/api/verification/**")
                        .hasAnyRole("ADMIN", "COORDINATOR", "CASHIER", "VOLUNTEER")
                        .requestMatchers("/api/volunteer/**")
                        .hasAnyRole("ADMIN", "COORDINATOR", "VOLUNTEER")
                        .requestMatchers("/api/coordinator/**")
                        .hasAnyRole("ADMIN", "COORDINATOR")
                        .requestMatchers("/api/shifts/overview")
                        .hasAnyRole("ADMIN", "COORDINATOR", "CASHIER")
                        .requestMatchers("/api/admin/pos/**")
                        .hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
