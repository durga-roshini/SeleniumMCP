import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;

public class LoginVerificationTest {

    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();

        try {
            // Step 1: Open page
            driver.get("https://practicetestautomation.com/practice-test-login/");
            Thread.sleep(2000);

            // Step 2: Type username
            WebElement usernameField = driver.findElement(By.id("username"));
            usernameField.sendKeys("student");

            // Step 3: Type password
            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys("Password123");

            // Step 4: Push Submit button
            WebElement submitButton = driver.findElement(By.id("submit"));
            submitButton.click();
            Thread.sleep(3000);

            // Step 5: Verify URL
            String currentUrl = driver.getCurrentUrl();
            assert currentUrl.contains("practicetestautomation.com/logged-in-successfully/")
                    : "URL verification failed";
            System.out.println("✓ URL verification passed");

            // Step 6: Verify success message
            WebElement pageContent = driver.findElement(By.tagName("body"));
            String pageText = pageContent.getText();
            assert pageText.contains("Congratulations") || pageText.contains("successfully logged in")
                    : "Success message not found";
            System.out.println("✓ Success message verified");

            // Step 7: Verify Log out button
            WebElement logoutButton = driver.findElement(By.xpath("//a[contains(text(), 'Log out')]"));
            assert logoutButton.isDisplayed() : "Log out button not found";
            System.out.println("✓ Log out button verified");

            System.out.println("\n✓ All tests passed for KAN-4: Login Verification");

        } catch (Exception e) {
            System.err.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}