package br.com.fiap.lambda;

import br.com.fiap.lambda.dto.FeedbackDTO;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class LambdaHandlerTest {
    @Test
    void testSimpleLambdaSuccess() {
        // you test your lambdas by invoking on http://localhost:8081
        // this works in dev mode too

        FeedbackDTO feedbackDTO = new FeedbackDTO("Teste de feedback", 8);
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(feedbackDTO)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body(containsString("descricao"));
    }

}
