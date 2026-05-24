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
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void searchReturnsDiscoveredEventsWhenApiIsSuccessful() {
        // ARRANGE: Colocamos uma data futura (2030) para garantir que o
        // nextOccurrence() do AgendaLxProvider nunca a ignora.
        String jsonResponse = """
            [
              {
                "id": 999,
                "title": { "rendered": "Feira do Livro de Lisboa" },
                "description": ["<p>A maior feira do livro!</p>"],
                "occurences": ["2030-06-01"],
                "string_times": "qui: 15h30",
                "link": "https://agendalx.pt/evento",
                "venue": {
                  "123": { "name": "Parque Eduardo VII" }
                }
              }
            ]
            """;

        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("search", equalTo("livro"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponse)));

        // ACT
        List<DiscoveredEvent> results = agendaLxProvider.search("livro");

        // ASSERT
        assertEquals(1, results.size());
        DiscoveredEvent event = results.get(0);

        assertEquals("Agenda Cultural de Lisboa", event.source());
        assertEquals("999", event.externalId());
        assertEquals("Feira do Livro de Lisboa", event.title());
        assertEquals("Parque Eduardo VII", event.venue());

        // Verifica se limpou a tag <p> do HTML
        assertEquals("A maior feira do livro!", event.description());
    }

    @Test
    void searchIgnoresEventsWhoseOccurrencesAreAllInThePast() {
        // ARRANGE: Um evento cujas datas já passaram todas (ex: ano de 2010).
        String jsonResponsePastEvent = """
            [
              {
                "id": 777,
                "title": { "rendered": "Concerto Antigo" },
                "description": ["<p>Já aconteceu</p>"],
                "occurences": ["2010-01-01", "2010-01-02"],
                "string_times": "sex: 21h00",
                "link": "https://agendalx.pt/antigo"
              }
            ]
            """;

        stubFor(get(urlPathEqualTo("/events"))
                .withQueryParam("search", equalTo("antigo"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponsePastEvent)));

        // ACT
        List<DiscoveredEvent> results = agendaLxProvider.search("antigo");

        // ASSERT: O método nextOccurrence() não encontra datas no futuro,
        // devolve null, e o evento é ignorado (continue).
        assertTrue(results.isEmpty());
    }
}