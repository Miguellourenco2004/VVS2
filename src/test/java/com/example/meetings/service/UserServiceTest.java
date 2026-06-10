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
     * Testa o cenário de erro onde o user já existe.
     * Cobre a linha do if (userRepository.existsByUsername(username)) quando é True.
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
     * Testa o cenário de sucesso do registo.
     * Cobre as linhas responsáveis pela criação, codificação da password e persistência do user.
     */
    @Test
    void registerSuccess() {

        //usern  ainda não existe
        when(userRepository.existsByUsername("miguel")).thenReturn(false);

        // Simula a encriptação da password
        when(passwordEncoder.encode("benfica")).thenReturn("benficahash");

        // Preparar o user retornado pelo save
        User userRetornado = mock(User.class);

        // Simula o save do user
        when(userRepository.save(any(User.class))).thenReturn(userRetornado);

        User user = userService.register("miguel", "miguelou04@email.com", "Benfica123");


        // Verifica  se o utilzador nao é null e save aparecer 1 vez
        assertNotNull(user);
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Testa o cenário de sucesso na procura de user.
     * Cobre as linhas de pesquisa e retorno do user esperado.
     */
    @Test
    void requireByUsername() {
        // Preparar os testes criar o user e quando chamada miguel retorna o user
        User user = mock(User.class);
        when(userRepository.findByUsername("miguel")).thenReturn(Optional.of(user));

        // Procura user
        User res = userService.requireByUsername("miguel");

        // Verifica se o user retornado é o esperado
        assertEquals(user, res);
    }

    /**
     * Testa o cenário de erro onde o user não é encontrado.
     * Cobre a linha do orElseThrow, lançando a exceção IllegalArgumentException.
     */
    @Test
    void requireByUsernameNotFound() {
        // Simula que o user não existe
        when(userRepository.findByUsername("mIGUEL")).thenReturn(Optional.empty());


        // Verifica se é lançado exetion por nao ser encontrado

        assertThrows(IllegalArgumentException.class, () -> {
            userService.requireByUsername("mIGUEL");
        });
    }
}