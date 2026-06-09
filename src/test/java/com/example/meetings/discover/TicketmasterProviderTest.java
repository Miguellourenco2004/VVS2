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

/**
 * Testes de integração do provider Ticketmaster.
 *
 * Utiliza WireMock para simular respostas da API externa
 * sem necessidade de chamadas reais ao Ticketmaster.
 */
@SpringBootTest(properties = {
        "app.discover.ticketmaster.api-key=fake-api-key",
        "app.discover.ticketmaster.country-code=PT",
        "app.discover.ticketmaster.base-url=http://localhost:8089"
})
class TicketmasterProviderTest {

    @Autowired
    private TicketmasterProvider ticketmasterProvider;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        // Inicializa servidor WireMock
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
    void searchReturnsEventsSuccess() {
        // Preparar JSON  da API Ticketmaster
        String jsonResponse = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "tm-555",
                    "name": "Concerto Eminem",
                    "url": "https://ticketmaster.pt/rap",
                    "info": "The real slim",
                    "dates": {
                      "start": {
                        "dateTime": "2026-06-24T17:30:00Z"
                      }
                    },
                    "_embedded": {
                      "venues": [
                        { "name": "Estádio da Luz" }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        // Quando existir pedido GET para /events.json
        // com os parâmetros corretos, devolve JSON mockado
        stubFor(get(urlPathEqualTo("/events.json"))
                .withQueryParam("keyword", equalTo("eminem"))
                .withQueryParam("apikey", equalTo("fake-api-key"))
                .withQueryParam("countryCode", equalTo("PT"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponse)));

        // Pesquisa
        List<DiscoveredEvent> results = ticketmasterProvider.search("eminem");

        // Verificar se tem o resultado e se oes dados esta corretos
        assertEquals(1, results.size());
        DiscoveredEvent event = results.get(0);

        assertEquals("Ticketmaster", event.source());
        assertEquals("tm-555", event.externalId());
        assertEquals("Concerto Eminem", event.title());
        assertEquals("Estádio da Luz", event.venue());
        assertNotNull(event.start());
    }

    /**
     * Testa quando a API devolve erro.
     */
    @Test
    void searchEmptyOnApiError() {
        // Simula um erro 500
        stubFor(get(urlPathMatching("/events.json.*"))
                .willReturn(aResponse().withStatus(500)));

        // pesquisa
        List<DiscoveredEvent> results = ticketmasterProvider.search("eminem");

        // O catch deve impedir crash e devolver lista vazia
        assertTrue(results.isEmpty());
    }


    /**
     * Testa provider
     * não está configurado.
     */
    @Test
    void searchEmptyIfUnconfigured() {
        // Preparar provider sem API Key


        TicketmasterProvider unconfiguredProvider = new TicketmasterProvider(
                "", // API Key vazia!
                "PT",
                "http://localhost:8089"
        );

      // pesquisa
        List<DiscoveredEvent> results = unconfiguredProvider.search("taylor");

        // Verifica se devolve lista vazia e o isConfigured() funcionou
        assertTrue(results.isEmpty());
    }


    /**
     * Testa o evento
     * que não possui data de início.
     */
    @Test
    void searchExcludesEventsNoDate() {
        // Preparar JSON sem campo dateTime
        String jsonResponseWithoutDate = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "tm-888",
                    "name": "Tertulia do PCP",
                    "url": "https://ticketmaster.pt/pcp"
                  }
                ]
              }
            }
            """;

        // Simula resposta API
        stubFor(get(urlPathEqualTo("/events.json"))
                .withQueryParam("keyword", equalTo("pcp"))
                .withQueryParam("apikey", equalTo("fake-api-key"))   // apikey
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(jsonResponseWithoutDate)));

        // pesquisa
        List<DiscoveredEvent> results = ticketmasterProvider.search("pcp");

        //verifica que sem data time , a lista vai retornar vazia
        assertTrue(results.isEmpty());
    }
}