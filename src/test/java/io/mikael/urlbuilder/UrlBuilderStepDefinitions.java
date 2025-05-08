package io.mikael.urlbuilder;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
            throws URISyntaxException
    {
        final URI uri = new URI(uriString);
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
        assertEquals(result, builder.toString());
    }

    @Then("^as a URI it should be (.*)$")
    public void as_a_uri_string_it_should_be_x(final String result) {
        assertEquals(result, builder.toUri().toString());
    }

    @Then("^as a (.*) encoded string it should be (.*)$")
    public void as_a_y_encoded_string_it_should_be_x(final String encoding, final String result) {
        assertEquals(result, builder.encodeAs(encoding).toString());
    }

    @Then("^the user info should be (.*)$")
    public void the_user_info_should_be_u(final String userInfo) {
        assertEquals(userInfo, builder.userInfo);
    }

    @Then("^the host name should be (.*)$")
    public void the_host_name_should_be_h(final String h) {
        assertEquals(h, builder.hostName);
    }

    @Then("^the path should be (.*)$")
    public void the_path_should_be_f(final String path) {
        assertEquals(path, builder.path);
    }

    @Then("^the parameter (.*) should be (.*)$")
    public void the_parameter_key_should_be_value(final String key, final String value) {
        assertEquals(value, builder.queryParameters.get(key).get(0));
    }

    @Then("^the fragment should be (.*)$")
    public void the_fragment_should_be_f(final String fragment) {
        assertEquals(fragment, builder.fragment);
    }

    @Then("^it should be an empty string$")
    public void it_should_be_an_empty_string() {
        assertEquals("", builder.toString());
    }

    @Then("^the unicode path should be a smiley$")
    public void the_unicode_path_should_be_a_smiley() {
        assertEquals("/\u263A", builder.path);
    }

    @Then("^the unicode path should be a playing card ace of spades$")
    public void the_unicode_path_should_be_a_playing_card_ace_of_spades() {
        assertEquals("/\uD83C\uDCA1", builder.path);
    }

    @Then("^the port should be (\\d+)$")
    public void thePortShouldBe(Integer port) throws Throwable {
        assertEquals(port, builder.port, "Port doesn't match");
    }

}
