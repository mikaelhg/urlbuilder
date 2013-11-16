Feature: UrlBuilder url creation

  Scenario: Empty URL builder
    Given I create an empty builder
    Then it should be an empty string

  Scenario: Basic Google URL string
    Given I create a builder from the string http://www.google.com/?q=test
    When I set the schema to https
    Then as a string it should be https://www.google.com/?q=test

  Scenario: From URL with urlencoded space character
    Given I create a builder from the URL http://www.example.com/a%20b/
    Then as a string it should be http://www.example.com/a+b/

  Scenario: Equals character in query parameter value
    Given I create a builder from the string /?a=1=2
    Then the parameter a should be 1=2
