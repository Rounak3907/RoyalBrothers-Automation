package steps;

import com.microsoft.playwright.Page;
import io.cucumber.java.en.*;
import pages.RoyalBrothersPage;
import utils.PlaywrightFactory;
import static org.assertj.core.api.Assertions.assertThat;


public class RoyalBrothersSteps {
    Page page;
    RoyalBrothersPage rb;
    String startDate, endDate;

    @Given("I opened the Royal Brothers website.")
    public void openSite() {
        page = PlaywrightFactory.initBrowser();
        rb = new RoyalBrothersPage(page);
        rb.navigate();
    }

    @When("I selected City {string}.")
    public void selectCity(String city) {
        rb.selectCity(city);
    }

    @When("I entered Booking time {string} to {string}.")
    public void enterTime(String start, String end) {
        startDate = start;
        endDate = end;
        rb.enterBookingTime(start, end);
    }

    @When("I clicked on Search.")
    public void clickSearch() {
        rb.search();
    }

    @Then("the selected date range is revalidated.")
    public void validateDate() {
        rb.validateDateApplied(startDate, endDate);
    }

    @When("I applied location filter {string}.")
    public void applyLocation(String loc) {
        rb.applyLocationFilter(loc);
    }

    @Then("all shown bikes are validated to belong to {string}.")
    public void validateLocation(String loc) {
        rb.validateBikeLocation(loc);
    }

    @Then("all Bike details are printed.")
    public void printData() {
        rb.printBikeData();
        PlaywrightFactory.closeBrowser();
    }

    @Then("an error message should be shown for invalid date selection")
    public void validateInvalidDateMessage() {
        String error = page.locator(".error-message").innerText();
        assertThat(error).contains("Drop-off date cannot be earlier than pickup");
    }
    @Then("no location results should be displayed")
    public void validateNoLocationResults() {
        int count = page.locator(".location_listing .location").count();
        assertThat(count).isEqualTo(0);
    }
    @Then("an Timeout error message should appear.")
    public void validateCityRequiredMessage(String msg) {
        String toast = page.locator(".toast").innerText();
        assertThat(toast).contains(msg);
    }

}
