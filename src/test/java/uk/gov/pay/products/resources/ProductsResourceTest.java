package uk.gov.pay.products.resources;

import com.google.common.collect.ImmutableMap;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;
import uk.gov.pay.products.fixtures.ProductEntityFixture;
import uk.gov.pay.products.model.Product;
import uk.gov.pay.products.persistence.entity.CatalogueEntity;
import uk.gov.pay.products.util.ProductStatus;

import javax.ws.rs.HttpMethod;
import java.io.Serializable;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.products.fixtures.CatalogueEntityFixture.aCatalogueEntity;
import static uk.gov.pay.products.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.products.util.RandomIdGenerator.randomUuid;

public class ProductsResourceTest extends IntegrationTest {

    private static final String EXTERNAL_SERVICE_ID = "external_service_id";
    private static final String PAY_API_TOKEN = "pay_api_token";
    private static final String NAME = "name";
    private static final String PRICE = "price";
    private static final String EXTERNAL_ID = "external_id";
    private static final String DESCRIPTION = "description";
    private static final String RETURN_URL = "return_url";

    @Test
    public void shouldFail_whenSavingAProduct_withInCorrectAuthToken() throws Exception {
        ImmutableMap<String, ? extends Serializable> payload = ImmutableMap.of(
                "external_service_id", randomUuid(),
                "pay_api_token", randomUuid(),
                "name", "a-name",
                "price", 1234);

        givenSetup()
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer invalid-api-key")
                .accept(APPLICATION_JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/products")
                .then()
                .statusCode(401);

    }

    @Test
    public void shouldSuccess_whenSavingAValidProduct_withMinimumMandatoryFields() throws Exception {

        String externalServiceId = randomUuid();
        String payApiToken = randomUuid();
        String name = "Flashy new GOV Service";
        Long price = 1050L;

        ImmutableMap<String, ? extends Serializable> payload = ImmutableMap.of(
                EXTERNAL_SERVICE_ID, externalServiceId,
                PAY_API_TOKEN, payApiToken,
                NAME, name,
                PRICE, price);

        ValidatableResponse response = givenAuthenticatedSetup()
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/products")
                .then()
                .statusCode(201);

        response
                .body(NAME, is("Flashy new GOV Service"))
                .body(EXTERNAL_SERVICE_ID, is(externalServiceId))
                .body(PRICE, is(1050))
                .body(EXTERNAL_ID, matchesPattern("^[0-9a-z]{32}$"));

        String externalId = response.extract().path(EXTERNAL_ID);

        System.out.println(response.extract().path("_links").toString());

        String productsUrl = "http://localhost:8080/v1/api/products/";
        String productsUIUrl = "http://localhost:3000/pay/";
        response
                .body("_links", hasSize(2))
                .body("_links[0].href", matchesPattern(productsUrl + externalId))
                .body("_links[0].method", is(HttpMethod.GET))
                .body("_links[0].rel", is("self"))
                .body("_links[1].href", matchesPattern(productsUIUrl + externalId))
                .body("_links[1].method", is(HttpMethod.POST))
                .body("_links[1].rel", is("pay"));

    }

    @Test
    public void shouldSuccess_whenSavingAValidProduct_withAllFields() throws Exception {

        String externalServiceId = randomUuid();
        String payApiToken = randomUuid();
        String name = "Flashy new GOV Service";
        Long price = 1050L;
        String description = "Some test description";

        String returnUrl = "http://some.valid.url";
        ImmutableMap<Object, Object> payload = ImmutableMap.builder()
                .put(EXTERNAL_SERVICE_ID, externalServiceId)
                .put(PAY_API_TOKEN, payApiToken)
                .put(NAME, name)
                .put(PRICE, price)
                .put(DESCRIPTION, description)
                .put(RETURN_URL, returnUrl)
                .build();

        ValidatableResponse response = givenAuthenticatedSetup()
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(mapper.writeValueAsString(payload))
                .post("/v1/api/products")
                .then()
                .statusCode(201);

        response
                .body(NAME, is(name))
                .body(EXTERNAL_SERVICE_ID, is(externalServiceId))
                .body(PRICE, is(1050))
                .body(EXTERNAL_ID, matchesPattern("^[0-9a-z]{32}$"))
                .body(DESCRIPTION, is(description))
                .body(RETURN_URL, is(returnUrl));

        String externalId = response.extract().path(EXTERNAL_ID);

        System.out.println(response.extract().path("_links").toString());

        String productsUrl = "http://localhost:8080/v1/api/products/";
        String productsUIUrl = "http://localhost:3000/pay/";
        response
                .body("_links", hasSize(2))
                .body("_links[0].href", matchesPattern(productsUrl + externalId))
                .body("_links[0].method", is(HttpMethod.GET))
                .body("_links[0].rel", is("self"))
                .body("_links[1].href", matchesPattern(productsUIUrl + externalId))
                .body("_links[1].method", is(HttpMethod.POST))
                .body("_links[1].rel", is("pay"));

    }

    @Test
    public void shouldError_whenSavingAProduct_withMandatoryFieldsMissing() throws Exception {
        givenAuthenticatedSetup()
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(mapper.writeValueAsString("{}"))
                .post("/v1/api/products")
                .then()
                .statusCode(400);
    }

    @Test
    public void givenAnExistingExternalProductId_shouldFindAndReturnProduct() throws Exception {
        String externalId = randomUuid();
        CatalogueEntity aCatalogueEntity = aCatalogueEntity().build();

        Product product = ProductEntityFixture.aProductEntity()
                .withExternalId(externalId)
                .withCatalogue(aCatalogueEntity)
                .build()
                .toProduct();

        int catalogueId = randomInt();
        databaseHelper.addProductAndCatalogue(product, catalogueId);

        ValidatableResponse response = givenAuthenticatedSetup()
                .when()
                .accept(APPLICATION_JSON)
                .get(format("/v1/api/products/%s", externalId))
                .then()
                .statusCode(200);

        Integer intPrice = response.extract().path(PRICE);
        Long price = new Long(intPrice);
        assertThat(price, equalTo(product.getPrice()));

        response
                .body(NAME, is(product.getName()))
                .body(EXTERNAL_ID, matchesPattern("^[0-9a-z]{32}$"))
                .body(DESCRIPTION, is(product.getDescription()))
                .body(RETURN_URL, is(product.getReturnUrl()));

        String productsUrl = "http://localhost:8080/v1/api/products/";
        String productsUIUrl = "http://localhost:3000/pay/";
        response
                .body("_links", hasSize(2))
                .body("_links[0].href", matchesPattern(productsUrl + externalId))
                .body("_links[0].method", is(HttpMethod.GET))
                .body("_links[0].rel", is("self"))
                .body("_links[1].href", matchesPattern(productsUIUrl + externalId))
                .body("_links[1].method", is(HttpMethod.POST))
                .body("_links[1].rel", is("pay"));
    }

    @Test
    public void givenANonExistingExternalProductId_shouldReturn404() throws Exception {
        givenAuthenticatedSetup()
                .accept(APPLICATION_JSON)
                .get(format("/v1/api/products/%s", randomUuid()))
                .then()
                .statusCode(404);
    }

    @Test
    public void givenANotAuthenticatedRequest_shouldReturn401() throws Exception {
        givenSetup()
                .accept(APPLICATION_JSON)
                .get(format("/v1/api/products/%s", randomUuid()))
                .then()
                .statusCode(401);

        givenSetup()
                .accept(APPLICATION_JSON)
                .patch(format("/v1/api/products/%s/disable", randomUuid()))
                .then()
                .statusCode(401);

        givenSetup()
                .when()
                .accept(APPLICATION_JSON)
                .get(format("/v1/api/products?externalServiceId=%s", randomUuid()))
                .then()
                .statusCode(401);
    }

    @Test
    public void givenAValidExternalProductId_shouldDisableTheProduct() throws Exception{
        String externalId = randomUuid();
        CatalogueEntity aCatalogueEntity = aCatalogueEntity().build();

        Product product = ProductEntityFixture.aProductEntity()
                .withExternalId(externalId)
                .withCatalogue(aCatalogueEntity)
                .build()
                .toProduct();

        int catalogueId = randomInt();
        databaseHelper.addProductAndCatalogue(product, catalogueId);

        givenAuthenticatedSetup()
                .when()
                .accept(APPLICATION_JSON)
                .patch(format("/v1/api/products/%s/disable", externalId))
                .then()
                .statusCode(204);

    }

    @Test
    public void givenANonExistingExternalProductId_whenDisableAProduct_shouldReturn404() throws Exception{
        givenAuthenticatedSetup()
                .when()
                .accept(APPLICATION_JSON)
                .patch(format("/v1/api/products/%s/disable", randomUuid()))
                .then()
                .statusCode(404);
    }

    @Test
    public void givenAnExistingExternalServiceId_shouldFindAndReturnProducts() throws Exception {
        String externalServiceId = randomUuid();
        CatalogueEntity aCatalogueEntity = aCatalogueEntity()
                .withExternalServiceId(externalServiceId)
                .build();

        Product product = ProductEntityFixture.aProductEntity()
                .withCatalogue(aCatalogueEntity)
                .build()
                .toProduct();

        int catalogueId = randomInt();
        databaseHelper.addProductAndCatalogue(product, catalogueId);

        Product product_2 = ProductEntityFixture.aProductEntity()
                .withCatalogue(aCatalogueEntity)
                .build()
                .toProduct();

        databaseHelper.addProduct(product_2, catalogueId);

        ValidatableResponse response = givenAuthenticatedSetup()
                .when()
                .accept(APPLICATION_JSON)
                .get(format("/v1/api/products?externalServiceId=%s", externalServiceId))
                .then()
                .statusCode(200);

        response.body("", hasSize(2))
                .body("[0].external_service_id", matchesPattern(externalServiceId))
                .body("[0]._links", hasSize(2))
                .body("[0].description", matchesPattern(product.getDescription()))
                .body("[0].price", is(product.getPrice().intValue()))
                .body("[0].name", matchesPattern(product.getName()))
                .body("[1].name", matchesPattern(product_2.getName()));
    }

    @Test
    public void givenNonExistingExternalServiceId_shouldReturn404() throws Exception {
        givenAuthenticatedSetup()
                .when()
                .accept(APPLICATION_JSON)
                .get(format("/v1/api/products?externalServiceId=%s", randomUuid()))
                .then()
                .statusCode(404);
    }

    @Test
    public void givenAnExistingExternalServiceId_whenProductIsAlreadyDisabled_thenShouldReturn404() throws Exception {
        String externalServiceId = randomUuid();
        CatalogueEntity aCatalogueEntity = aCatalogueEntity()
                .withExternalServiceId(externalServiceId)
                .build();

        Product product = ProductEntityFixture.aProductEntity()
                .withCatalogue(aCatalogueEntity)
                .withStatus(ProductStatus.INACTIVE)
                .build()
                .toProduct();

        int catalogueId = randomInt();
        databaseHelper.addProductAndCatalogue(product, catalogueId);

        givenAuthenticatedSetup()
                .when()
                .accept(APPLICATION_JSON)
                .get(format("/v1/api/products?externalServiceId=%s", externalServiceId))
                .then()
                .statusCode(404);
    }
}
