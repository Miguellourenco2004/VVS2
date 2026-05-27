package com.example.meetings.service;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Testa o ramo (Branch) onde o utilizador já existe.
     * Cobre o 'if (userRepository.existsByUsername(username))' como VERDADEIRO.
     */
    @Test
    void register_UtilizadorJaExiste_LancaExcecao() {
        when(userRepository.existsByUsername("joao")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register("joao", "joao@email.com", "senha123");
        });

        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Testa o ramo (Branch) de sucesso do registo.
     * Cobre a criação, codificação e persistência do utilizador (Line Coverage).
     */
    @Test
    void register_UtilizadorNovo_GuardaERetornaUtilizador() {
        when(userRepository.existsByUsername("maria")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash");

        // Mock do utilizador para validar o retorno sem depender de setters inexistentes
        User userRetornado = mock(User.class);
        when(userRepository.save(any(User.class))).thenReturn(userRetornado);

        User resultado = userService.register("maria", "maria@email.com", "senha123");

        assertNotNull(resultado);
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Testa o caso de sucesso do requireByUsername.
     */
    @Test
    void requireByUsername_EncontraUtilizador_RetornaUser() {
        User user = mock(User.class);
        when(userRepository.findByUsername("tiago")).thenReturn(Optional.of(user));

        User resultado = userService.requireByUsername("tiago");

        assertEquals(user, resultado);
    }

    /**
     * Testa o ramo (Branch) onde o utilizador não é encontrado.
     * Cobre o orElseThrow (Line Coverage).
     */
    @Test
    void requireByUsername_NaoEncontraUtilizador_LancaExcecao() {
        when(userRepository.findByUsername("desconhecido")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            userService.requireByUsername("desconhecido");
        });
    }
}