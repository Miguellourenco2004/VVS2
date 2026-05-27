
package com.example.meetings.repository;

import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUsernameReturnsUserWhenExists() {
        // ARRANGE
        User user = new User("joao_db", "joao@email.com", "senha");
        entityManager.persistAndFlush(user);

        // ACT
        Optional<User> found = userRepository.findByUsername("joao_db");

        // ASSERT
        assertTrue(found.isPresent());
        assertEquals("joao@email.com", found.get().getEmail());
    }

    @Test
    void existsByUsernameReturnsTrueWhenUsernameIsTaken() {
        // ARRANGE
        entityManager.persistAndFlush(new User("maria", "maria@email.com", "senha"));

        // ACT & ASSERT
        assertTrue(userRepository.existsByUsername("maria"));
        assertFalse(userRepository.existsByUsername("utilizador_inexistente"));
    }

    @Test
    void findByIcalTokenReturnsUserCorrectly() {
        // ARRANGE
        User user = new User("carlos", "carlos@email.com", "senha");
        entityManager.persistAndFlush(user);
        String generatedToken = user.getIcalToken(); // O token é gerado no construtor

        // ACT
        Optional<User> found = userRepository.findByIcalToken(generatedToken);

        // ASSERT
        assertTrue(found.isPresent());
        assertEquals("carlos", found.get().getUsername());
    }

    @Test
    void savingUserWithDuplicateUsernameThrowsException() {
        // ARRANGE: Inserimos o primeiro utilizador
        entityManager.persistAndFlush(new User("duplicado", "um@email.com", "senha"));

        // ACT & ASSERT: Tentamos inserir um segundo com o mesmo username
        User user2 = new User("duplicado", "dois@email.com", "senha");

        // Como a coluna tem unique=true, a base de dados tem de rejeitar e atirar um erro de integridade!
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}