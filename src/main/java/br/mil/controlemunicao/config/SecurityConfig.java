package br.mil.controlemunicao.config;

import br.mil.controlemunicao.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/login", "/error").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/usuarios/**", "/organizacoes/**", "/municoes/**", "/lotes/**", "/paiols/**", "/militares/**", "/viaturas/**", "/auditoria/**", "/admin/**").hasAnyRole("ADMINISTRADOR", "GESTOR")
                .requestMatchers("/movimentacoes/**", "/estoque/**", "/devolucoes/**").hasAnyRole("ADMINISTRADOR", "GESTOR", "OPERADOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> usuarioRepository.findByUsername(username)
            .map(usuario -> org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getUsername())
                .password(usuario.getSenha())
                .authorities(usuario.getAuthorities())
                .disabled(!usuario.isAtivo())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
