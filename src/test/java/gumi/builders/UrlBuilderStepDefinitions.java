package gumi.builders;

import static org.junit.Assert.*;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import java.net.*;

public class UrlBuilderStepDefinitions {

    private UrlBuilder builder;

    @Given("^I create an empty builder$")
    public void i_create_an_empty_builder() {
        builder = UrlBuilder.empty();
    }

    @Given("^I create a builder from the string (.*)$")
    public void i_create_a_builder_from_the_string_x(final String urlString) {
        builder = UrlBuilder.fromString(urlString);
    }

    @Given("^I create a builder from the URL (.*)$")
    public void i_create_a_builder_from_the_url_x(final String urlString) throws MalformedURLException {
        builder = UrlBuilder.fromUrl(new URL(urlString));
    }

    @Given("^I create a builder from a URI (.*) with a null path and query$")
    public void i_create_a_builder_from_a_uri_with_a_null_path_and_query(final String uriString)
            throws URISyntaxException {
        URI uri = new URI(uriString);
        assertNull(uri.getPath());
        assertNull(uri.getQuery());
        builder = UrlBuilder.fromUri(uri);
    }

    @Given("^I create a builder from the (.*) encoded string (.*)$")
    public void i_create_a_builder_from_the_x_encoded_string_y(final String encoding, final String urlString) {
        builder = UrlBuilder.fromString(urlString, encoding);
    }

    @When("^I set the schema to (.*)$")
    public void i_set_the_schema_to_x(final String value) {
        builder = builder.withScheme(value);
    }

    @When("^I set the host to (.*)$")
    public void i_set_the_host_to_x(final String value) {
        builder = builder.withHost(value);
    }

    @When("^I set the path to (.*)$")
    public void i_set_the_path_to_x(final String value) {
        builder = builder.withPath(value);
    }

    @Then("^as a string it should be (.*)$")
    public void as_a_string_it_should_be_x(final String result) {
        assertEquals(result, builder.toString());
    }

    @Then("^as a (.*) encoded string it should be (.*)$")
    public void as_a_y_encoded_string_it_should_be_x(final String encoding, final String result) {
        assertEquals(result, builder.encodeAs(encoding).toString());
    }

    @Then("^the path should be (.*)$")
    public void the_path_should_be_f(final String p) {
        assertEquals(p, builder.path);
    }

    @Then("^the parameter (.*) should be (.*)$")
    public void the_parameter_key_should_be_value(final String key, final String value) {
        assertEquals(value, builder.queryParameters.get(key).get(0));
    }

    @Then("^the fragment should be (.*)$")
    public void the_fragment_should_be_f(final String f) {
        assertEquals(f, builder.fragment);
    }

    @Then("^it should be an empty string$")
    public void it_should_be_an_empty_string() {
        assertEquals("", builder.toString());
    }

}
