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
    Then as a string it should be http://www.example.com/a%20b/

  Scenario: Percent Encode a plus in the path
    Given I create a builder from the URL http://www.example.com/a+b%2b/
    Then the path should be /a+b+/
      And as a string it should be http://www.example.com/a%2Bb%2B/

  Scenario: From URL with allowed special characters in the path
    Given I create a builder from the URL http://example.com/a=&b/
    Then as a string it should be http://example.com/a=&b/

  Scenario: From URL with space character in the query
    Given I create a builder from the URL http://example.com/?some+key=some%20value
    Then as a string it should be http://example.com/?some%20key=some%20value

  Scenario: From URL with special characters in the query
    Given I create a builder from the URL http://example.com/?some+%2b%20key=some%20%3d?value
    Then the parameter some + key should be some =?value
      And as a string it should be http://example.com/?some%20%2B%20key=some%20%3D%3Fvalue

  Scenario: From URL with special characters in the fragment
    Given I create a builder from the URL http://example.com/#=?%23
    Then the fragment should be =?#
      And as a string it should be http://example.com/#=?%23

  Scenario: Equals character in query parameter value
    Given I create a builder from the string /?a=1=2
    Then the parameter a should be 1=2

  Scenario: From URL with user info
    Given I create a builder from the URL https://bob:passwd@example.com/secure
    Then as a string it should be https://bob:passwd@example.com/secure
      And the user info should be bob:passwd
      And the host name should be example.com

  Scenario: From URL with encoded characters in the user info
    Given I create a builder from the string https://bobby%20droptables:passwd@example.com/secure
    Then as a string it should be https://bobby%20droptables:passwd@example.com/secure
      And the user info should be bobby droptables:passwd

  Scenario: From URI with a null path and query
    Given I create a builder from a URI mailto:bob@example.com with a null path and query
    Then as a string it should be mailto:

  Scenario: Round trip conversion
    Given I have these URLs:
      | https://                     |
      | https://www                  |
      | https://www:1234             |
      | https://www:1234/            |
      | https://www:1234/foo         |
      | https://www:1234/foo/bar     |
      | https://www:1234/foo/bar/    |
      | https://www:1234/foo/bar//   |
      | https://www:1234/foo//bar//  |
      | //google.com/logo.png        |
      | g:h                          |
      | http://a/b/c/d;p?y           |
      | http://a/b/c/d;p?q#s         |
      | http://a/b/c/g?y#s           |
      | http://a/b/c/g;x?y#s         |
      | http://a/b/c/g#s/../x        |
      | http:g                       |
    Then the urls stay the same after a roundtrip conversion
