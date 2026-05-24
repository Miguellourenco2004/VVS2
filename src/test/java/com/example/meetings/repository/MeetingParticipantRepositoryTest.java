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

    @Test
    void findByUserAndStatusReturnsCorrectParticipants() {
        // ARRANGE
        User organizer = new User("org", "org@email.com", "senha");
        User invitee = new User("convidado", "conv@email.com", "senha");
        entityManager.persist(organizer);
        entityManager.persist(invitee);

        Meeting meeting1 = new Meeting("Reunião 1", "", Instant.now(), Instant.now().plusSeconds(3600), organizer);
        Meeting meeting2 = new Meeting("Reunião 2", "", Instant.now(), Instant.now().plusSeconds(3600), organizer);
        entityManager.persist(meeting1);
        entityManager.persist(meeting2);

        // O convidado aceita a Reunião 1, mas está Pendente na Reunião 2
        entityManager.persist(new MeetingParticipant(meeting1, invitee, InviteStatus.ACCEPTED));
        entityManager.persist(new MeetingParticipant(meeting2, invitee, InviteStatus.PENDING));
        entityManager.flush();

        // ACT: Queremos buscar apenas os convites PENDENTES do convidado
        List<MeetingParticipant> pendingInvites = participantRepository.findByUserAndStatus(invitee, InviteStatus.PENDING);

        // ASSERT
        assertEquals(1, pendingInvites.size());
        assertEquals("Reunião 2", pendingInvites.get(0).getMeeting().getTitle());
        assertEquals(InviteStatus.PENDING, pendingInvites.get(0).getStatus());
    }

    @Test
    void findByMeetingIdAndUserIdReturnsCorrectParticipant() {
        // ARRANGE
        User user = new User("teste", "teste@email.com", "senha");
        entityManager.persist(user);

        Meeting meeting = new Meeting("Reunião X", "", Instant.now(), Instant.now().plusSeconds(3600), user);
        entityManager.persist(meeting);

        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.ACCEPTED);
        entityManager.persistAndFlush(participant);

        Long meetingId = meeting.getId();
        Long userId = user.getId();

        // ACT
        Optional<MeetingParticipant> found = participantRepository.findByMeetingIdAndUserId(meetingId, userId);

        // ASSERT
        assertTrue(found.isPresent());
        assertEquals(meetingId, found.get().getMeeting().getId());
        assertEquals(userId, found.get().getUser().getId());
    }
}