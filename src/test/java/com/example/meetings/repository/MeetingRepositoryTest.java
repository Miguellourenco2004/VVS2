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

    // Usamos o TestEntityManager
    // para inserir dados na base de dados antes de corrermos a nossa query.
    @Autowired
    private TestEntityManager entityManager;


    /**
     * Testa a pesquisa de reuniões do calendário.
     *
     * Deve devolver reuniões onde o utilizador
     * é organizador ou participante com estado
     * PENDING ou ACCEPTED.
     */
    @Test
    void findCalendarMeetingsSuccess() {

        // Preparar users
        User Miguel = new User("Miguel", "Miguel@email.com", "senha");
        User pilo = new User("pilo", "pilo@email.com", "senha");
        entityManager.persist(Miguel);
        entityManager.persist(pilo);

        Instant agora = Instant.now();

        // Reunião pilo é organizador
        Meeting reuniaoOrganizada = new Meeting("Reunião pilo", "Desc", agora, agora.plus(1, ChronoUnit.HOURS), pilo);
        entityManager.persist(reuniaoOrganizada);

        // Reunião pilo tem convite pendente
        Meeting reuniaoPendente = new Meeting("Convite Pendente", "Desc", agora, agora.plus(1, ChronoUnit.HOURS), Miguel);
        reuniaoPendente.addParticipant(new MeetingParticipant(reuniaoPendente, pilo, InviteStatus.PENDING));
        entityManager.persist(reuniaoPendente);

        // Reunião recusada pelo pilo
        Meeting reuniaoRecusada = new Meeting("Convite Recusado", "Desc", agora, agora.plus(1, ChronoUnit.HOURS), Miguel);
        reuniaoRecusada.addParticipant(new MeetingParticipant(reuniaoRecusada, pilo, InviteStatus.DECLINED));
        entityManager.persist(reuniaoRecusada);

        entityManager.flush();

        // Pesquisa reuniões visíveis no calendário do pilo
        List<Meeting> calendarioDoBob = meetingRepository.findCalendarMeetings(pilo);

        // verificar se aparecem aoenas duas reunioes
        assertEquals(2, calendarioDoBob.size()); // Apenas 2

        boolean temRecusada = calendarioDoBob.stream()
                .anyMatch(m -> m.getTitle().equals("Convite Recusado"));
        assertFalse(temRecusada);
    }



    /**
     * Testa a pesquisa de reuniões
     * ao mesmo tempo.
     */
    @Test
    void findOverlapping() {
        // Preparar user
        User miguel = new User("miguel", "miguel@email.com", "benfica");
        entityManager.persist(miguel);

        Instant inic = Instant.parse("2026-02-02T15:00:00Z");
        Instant fim = Instant.parse("2026-02-02T17:00:00Z");

        // Reunião dentro da tempo
        Meeting dentro = new Meeting("Dentro", "Desc",
                Instant.parse("2026-02-02T15:30:00Z"),
                Instant.parse("2026-02-02T16:30:00Z"), miguel);
        entityManager.persist(dentro);

        //reuniao fora do tempo
        Meeting fora = new Meeting("Fora", "Desc",
                Instant.parse("2026-02-02T18:00:00Z"),
                Instant.parse("2026-02-02T18:30:00Z"), miguel);
        entityManager.persist(fora);

        entityManager.flush();

        // Pesquisa reuniões sobrepostas
        List<Meeting> overlapping = meetingRepository.findOverlapping(miguel, inic, fim);

        // verificar que apenas aparece 1 reuniao valida e é a dentro
        assertEquals(1, overlapping.size());
        assertEquals("Dentro", overlapping.get(0).getTitle());
    }


    /**
     * Testa se as reuniões devolvidas
     * são distintas e ordenadas por data.
     */
    @Test
    void findCalendarMeetingsOrdered() {
        // Preparar user
        User Miguel = new User("Miguel", "Miguel@email.com", "benfica");
        entityManager.persist(Miguel);

        Instant agora = Instant.now();
        // Reunião mais tarde
        Meeting reuniaoTarde = new Meeting("Reunião Tarde", "", agora.plusSeconds(7200), agora.plusSeconds(10000), Miguel);

        reuniaoTarde.addParticipant(new MeetingParticipant(reuniaoTarde, Miguel, InviteStatus.ACCEPTED));
        entityManager.persist(reuniaoTarde);

        // Reunião mais cedo
        Meeting reuniaoCedo = new Meeting("Reunião Cedo", "", agora, agora.plusSeconds(3600), Miguel);
        reuniaoCedo.addParticipant(new MeetingParticipant(reuniaoCedo, Miguel, InviteStatus.ACCEPTED));
        entityManager.persist(reuniaoCedo);

        entityManager.flush();

        // pesuisar
        List<Meeting> meetings = meetingRepository.findCalendarMeetings(Miguel);

        // Verifica ausência de duplicados e ordenação por m.startTime
        assertEquals(2, meetings.size());
        assertEquals("Reunião Cedo", meetings.get(0).getTitle());
        assertEquals("Reunião Tarde", meetings.get(1).getTitle());
    }

    /**
     * Testa a deteção de sobreposições parciais
     * entre reuniões e uma janela temporal.
     */
    @Test
    void  findOverlappingReturnsPartial() {
        // Preparar user
        User miguel = new User("miguel", "miguel@email.com", "benfica");
        entityManager.persist(miguel);

        // Janela temporal
        Instant inicioPesquisa = Instant.parse("2026-05-20T12:00:00Z");
        Instant fimPesquisa = Instant.parse("2026-05-20T14:00:00Z");

        // Sobreposição no início da janela
        Meeting início = new Meeting("Início", "",
                Instant.parse("2026-05-20T11:00:00Z"),
                Instant.parse("2026-05-20T13:00:00Z"), miguel);
        entityManager.persist(início);

        // Sobreposição no fim da janela
        Meeting fim = new Meeting("Fim", "",
                Instant.parse("2026-05-20T13:00:00Z"),
                Instant.parse("2026-05-20T15:00:00Z"), miguel);
        entityManager.persist(fim);

        // Reunião que apenas toca no limite da janela do começo
        Meeting encostada = new Meeting("Encostada", "",
                Instant.parse("2026-05-20T11:00:00Z"),
                Instant.parse("2026-05-20T12:00:00Z"), miguel);
        entityManager.persist(encostada);

        entityManager.flush();

        // Pesquisa dentro do limiar
        List<Meeting> overlaps = meetingRepository.findOverlapping(miguel, inicioPesquisa, fimPesquisa);

        // Verifica se apenas 2 reuniões foram devolvidas e as certas
        assertEquals(2, overlaps.size());
        assertTrue(overlaps.stream().anyMatch(m -> m.getTitle().equals("Início")));
        assertTrue(overlaps.stream().anyMatch(m -> m.getTitle().equals("Fim")));
        assertFalse(overlaps.stream().anyMatch(m -> m.getTitle().equals("Encostada")));
    }
}