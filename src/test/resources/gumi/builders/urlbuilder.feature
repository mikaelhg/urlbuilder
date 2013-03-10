Feature: UrlBuilder url creation

  Scenario: Empty URL
    Given I create an empty builder
    Then it should be an empty string

  Scenario: Basic Google URL
    Given I create a builder from the string http://www.google.com/?q=test
    When I set the schema to https
    Then as a string it should be https://www.google.com/?q=test

