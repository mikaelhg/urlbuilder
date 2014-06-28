package gumi.builders;

import static org.testng.Assert.*;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import java.net.*;
import java.util.List;

public class UrlBuilderStepDefinitions {

    private UrlBuilder builder;

    private List<String> urls;

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

    @Given("^I have these URLs:$")
    public void i_have_these_urls(final List<String> urls) {
        this.urls = urls;
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

    @Then("^the urls stay the same after a roundtrip conversion$")
    public void the_urls_stay_the_same_after_a_roundtrip_conversion() {
        for (final String url : urls) {
            assertEquals(url, UrlBuilder.fromString(url).toString());
        }
    }

    @Then("^as a string it should be (.*)$")
    public void as_a_string_it_should_be_x(final String result) {
        assertEquals(builder.toString(), result);
    }

    @Then("^as a (.*) encoded string it should be (.*)$")
    public void as_a_y_encoded_string_it_should_be_x(final String encoding, final String result) {
        assertEquals(builder.encodeAs(encoding).toString(), result);
    }

    @Then("^the user info should be (.*)$")
    public void the_user_info_should_be_u(final String userInfo) {
        assertEquals(builder.userInfo, userInfo);
    }

    @Then("^the host name should be (.*)$")
    public void the_host_name_should_be_h(final String h) {
        assertEquals(h, builder.hostName);
    }

    @Then("^the path should be (.*)$")
    public void the_path_should_be_f(final String path) {
        assertEquals(builder.path, path);
    }

    @Then("^the parameter (.*) should be (.*)$")
    public void the_parameter_key_should_be_value(final String key, final String value) {
        assertEquals(builder.queryParameters.get(key).get(0), value);
    }

    @Then("^the fragment should be (.*)$")
    public void the_fragment_should_be_f(final String fragment) {
        assertEquals(builder.fragment, fragment);
    }

    @Then("^it should be an empty string$")
    public void it_should_be_an_empty_string() {
        assertEquals(builder.toString(), "");
    }

    @Then("^the unicode path should be a smiley$")
    public void the_unicode_path_should_be_a_smiley() {
        assertEquals(builder.path, "/\u263A");
    }

    @Then("^the unicode path should be a playing card ace of spades$")
    public void the_unicode_path_should_be_a_playing_card_ace_of_spades() {
        assertEquals(builder.path, "/\uD83C\uDCA1");
    }
}
