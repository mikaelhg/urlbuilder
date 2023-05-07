package io.mikael.urlbuilder;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@SelectClasspathResource("io/mikael")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.mikael")
public class RunCukesTest {
}
