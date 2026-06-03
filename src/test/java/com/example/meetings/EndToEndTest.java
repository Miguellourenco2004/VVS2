package com.example.meetings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.EventProvider;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
// ACABAR COM OS HELPERDS
/**
 * End-to-End tests using Selenium WebDriver and a dedicated test database
 * Firefox runs in headless mode
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:file:./target/e2edb;AUTO_SERVER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.base-url=http://localhost"
})
public class EndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingParticipantRepository participantRepository;


    @MockBean(name = "ticketmasterProvider")
    private EventProvider mockEventProvider;

    private WebDriver driver;
    private WebDriverWait wait;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Setup ao firex fox para exevutar os testes
     *
     */
    @BeforeEach
    void setUp() {

        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");

        driver = new FirefoxDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                driver.manage().deleteAllCookies();
            } catch (Exception ignored) {}
            driver.quit();
        }
    }

    // ================= HELPERS =================

    private void register(String username, String email, String password) {
        driver.get(baseUrl() + "/register");
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        wait.until(ExpectedConditions.urlContains("/login"));
    }

    private void login(String username, String password) {
        driver.get(baseUrl() + "/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.clear();
        passwordField.clear();

        usernameField.sendKeys(username);
        passwordField.sendKeys(password);

        WebElement submit = driver.findElement(By.cssSelector("button[type=submit]"));
        wait.until(ExpectedConditions.elementToBeClickable(submit));
        submit.click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/calendar"),
                ExpectedConditions.urlContains("error")
        ));
    }



    /**
     * Testa  o registo com um utilizador que ja existe
     *
     */
    @Test
    void registerErrorUsernameAlreadyExists() {

        // Registar  user
        register("Miguel", "Miguelou04@gmail.com", "Benfica123");

        // registar outro user com o mesmo username
        driver.get(baseUrl() + "/register");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).sendKeys("Miguel");
        driver.findElement(By.id("email")).sendKeys("other@gmail.com");
        driver.findElement(By.id("password")).sendKeys("Benfica123");
        driver.findElement(By.cssSelector("button[type=submit]")).click();

        // Verificar se o deu erro a restirar o segundo devido ao mesmo user
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".error")));
        assertTrue(driver.getPageSource().contains("Username already taken"));
        assertTrue(driver.getCurrentUrl().contains("/register"));
    }




    /**
     * Testa o login mas colocando cardencias incorretas
     *
     */
    @Test
    void loginShowsErrorWhenCredentialsAreInvalid()  {
        // Registar user
        register("Miguel", "Miguelou04@gmail.com", "Benfica123");


        // Tentar login com a pass errada
        driver.get(baseUrl() + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).sendKeys("Miguel");
        driver.findElement(By.id("password")).sendKeys("ola");
        driver.findElement(By.cssSelector("button[type=submit]")).click();


        // verifcar que da erro ao logar por pass errada
        wait.until(ExpectedConditions.urlContains("error"));
        assertTrue(driver.getPageSource().contains("Invalid username or password"));

    }




    /**
     * Testa a criação de reuniões
     */
    /**
     * Testa a criação de reuniões
     */
    @Test
    void proposeMeetingCreatesMeeting() {

        // Registar e autenticar user
        register("Miguel", "Miguelou04@gmail.com", "Benfica123");
        login("Miguel", "Benfica123");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // marcar uma reuniao
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        driver.findElement(By.id("title")).sendKeys("lolzinho");
        driver.findElement(By.id("description")).sendKeys("descriçao");

        // Injetar data de início via JS
        WebElement startEl = driver.findElement(By.id("start"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", startEl, "2026-05-17T11:00");

        // Injetar data de fim via JS
        WebElement endEl = driver.findElement(By.id("end"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", endEl, "2026-05-17T11:30");

        // Submeter formulário via JS
        ((JavascriptExecutor) driver).executeScript("document.querySelector('form[action*=\"meetings\"]').submit();");

        // verificar se a reuniao ficou marcada
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("lolzinho"));
    }

    /**
     * Testa a criação de reuniões
     * com horas invalidas
     */
    @Test
    void proposeMeetingShowsErrorWhenEndTimeIsBeforeStartTime() {

        // Registar e autenticar user
        register("Miguel", "Miguelou04@gmail.com", "Benfica123");
        login("Miguel", "Benfica123");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // Preencher formulário com hora final invalida
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        driver.findElement(By.id("title")).sendKeys("lolzinho");

        // Injetar datas invertidas via JS
        WebElement startEl = driver.findElement(By.id("start"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", startEl, "2027-06-15T09:00");

        WebElement endEl = driver.findElement(By.id("end"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", endEl, "2026-06-15T08:00");

        // Submeter formulário via JS
        ((JavascriptExecutor) driver).executeScript("document.querySelector('form[action*=\"meetings\"]').submit();");

        // verificar se deu erro
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".error")));
        assertTrue(driver.getPageSource().contains("End time must be after start time"));
    }


    /**
     * Testa a aceitar contives
     * pendentes
     */
    @Test
    void pendingInviteAcceptsMeetingWhenUserConfirms() {
        // Registar users
        register("Miguel", "Miguel@gmail.com", "lisoba");
        register("coma", "coma@gmail.com", "lisboa");

        login("Miguel", "lisoba");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        driver.findElement(By.id("title")).sendKeys("fotbolada");

        // Injetar datas via JS
        WebElement startEl = driver.findElement(By.id("start"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", startEl, "2026-06-05T09:00");

        WebElement endEl = driver.findElement(By.id("end"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", endEl, "2026-06-05T09:30");

        driver.findElement(By.id("invitees")).sendKeys("coma");

        // Submeter formulário via JS
        ((JavascriptExecutor) driver).executeScript("document.querySelector('form[action*=\"meetings\"]').submit();");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // nova seccao
        driver.quit();

        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");

        driver = new FirefoxDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Verificar se existe o convite pendente
        login("coma", "lisboa");
        assertTrue(driver.getPageSource().contains("fotbolada"));
        assertTrue(driver.getPageSource().contains("pending"));

        // aceitar o convite
        WebElement acceptButton = driver.findElement(
                By.xpath("//input[@value='accept']/following-sibling::button | //input[@name='action'][@value='accept']/../button"));
        acceptButton.click();

        // verificar se ficou guardado
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("fotbolada"));
    }


    /**
     * Testa recusar convites pendentes
     */
    @Test
    void pendingInviteRemovesMeetingWhenUserDeclines() {

        register("Miguel", "Miguel@gmail.com", "lisoba");
        register("coma", "coma@gmail.com", "lisboa");

        login("Miguel", "lisoba");

        // criar reuniao e convidar particiepante
        wait.until(ExpectedConditions.urlContains("/calendar"));
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        driver.findElement(By.id("title")).sendKeys("Bora jola");

        // Injetar datas via JS
        WebElement startEl = driver.findElement(By.id("start"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", startEl, "2026-06-25T15:00");

        WebElement endEl = driver.findElement(By.id("end"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].type = 'text'; arguments[0].value = arguments[1];", endEl, "2026-06-25T15:30");

        driver.findElement(By.id("invitees")).sendKeys("coma");

        // Submeter formulário via JS
        ((JavascriptExecutor) driver).executeScript("document.querySelector('form[action*=\"meetings\"]').submit();");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // login e recusar a reuniao
        login("coma", "lisboa");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("Bora jola"));

        // recusa o convite
        WebElement declineButton = driver.findElement(
                By.xpath("//input[@name='action'][@value='decline']/../button"));
        declineButton.click();

        // Verificar que foi removida
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertFalse(driver.getPageSource().contains("Bora jola"));
    }





    /**
     * Testa a pesquisa de eventos atraves de um mock de enveto
     * para o teste nao falhar no futuro
     *
     */
    @Test
    void discoverEventCopiesEventToCalendarWhenSelected() {
        // mock do evento
        Mockito.when(mockEventProvider.isConfigured()).thenReturn(true);

        String titulo = "encontro de jogadores de pokemom";

        DiscoveredEvent fakeEvent = new DiscoveredEvent(
                "Ticketmaster",
                "ext-854",
                titulo,
                "Venha demonstrar os seus pockemons",
                Instant.parse("2026-08-20T10:00:00Z"),
                Instant.parse("2026-08-20T15:00:00Z"),
                "http://eventofalso.com",
                "Praça de espanha"
        );
        Mockito.when(mockEventProvider.search("encontro")).thenReturn(List.of(fakeEvent));

        // Registar e autenticar user
        register("Miguel", "Miguelou04@gmail.com", "Benfica123");
        login("Miguel", "Benfica123");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // procurar pelo evento
        driver.get(baseUrl() + "/discover?q=encontro");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        assertTrue(driver.getPageSource().contains(titulo));

        // copiar evento para calendario
        String a = "//form[.//input[@name='title' and @value='" + titulo + "']]";

        WebElement form = driver.findElement(By.xpath(a));
        ((JavascriptExecutor) driver).executeScript("arguments[0].submit();", form);

        // verificar se esta presente no calendario
        wait.until(ExpectedConditions.urlContains("/calendar"));

        String calendarPageSource = driver.getPageSource();
        assertTrue(calendarPageSource.contains(titulo));
    }
    /**
     * Testa  a  autenticação de um utilizador .
     * * Este fluxo verifica as seguintes ações sequenciais:
     *  Bloqueio de acesso a rotas protegidas redirecionamento para login.
     * Registo de uma nova conta de utilizador com sucesso.
     *  Login com as credenciais recém-criadas.
     *  Validação da sessão ativa (presença do nome do utilizador no calendário).
     * Término da sessão (logout) e redirecionamento correto.
     */

    @Test
    void userAuthenticationJourney() {
        // tentar acedar ao calendario
        driver.get(baseUrl() + "/calendar");


        // verificar se foi redirect para o login
        wait.until(ExpectedConditions.urlContains("/login"));
        // Registar utilizador
        register("Miguel", "Miguelou04@gmail.com", "Benfica123");

        // Verificar redirecionamento para login
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getPageSource().contains("Account created"));

        // Registar e autenticar user
        login("Miguel", "Benfica123");

        // verifica se foi redireciado
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("Miguel"));
        assertTrue(driver.getPageSource().contains("Your calendar"));

        // Efetuar logout
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelector('form[action*=\"logout\"]').submit();"
        );

        // vereficar se voltou para a pagina do login
        wait.until(ExpectedConditions.urlContains("/login"));
    }
}