import com.jayway.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * https://docs.google.com/spreadsheets/d/1O98qGYwbqhGLZoUuARdoLkbdouyQekho_NmzuKQIjOc/edit?pli=1#gid=157124725
 * TS002
 */
class MatchEndpointTest {
    //I had to create new API_KEY for this class to not get 429 http error
    private static final String API_KEY = "07d375d76f44468ea0a8bd61276bb476";
    private static final int TEST_ID = 235686;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://api.football-data.org/v2/matches";
    }

    @Test
    void checkAllMatchesAnonymously() {
        given().
                accept("application/json").
        when().
                get("/").
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", equalTo("anonymous")).
                header("X-Requests-Available", greaterThanOrEqualTo("1")).
                header("X-RequestCounter-Reset", greaterThanOrEqualTo("1")).
                body("count", greaterThanOrEqualTo(0));
    }

    @Test
    void checkRestrictedAPIPathWithoutKey() {
        given().
                accept("application/json").
                pathParam("id", TEST_ID).
        when().
                get("/{id}").
        then().
                statusCode(403).
                body("message", equalTo("The resource you are looking for is restricted. Please pass a valid API token and check your subscription for permission."));
    }

    @Test
    void checkRestrictedAPIPathWithIncorrectKey() {
        given().
                header("X-Auth-Token", "incorrectApiKey").
                accept("application/json").
                pathParam("id", TEST_ID).
        when().
                get("/{id}").
        then().
                statusCode(400).
                body("message", equalTo("Your API token is invalid."));
    }

    @Test
    void checkRestrictedAPIPathWithKey() {
        given().
                header("X-Auth-Token", API_KEY).
                accept("application/json").
                pathParam("id", TEST_ID).
        when().
                get("/{id}").
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("match.id", equalTo(TEST_ID)).
                body("match.competition.name", equalTo("Bundesliga"));
    }

    @DisplayName("Parametrized Test to check several matches that can be accessed by free account")
    @ParameterizedTest(name = "Checking match id #{index}")
    @MethodSource("checkMatchesWithAccessFromFreeAccountParameters")
    void checkMatchesWithAccessFromFreeAccount(int matchId, String name, String homeTeamName, String awayTeamName) {
        given().
                header("X-Auth-Token", API_KEY).
                accept("application/json").
                pathParam("id", matchId).
        when().
                get("/{id}").
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("match.id", equalTo(matchId)).
                body("match.competition.name", equalTo(name)).
                body("match.homeTeam.name", equalTo(homeTeamName)).
                body("match.awayTeam.name", equalTo(awayTeamName));
    }

    static Stream<Arguments> checkMatchesWithAccessFromFreeAccountParameters() {
        return Stream.of(
                Arguments.of("235686", "Bundesliga", "FC Bayern München", "TSG 1899 Hoffenheim"),
                Arguments.of("235454", "Eredivisie", "FC Emmen", "FC Utrecht"),
                Arguments.of("235100", "Championship", "Swansea City AFC", "Middlesbrough FC")
        );
    }

    @DisplayName("Parametrized Test to check several competitions that can not be accessed by free account")
    @ParameterizedTest(name = "Checking competition id #{index}")
    @ValueSource(ints = {110185, 110186, 110187})
    void checkMatchesWithoutAccessFromFreeAccount(int competitionIds) {
        given().
                header("X-Auth-Token", API_KEY).
                accept("application/json").
                pathParam("id", competitionIds).
        when().
                get("/{id}").
        then().
                statusCode(403).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("message", equalTo("The resource you are looking for is restricted. Please pass a valid API token and check your subscription for permission."));
    }

    @Test
    void checkMatchesFilteredByDates() {
        String dateFrom = "2019-02-10";
        String dateTo = "2019-02-11";
        given().
                header("X-Auth-Token", API_KEY).
                accept("application/json").
                queryParam("dateFrom", dateFrom).
                queryParam("dateTo", dateTo).
        when().
                get().
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("filters.dateFrom", equalTo(dateFrom)).
                body("filters.dateTo", equalTo(dateTo)).
                body("count", equalTo(32)).
                body("matches", hasSize(32));
    }

    @Test
    void checkMatchesFilteredByDatesAndStatus() {
        String dateFrom = "2019-02-11";
        String dateTo = "2019-02-12";
        String status = "FINISHED";
        given().
                header("X-Auth-Token", API_KEY).
                accept("application/json").
                queryParam("dateFrom", dateFrom).
                queryParam("dateTo", dateTo).
                queryParam("status", status).
        when().
                get().
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("filters.dateFrom", equalTo(dateFrom)).
                body("filters.dateTo", equalTo(dateTo)).
                body("filters.status", contains(status)).
                body("count", equalTo(10)).
                body("matches", hasSize(10));
    }

    @Test
    void checkMatchesFilteredByCompetitions() {
        String dateFrom = "2019-02-10";
        String dateTo = "2019-02-11";
        int competitionId = 2014;
        given().
                header("X-Auth-Token", API_KEY).
                accept("application/json").
                queryParam("dateFrom", dateFrom).
                queryParam("dateTo", dateTo).
                queryParam("competitions", competitionId).
        when().
                get().
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("filters.dateFrom", equalTo(dateFrom)).
                body("filters.dateTo", equalTo(dateTo)).
                body("filters.competitions", contains(competitionId)).
                body("count", equalTo(5)).
                body("matches", hasSize(5));
    }
}
