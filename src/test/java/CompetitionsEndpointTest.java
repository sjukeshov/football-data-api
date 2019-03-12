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
 * TS001
 */

class CompetitionsEndpointTest {
    //I had to create new apiKey for this class to not get 429 http error
    private String apiKey = "bb85687d58184c168039f89b12562f05";
    private int testId = 2000;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://api.football-data.org/v2/competitions";
    }

    @Test
    void checkAllCompetitionsAnonymously() {
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
                body("count", greaterThanOrEqualTo(1));
    }

    @Test
    void checkRestrictedAPIPathWithoutKey() {
        given().
                accept("application/json").
                pathParam("id", testId).
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
                pathParam("id", testId).
        when().
                get("/{id}").
        then().
                statusCode(400).
                body("message", equalTo("Your API token is invalid."));
    }

    @Test
    void checkRestrictedAPIPathWithKey() {
        given().
                header("X-Auth-Token", apiKey).
                accept("application/json").
                pathParam("id", testId).
        when().
                get("/{id}").
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("id", equalTo(testId)).
                body("name", equalTo("FIFA World Cup"));
    }

    @DisplayName("Parametrized Test to check several competitions that can be accessed by free account")
    @ParameterizedTest(name = "Checking competition id #{index}")
    @MethodSource("checkCompetitionsWithAccessFromFreeAccountParameters")
    void checkCompetitionsWithAccessFromFreeAccount(int competitionIds, String name, String country) {
        given().
                header("X-Auth-Token", apiKey).
                accept("application/json").
                pathParam("id", competitionIds).
        when().
                get("/{id}").
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("id", equalTo(competitionIds)).
                body("name", equalTo(name)).
                body("area.name", equalTo(country));
    }

    static Stream<Arguments> checkCompetitionsWithAccessFromFreeAccountParameters() {
        return Stream.of(
                Arguments.of("2014", "Primera Division", "Spain"),
                Arguments.of("2021", "Premier League", "England"),
                Arguments.of("2019", "Serie A", "Italy")
        );
    }

    @DisplayName("Parametrized Test to check several competitions that can not be accessed by free account")
    @ParameterizedTest(name = "Checking competition id #{index}")
    @ValueSource(ints = {2124, 2056, 2050})
    void checkSeveralCompetitionsWithoutAccessFromFreeAccount(int competitionIds) {
        given().
                header("X-Auth-Token", apiKey).
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
    void checkCompetitionsFilteredByArea() {
        int areaId = 2224;
        given().
                header("X-Auth-Token", apiKey).
                accept("application/json").
                queryParam("areas", areaId).
        when().
                get().
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("count", equalTo(4)).
                body("competitions", hasSize(4)).
                body("filters.areas", contains(areaId));
    }

    @Test
    void checkCompetitionsFilteredByPlan() {
        String planId = "TIER_ONE";
        given().
                header("X-Auth-Token", apiKey).
                accept("application/json").
                queryParam("plan", planId).
        when().
                get().
        then().
                statusCode(200).
                contentType(notNullValue()).
                header("X-API-Version", equalTo("v2")).
                header("X-Authenticated-Client", not("anonymous")).
                body("count", equalTo(12)).
                body("competitions", hasSize(12)).
                body("filters.plan", equalTo(planId));
    }
}
