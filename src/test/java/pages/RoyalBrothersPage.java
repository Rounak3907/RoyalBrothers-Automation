package pages;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import static org.assertj.core.api.Assertions.assertThat;

public class RoyalBrothersPage {
    Page page;
    private final List<String> bikeAvailabilityResults = new ArrayList<>();
    private int totalCards = 0;
    private int matched = 0;
    private int notMatched = 0;
    private int soldOut = 0;
    public RoyalBrothersPage(Page page){
        this.page = page;
    }
    public void navigate() {
        page.navigate("https://www.royalbrothers.com/");
    }
    public void selectCity(String city) {
        // Waiting for the modal to be visible
        page.waitForSelector("#modal-city",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        // Finding all matching <a> elements containing the city name
        Locator allMatchingCities = page.locator("#modal-content-scroll a.city-box");
        int count = allMatchingCities.count();
        for (int i = 0; i < count; i++) {
            Locator item = allMatchingCities.nth(i);
            String text = item.innerText().trim();
            // Matching exact city
            if (text.equalsIgnoreCase(city)) {
                item.click();
                return; // success
            }
        }
        throw new RuntimeException("City '" + city + "' not found in modal!");
    }

    public void enterBookingTime(String pickup, String drop) {
        selectDate("#pickup-date-other", pickup);
        selectTime("#pickup-time-other",pickup);
        selectDate("#dropoff-date-other",drop);
        selectTime("#dropoff-time-other",drop);
    }

    private void selectDate(String dateFieldSelector, String dateValue) {
        LocalDate date = LocalDate.parse(dateValue.split(" ")[0]);
        String day = String.valueOf(date.getDayOfMonth());
        int targetMonth = date.getMonthValue();
        int targetYear = date.getYear();
        String rootSelector = "#" + dateFieldSelector.replace("#", "") + "_root";
        Locator pickerRoot = page.locator(rootSelector);
        if (!pickerRoot.locator(".picker--opened").isVisible()) {
            page.click(dateFieldSelector);
            page.waitForTimeout(300);
        }
        Locator headerMonth = pickerRoot.locator(".picker__month");
        Locator headerYear  = pickerRoot.locator(".picker__year");
        headerMonth.waitFor();
        Locator nextBtn = pickerRoot.locator(".picker__nav--next");
        Locator prevBtn = pickerRoot.locator(".picker__nav--prev");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        // --- Month Loop ---
        while (true) {
            int currentMonth = Month.from(fmt.parse(headerMonth.innerText().trim())).getValue();
            int currentYear  = Integer.parseInt(headerYear.innerText().trim());

            if (currentMonth == targetMonth && currentYear == targetYear) break;

            if (currentYear < targetYear || (currentYear == targetYear && currentMonth < targetMonth))
                nextBtn.click();
            else
                prevBtn.click();

            page.waitForTimeout(200);
        }
        //First checking whether the date cell exists ---
        Locator possibleCells = pickerRoot.locator(
                ".picker__day:not(.picker__day--outfocus)"
        ).filter(new Locator.FilterOptions().setHasText(day));
        int count = possibleCells.count();
        System.out.println(count);
        if (count == 0) {
            throw new AssertionError(
                    "\n❌ INVALID DATE: '" + day + "' does not exist in the visible calendar DOM.\n" +
                            "This usually means the site blocked navigation to this invalid date.\n"
            );
        }
        Locator rawCell = possibleCells.first();
        //Checking if disabled ---
        boolean isDisabled =(Boolean) rawCell.evaluate("el => el.classList.contains('picker__day--disabled')");
        if (isDisabled) {
            throw new AssertionError(
                    "\n❌ INVALID DATE SELECTION\n" +
                            "Date attempted: " + dateValue + "\n" +
                            "Drop-off date cannot be earlier than pickup\n"
            );
        }
        rawCell.click();
        page.waitForTimeout(200);
    }


    private void selectTime(String timeFieldSelector, String dateValue) {
        String time = dateValue.split(" ")[1];
        String formattedTime = formatTimeForPicker(time); // e.g., "10:00 AM"
        // Ensuring picker is open (only click if not open)
        if (!page.locator("div.picker--time.picker--opened").isVisible()) {
            page.click(timeFieldSelector);
            page.waitForTimeout(300);
        }
        Locator openPicker = page.locator("div.picker--time.picker--opened");
        Locator timeList = openPicker.locator("ul.picker__list");
        timeList.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        Locator timeOption = timeList.locator("li.picker__list-item")
                .filter(new Locator.FilterOptions().setHasText(formattedTime));
        timeOption.first().scrollIntoViewIfNeeded();
        page.waitForTimeout(200);
        timeOption.first().click();
        page.waitForTimeout(300);
    }

    private String formatTimeForPicker(String time24) {
        DateTimeFormatter input = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter output = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        LocalTime t = LocalTime.parse(time24, input);
        return output.format(t);
    }

    public void search() {
        page.click("#booking-pc > form > button");
        // Allowing web backend request/render
        page.waitForTimeout(1500);
        Locator results = page.locator("div.search_page_row.each_card_form");
        // Waiting until at least one bike card appears
        page.waitForCondition(() -> results.count() > 0,
                new Page.WaitForConditionOptions().setTimeout(15000));
        assertThat(results.count())
                .as("Expected available bikes after search")
                .isGreaterThan(0);
    }

    public void validateDateApplied(String expectedStart, String expectedEnd) {
        // Extracting displayed values from UI
        String uiPickupDate = page.inputValue("#pickup-date-desk");
        String uiPickupTime = page.inputValue("#pickup-time-desk");
        String uiDropoffDate = page.inputValue("#dropoff-date-desk");
        String uiDropoffTime = page.inputValue("#dropoff-time-desk");
        // Converting UI format ( "25 Nov, 2025" + "10:00 AM" -> "2025-11-25 10:00" )
        String actualStart = normalizeDateTime(uiPickupDate, uiPickupTime);
        String actualEnd = normalizeDateTime(uiDropoffDate, uiDropoffTime);
        System.out.println(">>> Actual Start: " + actualStart);
        System.out.println(">>> Expected Start: " + expectedStart);
        System.out.println(">>> Actual End: " + actualEnd);
        System.out.println(">>> Expected End: " + expectedEnd);
        assertThat(actualStart).isEqualTo(expectedStart);
        assertThat(actualEnd).isEqualTo(expectedEnd);
    }

    private String normalizeDateTime(String dateText, String timeText) {
        DateTimeFormatter inputDate = DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH);
        LocalDate parsedDate = LocalDate.parse(dateText, inputDate);
        String formattedDate = parsedDate.toString(); // Converts "25 Nov, 2025" → "2025-11-25"
        // Convert time (e.g., "10:00 AM") → "10:00"
        DateTimeFormatter inputTime = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        DateTimeFormatter outputTime = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime parsedTime = LocalTime.parse(timeText, inputTime);
        return formattedDate + " " + outputTime.format(parsedTime);
    }

    public void applyLocationFilter(String location) {

        String searchKeyword = location.split(" ")[0];
        // Finding all location listings and picking the visible one (desktop)
        Locator visibleLocationList = page.locator(".location_listing")
                .filter(new Locator.FilterOptions().setHasNotText("keyboard_arrow_left"))
                .filter(new Locator.FilterOptions().setHasNotText("Location Search"))
                .first();
        visibleLocationList.waitFor(new Locator.WaitForOptions().setTimeout(8000));
        // Correct search input (avoid mobile duplicate)
        Locator searchInput = page.locator("input.location_input")
                .filter(new Locator.FilterOptions().setHasNotText("pickup"))
                .first();
        searchInput.click();
        searchInput.fill("");
        page.waitForTimeout(300);
        // Type slowly to trigger JS filtering
        searchInput.pressSequentially(searchKeyword,
                new Locator.PressSequentiallyOptions().setDelay(120));
        page.waitForTimeout(800);
        // Count after filtering
        Locator allFilteredResults = visibleLocationList.locator("label");
        int resultCount = allFilteredResults.count();
        System.out.println("Filtered results found: " + resultCount);
        // NEGATIVE SCENARIO HANDLING
        if (resultCount == 0) {
            System.out.println("⚠ No matching location found for: " + location);
            return;
        }
        // POSITIVE SCENARIO: Find exact match
        Locator matchingLabel = allFilteredResults
                .filter(new Locator.FilterOptions().setHasText(location))
                .first();
        if (matchingLabel.count() == 0) {
            // Exists results, but NOT exact match, still negative
            throw new AssertionError("\n❌ [NO EXACT LOCATION FOUND]");
        }

        matchingLabel.scrollIntoViewIfNeeded();
        matchingLabel.click();

        System.out.println("Location filter applied Successfully ✔ : " + location);

        page.waitForTimeout(1000);
    }


    public void validateBikeLocation(String expectedFullLocation) {
        System.out.println("\nVALIDATING DATA FOR FILTERED LOCATION MATCH: " + expectedFullLocation + "\n");
        // Expected format: "Indiranagar - Metro Station"
        String expectedTitle = expectedFullLocation.contains("(")
                ? expectedFullLocation.substring(0, expectedFullLocation.indexOf("(")).trim()
                : expectedFullLocation.trim();
        Locator cards = page.locator(".search_page_row");
        totalCards = cards.count();
        for (int i = 0; i < totalCards; i++) {
            Locator card = cards.nth(i);
            String bikeName = card.locator(".bike_name").innerText().trim();
            // Detecting if bike is sold out (NO dropdown, only a disabled badge/button)
            boolean isSoldOut = card.locator("span.badge-soldout, span:has-text('Sold Out')").first().isVisible();
            if (isSoldOut) {
                soldOut++;
                bikeAvailabilityResults.add("\nVehicle Name: " + bikeName +"\n Location: "+ expectedTitle + "\n Pickup → Sold Out ⚠ ");
                continue;
            }
            // Triggering dropdown for that specific bike
            card.locator(".dropdown-header").click();
            // Waiting until dropdown is visible
            Locator options = card.locator(".dropdown-menu.open .dropdown-options .dropdown-option");
            page.waitForTimeout(500);
            List<String> availableLocations = options.allInnerTexts().stream()
                    .map(text -> text.replaceAll("\\s+", " ").trim())
                    .toList();
            boolean found = availableLocations.stream()
                    .anyMatch(loc -> loc.toLowerCase().contains(expectedTitle.toLowerCase()));
            if (found) {
                matched++;
                bikeAvailabilityResults.add("\nVehicle Name: " + bikeName + "\nLocation: "+ expectedTitle +"\nPickup → AVAILABLE ✔ ");
            } else {
                notMatched++;
                bikeAvailabilityResults.add("\nVehicle Name: " + bikeName + "\nLocation: "+ expectedTitle +"\nPickup → NOT AVAILABLE ❌" );
            }
            // Closing dropdown after validation
            card.locator(".dropdown-header").click();
        }
        // ASSERTION ADDED
        if (matched == 0) {
            throw new AssertionError("\n❌ [NO BIKES MATCH THE SELECTED LOCATION]\nExpected location: "
                    + expectedFullLocation + "\nBut found 0 valid results.\n");
        }
        System.out.println("\nValidation Complete!");
        System.out.println("🏍 Total bikes scanned: " + totalCards);
        System.out.println("✔ Matching bikes: " + matched);
        System.out.println("❌ Not matching: " + notMatched);
        System.out.println("⚠ Sold Out: " + soldOut);

    }
    public void printBikeData() {
        System.out.println("Vehicle record : \n");
        bikeAvailabilityResults.forEach(System.out::println);
        }
    }

