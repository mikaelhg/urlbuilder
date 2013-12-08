Feature: Character sets

  Scenario: ISO-8859-1 encoded Ä
    Given I create a builder from the ISO-8859-1 encoded string /%C4
    Then as a ISO-8859-1 encoded string it should be /%C4

  Scenario: ISO-8859-1 encoded 0xFF
    Given I create a builder from the ISO-8859-1 encoded string /%FF
    Then as a ISO-8859-1 encoded string it should be /%FF
