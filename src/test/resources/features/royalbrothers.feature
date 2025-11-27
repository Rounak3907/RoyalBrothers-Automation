Feature: Royal Brothers Search Automation

  Scenario: Search & Validate Bikes with dynamic City and Location filter.
    Given I opened the Royal Brothers website.
    When I selected City "Bangalore".
    And I entered Booking time "2025-12-02 10:00" to "2025-12-03 18:00".
    And I clicked on Search.
    Then the selected date range is revalidated.
    When I applied location filter "Yeshwanthpur (BMTC Bus Station)".
    Then all shown bikes are validated to belong to "Yeshwanthpur (BMTC Bus Station)".
    And all Bike details are printed.

  Scenario: User selects invalid date range
    Given I opened the Royal Brothers website.
    When I selected City "Bangalore".
    And I entered Booking time "2025-12-04 10:00" to "2025-12-02 18:00".
    Then an error message should be shown for invalid date selection

  Scenario: User searches for an unavailable location
    Given I opened the Royal Brothers website.
    When I selected City "Bangalore".
    And I entered Booking time "2025-12-02 10:00" to "2025-12-03 18:00".
    And I clicked on Search.
    Then the selected date range is revalidated.
    When I applied location filter "ABC Road".
    Then no location results should be displayed

  Scenario: User tries searching without selecting a city
    Given I opened the Royal Brothers website.
    And I entered Booking time "2025-12-02 10:00" to "2025-12-03 18:00".
    Then an Timeout error message should appear.

