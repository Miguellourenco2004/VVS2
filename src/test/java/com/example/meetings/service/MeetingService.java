package com.example.meetings.service;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MeetingServiceTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MeetingService meetingService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    /**
     * Testa o ramo (Branch) de ERRO na validação de datas do método propose.
     * Cobre a linha 'if (!end.isAfter(start))' lançando exceção se a data de fim for anterior à de início.
     */
    @Test
    void propose_DataFimInvalida_LancaExcecao() {
        User dono = new User("dono", "dono@mail.com", "pass");
        Instant inicio = Instant.now();
        Instant fim = inicio.minusSeconds(3600); // Fim no passado!

        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.propose(dono, "Titulo", "Desc", inicio, fim, List.of());
        });

        // Verifica que o save não é chamado (Branch foi interrompido)
        verify(meetingRepository, never()).save(any());
    }

    /**
     * Testa o ramo (Branch) de SUCESSO do método propose.
     * Cobre o loop 'for', a linha do 'if (normalized.isEmpty() || !seen.add(normalized))' para convidados válidos
     * e garante a Line Coverage da persistência na base de dados.
     */
    @Test
    void propose_DadosValidos_GuardaReuniaoComConvidados() {
        User dono = new User("dono", "dono@mail.com", "pass");
        User convidado = new User("pedro", "pedro@mail.com", "pass");

        when(userRepository.findByUsername("pedro")).thenReturn(Optional.of(convidado));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(i -> i.getArguments()[0]);

        Meeting resultado = meetingService.propose(dono, "Título", "Desc",
                Instant.now(), Instant.now().plusSeconds(3600), List.of("pedro"));

        assertNotNull(resultado);
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    /**
     * Testa o ramo (Branch) de ERRO de validação de Status no método respond.
     * Cobre as linhas de decisão onde um estado como 'PENDING' não pode ser enviado na resposta.
     */
    @Test
    void respond_StatusInvalido_LancaExcecao() {
        User convidado = new User("pedro", "pedro@mail.com", "pass");

        // Tentamos responder com "PENDING", o que deve acionar o 'if' e falhar
        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.respond(1L, convidado, InviteStatus.PENDING);
        });
    }

    /**
     * Testa o ramo (Branch) de SUCESSO no método respond.
     * Cobre as linhas onde o convite é encontrado e o Status é atualizado.
     */
    @Test
    void respond_StatusAceite_AtualizaConvite() {
        // 1. Mock do utilizador: não precisamos de 'setId', apenas que o getter funcione
        User convidado = mock(User.class);
        when(convidado.getId()).thenReturn(100L);

        // 2. Mock do participante: contorna o construtor protegido
        MeetingParticipant convite = mock(MeetingParticipant.class);

        // 3. Configura o comportamento do mock do repositório
        when(participantRepository.findByMeetingIdAndUserId(1L, 100L))
                .thenReturn(Optional.of(convite));

        // 4. Ação: o serviço chamará o setStatus no mock
        meetingService.respond(1L, convidado, InviteStatus.ACCEPTED);

        // 5. Verificação: Verifica se o método setStatus foi chamado no mock com o valor correto
        verify(convite).setStatus(InviteStatus.ACCEPTED);
    }
}