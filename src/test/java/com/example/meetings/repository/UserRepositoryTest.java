
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

    /**
     * Testa a pesquisa de utilizadores
     * pelo username.
     */
    @Test
    void findByUsername() {
        // preparar user
        User user = new User("Miguel", "miguelou04@email.com", "benfica");
        entityManager.persistAndFlush(user);

        // pesquiser o user
        Optional<User> found = userRepository.findByUsername("Miguel");

        //   // Verifica se o utilizador foi encontrado
        assertTrue(found.isPresent());
        assertEquals("joao@email.com", found.get().getEmail());
    }


    /**
     * Testa a verificação de usernames
     * já existentes ou nao  na base de dados.
     */
    @Test
    void existsByUsernameReturnsTrueWhenUsernameIsTaken() {
        // preparar user
        entityManager.persistAndFlush(new User("miguel", "miguelou04@email.com", "benfica"));

        // Verifica que priemiro user existe e segundo nao
        assertTrue(userRepository.existsByUsername("miguel"));
        assertFalse(userRepository.existsByUsername("macaco"));
    }


    /**
     * Testa a pesquisa de utilizadores
     * através do token iCal.
     */
    @Test
    void findByIcalTokenReturnsUserCorrectly() {
        // preparar user
        User user = new User("Miguel", "miguelou04@email.com", "benfica");
        entityManager.persistAndFlush(user);
        String generatedToken = user.getIcalToken(); // O token é gerado no construtor

        // pesquisa pelo token
        Optional<User> found = userRepository.findByIcalToken(generatedToken);

        // verifica se foi o user correto encontrado
        assertTrue(found.isPresent());
        assertEquals("Miguel", found.get().getUsername());
    }


    /**
     * Testa a inserção de
     * doius users com o mesmo
     * username
     */
    @Test
    void UserWithDuplicateUsername() {
        //  preprarr user 1
        entityManager.persistAndFlush(new User("miguel", "um@email.com", "benfica"));

        // preprar user 2
        User user2 = new User("miguel", "dois@email.com", "benfica");

        //   // Verifica se a base de dados rejeita o duplicado unique=true
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}