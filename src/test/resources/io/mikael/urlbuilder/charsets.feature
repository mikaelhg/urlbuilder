Feature: Character sets

  Scenario: ISO-8859-1 encoded Ä
    Given I create a builder from the ISO-8859-1 encoded string /%C4
     Then as a ISO-8859-1 encoded string it should be /%C4

  Scenario: ISO-8859-1 encoded 0xFF
    Given I create a builder from the ISO-8859-1 encoded string /%FF
     Then as a ISO-8859-1 encoded string it should be /%FF

  Scenario: encode a 16-bit Unicode char
    Given I create a builder from the UTF-16 encoded string http://example.com/%26%3A
     Then the unicode path should be a smiley
      And as a UTF-8 encoded string it should be http://example.com/%E2%98%BA

  Scenario: encode a surrogate pair
    Given I create a builder from the UTF-32 encoded string http://example.com/%00%01%F0%A1
     Then the unicode path should be a playing card ace of spades
      And as a UTF-8 encoded string it should be http://example.com/%F0%9F%82%A1
