package com.example.meetings.repository;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MeetingParticipantRepositoryTest {

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private TestEntityManager entityManager;



    /**
     * Testa a pesquisa de participantes
     * por utilizador e estado do convite
     */
    @Test
    void findByUserAndStatus() {

        // Preparar users
        User user = new User("Miguel", "miguelou04@email.com", "benfica");
        User user1 = new User("roberto", "robs@email.com", "sporting");
        entityManager.persist(user);
        entityManager.persist(user1);

        Meeting reuniao = new Meeting("Reunião 1", "", Instant.now(), Instant.now().plusSeconds(3600), user);
        Meeting reuniao2 = new Meeting("Reunião 2", "", Instant.now(), Instant.now().plusSeconds(3600), user);
        entityManager.persist(reuniao);
        entityManager.persist(reuniao2);

        // O convidado aceitou a primeira reunião e segunda ainda nao
        entityManager.persist(new MeetingParticipant(reuniao, user1, InviteStatus.ACCEPTED));
        entityManager.persist(new MeetingParticipant(reuniao2, user1, InviteStatus.PENDING));
        entityManager.flush();

        // Convites pendentes
        List<MeetingParticipant> pendingInvites = participantRepository.findByUserAndStatus(user1, InviteStatus.PENDING);

        //  verifica 1 reuniao pendende e que é adois
        assertEquals(1, pendingInvites.size());
        assertEquals("Reunião 2", pendingInvites.get(0).getMeeting().getTitle());
        assertEquals(InviteStatus.PENDING, pendingInvites.get(0).getStatus());
    }


    /**
     * Testa a pesquisa de participante
     * através do ID da reunião e do ID do utilizador.
     */
    @Test
    void  findParticipantByMeetingAndUser() {
        // user
        User user = new User("Miguel", "Miguelou04@email.com", "benfica");
        entityManager.persist(user);

        // reuniao
        Meeting meeting = new Meeting("Reunião trabalho", "", Instant.now(), Instant.now().plusSeconds(3600), user);
        entityManager.persist(meeting);
        // Criar participante associado à reunião
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.ACCEPTED);
        entityManager.persistAndFlush(participant);

        Long meetingId = meeting.getId();
        Long userId = user.getId();

        // pesquisar pelo id
        Optional<MeetingParticipant> found = participantRepository.findByMeetingIdAndUserId(meetingId, userId);

        // verificar se o id do parcipicate  esta presente  e id da reuinao
        assertTrue(found.isPresent());
        assertEquals(meetingId, found.get().getMeeting().getId());
        assertEquals(userId, found.get().getUser().getId());
    }
}