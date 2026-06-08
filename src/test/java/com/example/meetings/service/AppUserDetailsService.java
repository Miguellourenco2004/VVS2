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
     * Testa Branch de Sucesso.
     * Cobre as linhas de execução onde o repositório encontra o utilizador e cria o objeto UserDetails com sucesso.
     */
    @Test
    void loadUserByUsernameSuccess() {
       // Preprar os dados
        User user = new User("Miguel", "Miguelou04@email.com", "benfica");
        when(userRepository.findByUsername("Miguel")).thenReturn(Optional.of(user));

        // executar a pesquisa
        UserDetails r = appUserDetailsService.loadUserByUsername("Miguel");

        // vefiricar se o resultado obtido foi o esperado de encontrar o username
        assertNotNull(r);
        assertEquals("Miguel", r.getUsername());
        assertEquals("benfica", r.getPassword());
    }

    /**
     * Testa Branch de ERRO no Optional.orElseThrow.
     * Cobre a linha em que é lançada a exceção UsernameNotFoundException.
     */
    @Test
    void loadUserByUsernameNotFound() {
        // Preparação
        when(userRepository.findByUsername("naoexiste")).thenReturn(Optional.empty());

        // verificar se o resultado obtifo foi o  esperado de lançar a exceção
        assertThrows(UsernameNotFoundException.class, () -> {
            appUserDetailsService.loadUserByUsername("naoexiste");
        });
    }
}