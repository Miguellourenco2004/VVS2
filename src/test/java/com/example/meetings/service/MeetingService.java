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
     * Testa Branch de error na validação de datas do  propose.
     * Cobre a linha 'if (!end.isAfter(start))'  exceção se a  data fim < data inicio
     */
    @Test
    void propose_DataFimInvalida_LancaExcecao() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "benfica");
        Instant inicio = Instant.now();
        Instant fim = inicio.minusSeconds(3600); // o fim esta no passado

        // Verificar se o resultaco contem : a execao por o fim < inicio
        assertThrows(IllegalArgumentException.class, () -> {
            meetingService.propose(user, "Titulo", "Desc", inicio, fim, List.of());
        });

        // Verifica que o save não é chamdo logo nao foi guardo o propose nao foi guardado
        verify(meetingRepository, never()).save(any());
    }

    /**
     * Testa o Branch de susseful do propose.
     * Cobre o loop 'for', a linha do 'if (normalized.isEmpty() || !seen.add(normalized))' para convidados válidos
     * e garante a Line Coverage da persistência na base de dados.
     */
    @Test
    void propose_DadosValidos_GuardaReuniaoComConvidados() {
        // Preprar o teste
        User user = new User("Miguel", "Miguelou04@mail.com", "Benfica");
        User convite = new User("pedro", "pedro@mail.com", "Pedro");

        // Devolve o  user pedro quando for procurado
        when(userRepository.findByUsername("pedro")).thenReturn(Optional.of(convite));

        // Devolve a própria reunião guardada
        when(meetingRepository.save(any(Meeting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // resultado
        Meeting resultado = meetingService.propose(user, "Título", "Desc",
                Instant.now(), Instant.now().plusSeconds(3600), List.of("pedro"));

        // Verifica se o o resultado nao é null
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