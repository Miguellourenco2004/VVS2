package com.example.meetings.service;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Testa o ramo (Branch) de SUCESSO.
     * Cobre as linhas de execução onde o repositório encontra o utilizador e cria o objeto UserDetails com sucesso.
     */
    @Test
    void loadUserByUsername_UtilizadorEncontrado_RetornaUserDetails() {
        // Preparação
        User utilizador = new User("carlos", "carlos@email.com", "hashDaPass");
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(utilizador));

        // Ação
        UserDetails resultado = appUserDetailsService.loadUserByUsername("carlos");

        // Verificação
        assertNotNull(resultado);
        assertEquals("carlos", resultado.getUsername());
        assertEquals("hashDaPass", resultado.getPassword());
    }

    /**
     * Testa o ramo (Branch) de ERRO no Optional.orElseThrow.
     * Cobre a linha em que é lançada a exceção UsernameNotFoundException.
     */
    @Test
    void loadUserByUsername_UtilizadorNaoExiste_LancaExcecao() {
        // Preparação
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        // Ação e Verificação
        assertThrows(UsernameNotFoundException.class, () -> {
            appUserDetailsService.loadUserByUsername("fantasma");
        });
    }
}