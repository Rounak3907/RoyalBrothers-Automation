Feature: Royal Brothers Search Automation

  Scenario: Search & Validate Bikes with dynamic City and Location filter.
    Given I opened the Royal Brothers website.
    When I selected City "Bangalore".
    And I entered Booking time "2025-11-25 10:00" to "2025-11-26 18:00".
    And I clicked on Search.
    Then the selected date range is revalidated.
    When I applied location filter "Yeshwanthpur (BMTC Bus Station)".
    Then all shown bikes are validated to belong to "Yeshwanthpur (BMTC Bus Station)".
    And all Bike details are printed.
