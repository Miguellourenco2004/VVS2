package com.example.meetings.discover;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "app.discover.agendalx.base-url=http://localhost:8089"
})
class AgendaLxProviderTest {

    @Autowired
    private AgendaLxProvider agendaLxProvider;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        // Inicializa o servidor WireMock
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }


    /**
     * Testa a pesquisa de eventos
     * quando a API responde corretamente.
     */
    @Test
    void searcEventsWhenApiIsSuccessful() {
        // Preparar resposta JSON da API
        String jsonResponse = """
            [
              {
                "id": 1,
                "title": { "rendered": "FESTA da moita" },
                "description": ["<p>oFesta RIJA!</p>"],
                "occurences": ["2030-06-01"],
                "string_times": "qui&/sexta/sabado: 18.30",
                "link": "https://agendalx.pt/evento",
                "venue": {
                  "123": { "name": "Moita do Ribatejo" }
                }
              }
            ]
            """;
        //resposta da API para pesquisa
        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("search", equalTo("moita"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponse)));

        // Executa a pesquisa
        List<DiscoveredEvent> results = agendaLxProvider.search("moita");

        // verificar se obter 1 resultado
        assertEquals(1, results.size());
        DiscoveredEvent event = results.get(0);

        // vereficar se é o json
        assertEquals("Agenda Cultural de Lisboa", event.source());
        assertEquals("1", event.externalId());
        assertEquals("FESTA da moita", event.title());
        assertEquals("Moita do Ribatejo", event.venue());
        assertEquals("oFesta RIJA!", event.description());
    }

    /**
     * Testa  adicionar pesquicasa por um evento que ja acabou
     */
    @Test
    void searchIgnoresEventsWhoseOccurrencesAreAllInThePast() {
        // Preparar resposta JSON com evento antigo 2016
        String jsonResponsePastEvent = """
            [
              {
                "id": 777,
                "title": { "rendered": "Euro 2016" },
                "description": ["<p>celebrar a conquista do euro</p>"],
                "occurences": ["2016-06-01", "2016-07-02"],
                "string_times": "mes: 22h00",
                "link": "https://agendalx.pt/antigo"
              }
            ]
            """;
        // Resposta  API para pesquisa
        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("search", equalTo("euro"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponsePastEvent)));

        // pesquisa
        List<DiscoveredEvent> results = agendaLxProvider.search("antigo");

        //  O método nextOccurrence() não encontra datas no futuro,
        // devolve null, e os resultados estao vazios.
        assertTrue(results.isEmpty());
    }
}