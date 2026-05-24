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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MeetingRepositoryTest {

    @Autowired
    private MeetingRepository meetingRepository;

    // Usamos o TestEntityManager (ferramenta nativa do Spring para testes JPA)
    // para inserir dados na base de dados antes de corrermos a nossa query.
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findCalendarMeetingsReturnsMeetingsWhereUserIsOrganizerOrAcceptedPendingParticipant() {
        // ARRANGE: Preparar a base de dados com utilizadores e reuniões reais
        User alice = new User("alice", "alice@email.com", "senha");
        User bob = new User("bob", "bob@email.com", "senha");
        entityManager.persist(alice);
        entityManager.persist(bob);

        Instant agora = Instant.now();

        // 1. Reunião onde o Bob é o Organizador
        Meeting reuniaoOrganizada = new Meeting("Reunião do Bob", "Desc", agora, agora.plus(1, ChronoUnit.HOURS), bob);
        entityManager.persist(reuniaoOrganizada);

        // 2. Reunião onde a Alice organiza, e convida o Bob (Ele está PENDING)
        Meeting reuniaoPendente = new Meeting("Convite Pendente", "Desc", agora, agora.plus(1, ChronoUnit.HOURS), alice);
        reuniaoPendente.addParticipant(new MeetingParticipant(reuniaoPendente, bob, InviteStatus.PENDING));
        entityManager.persist(reuniaoPendente);

        // 3. Reunião onde a Alice organiza, e convida o Bob (Ele DECLINED) - ESTA NÃO DEVE APARECER!
        Meeting reuniaoRecusada = new Meeting("Convite Recusado", "Desc", agora, agora.plus(1, ChronoUnit.HOURS), alice);
        reuniaoRecusada.addParticipant(new MeetingParticipant(reuniaoRecusada, bob, InviteStatus.DECLINED));
        entityManager.persist(reuniaoRecusada);

        entityManager.flush(); // Força a escrita das entidades na BD H2

        // ACT: Executar a query manual JPQL que queremos testar
        List<Meeting> calendarioDoBob = meetingRepository.findCalendarMeetings(bob);

        // ASSERT: Verificamos se a query filtrou corretamente
        assertEquals(2, calendarioDoBob.size()); // Apenas a "Reunião do Bob" e a "Convite Pendente"

        boolean temRecusada = calendarioDoBob.stream()
                .anyMatch(m -> m.getTitle().equals("Convite Recusado"));
        assertFalse(temRecusada, "A query não deve devolver reuniões que o utilizador recusou!");
    }

    @Test
    void findOverlappingReturnsOnlyMeetingsWithinTimeWindow() {
        // ARRANGE
        User alice = new User("alice", "alice@email.com", "senha");
        entityManager.persist(alice);

        Instant inicioJanela = Instant.parse("2026-01-01T10:00:00Z");
        Instant fimJanela = Instant.parse("2026-01-01T12:00:00Z");

        // Reunião totalmente dentro da janela (10:30 às 11:30)
        Meeting dentro = new Meeting("Dentro", "Desc",
                Instant.parse("2026-01-01T10:30:00Z"),
                Instant.parse("2026-01-01T11:30:00Z"), alice);
        entityManager.persist(dentro);

        // Reunião fora da janela (13:00 às 14:00)
        Meeting fora = new Meeting("Fora", "Desc",
                Instant.parse("2026-01-01T13:00:00Z"),
                Instant.parse("2026-01-01T14:00:00Z"), alice);
        entityManager.persist(fora);

        entityManager.flush();

        // ACT
        List<Meeting> overlapping = meetingRepository.findOverlapping(alice, inicioJanela, fimJanela);

        // ASSERT
        assertEquals(1, overlapping.size());
        assertEquals("Dentro", overlapping.get(0).getTitle());
    }

    @Test
    void findCalendarMeetingsReturnsDistinctAndOrderedMeetings() {
        // ARRANGE
        User carlos = new User("carlos", "carlos@email.com", "senha");
        entityManager.persist(carlos);

        Instant agora = Instant.now();

        // Reunião 2 (Mais tarde)
        Meeting reuniaoTarde = new Meeting("Reunião Tarde", "", agora.plusSeconds(7200), agora.plusSeconds(10000), carlos);
        // O Carlos é organizador E participante (Isto causaria duplicação sem o DISTINCT)
        reuniaoTarde.addParticipant(new MeetingParticipant(reuniaoTarde, carlos, InviteStatus.ACCEPTED));
        entityManager.persist(reuniaoTarde);

        // Reunião 1 (Mais cedo)
        Meeting reuniaoCedo = new Meeting("Reunião Cedo", "", agora, agora.plusSeconds(3600), carlos);
        reuniaoCedo.addParticipant(new MeetingParticipant(reuniaoCedo, carlos, InviteStatus.ACCEPTED));
        entityManager.persist(reuniaoCedo);

        entityManager.flush();

        // ACT
        List<Meeting> meetings = meetingRepository.findCalendarMeetings(carlos);

        // ASSERT
        assertEquals(2, meetings.size(), "O DISTINCT deve garantir que as reuniões não vêm duplicadas");
        // Verifica a ordenação do ORDER BY m.startTime
        assertEquals("Reunião Cedo", meetings.get(0).getTitle());
        assertEquals("Reunião Tarde", meetings.get(1).getTitle());
    }

    @Test
    void findOverlappingDetectsPartialOverlaps() {
        // ARRANGE
        User ana = new User("ana", "ana@email.com", "senha");
        entityManager.persist(ana);

        // A nossa janela de pesquisa é das 12h00 às 14h00
        Instant inicioPesquisa = Instant.parse("2026-05-10T12:00:00Z");
        Instant fimPesquisa = Instant.parse("2026-05-10T14:00:00Z");

        // 1. Interceção Parcial: Começa às 11h e acaba às 13h (Apanha a primeira hora da janela)
        Meeting overlapInicio = new Meeting("Overlap Início", "",
                Instant.parse("2026-05-10T11:00:00Z"),
                Instant.parse("2026-05-10T13:00:00Z"), ana);
        entityManager.persist(overlapInicio);

        // 2. Interceção Parcial: Começa às 13h e acaba às 15h (Apanha a última hora da janela)
        Meeting overlapFim = new Meeting("Overlap Fim", "",
                Instant.parse("2026-05-10T13:00:00Z"),
                Instant.parse("2026-05-10T15:00:00Z"), ana);
        entityManager.persist(overlapFim);

        // 3. Sem Interceção: Acaba exatamente quando a janela começa (11h às 12h)
        Meeting encostada = new Meeting("Encostada", "",
                Instant.parse("2026-05-10T11:00:00Z"),
                Instant.parse("2026-05-10T12:00:00Z"), ana);
        entityManager.persist(encostada);

        entityManager.flush();

        // ACT
        List<Meeting> overlaps = meetingRepository.findOverlapping(ana, inicioPesquisa, fimPesquisa);

        // ASSERT
        assertEquals(2, overlaps.size());
        assertTrue(overlaps.stream().anyMatch(m -> m.getTitle().equals("Overlap Início")));
        assertTrue(overlaps.stream().anyMatch(m -> m.getTitle().equals("Overlap Fim")));
        assertFalse(overlaps.stream().anyMatch(m -> m.getTitle().equals("Encostada")), "Reuniões que tocam nos limites não devem sobrepor");
    }
}