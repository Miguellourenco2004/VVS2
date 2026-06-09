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
/**
 * Testes unitários para a UserService.
 *
 * Utiliza Mockito para simular as respostas da base de dados e a injeção do codificador de passwords,
 * garantindo o funcionamento correto do registo de contas ,
 * a encriptação de credenciais e a validação de utilizadores existentes.
 */
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
     * Testa o Branch  onde o utilizador ja existe
     * Cobre o 'if (userRepository.existsByUsername(username))' como true .
     */
    @Test
    void registerDuplicateUsernameFails() {

        when(userRepository.existsByUsername("miguel")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register("miguel", "miguel@email.com", "benfica");
        });

        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Testa o Branch de sucesso do registo.
     * Cobre a criação, codificação e persistência do utilizador  .
     */
    @Test
    void registerSuccess() {

        //usern  ainda não existe
        when(userRepository.existsByUsername("maria")).thenReturn(false);

        // Simula a encriptação da password
        when(passwordEncoder.encode("benfica")).thenReturn("benficahash");

        // Preparar o utilizador retornado pelo save
        User userRetornado = mock(User.class);

        // Simula o save do utilizador
        when(userRepository.save(any(User.class))).thenReturn(userRetornado);

        User user = userService.register("miguel", "miguelou04@email.com", "Benfica123");


        // Verifica  se o utilzador nao é null e save aparecer 1 vez
        assertNotNull(user);
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Testa o caso de sucesso do requireByUsername.
     */
    @Test
    void requireByUsername() {
        // Preparar os testes criar o user e quando chamada miguel retorna o user
        User user = mock(User.class);
        when(userRepository.findByUsername("miguel")).thenReturn(Optional.of(user));

        // Procura user
        User res = userService.requireByUsername("miguel");

        // Verifica se o utilizador retornado é o esperado
        assertEquals(user, res);
    }

    /**
     * Testa o Branch onde o utilizador não é encontrado.
     * Cobre o orElseThrow
     */
    @Test
    void requireByUsernameNotFound() {
        // Simula que o utilizador não existe
        when(userRepository.findByUsername("mIGUEL")).thenReturn(Optional.empty());


        // Verifica se é lançado exetion por nao ser encontrado

        assertThrows(IllegalArgumentException.class, () -> {
            userService.requireByUsername("mIGUEL");
        });
    }
}