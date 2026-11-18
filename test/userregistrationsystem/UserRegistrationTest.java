package userregistrationsystem;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class UserRegistrationTest {

    private Login login;

    public UserRegistrationTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {

        login = new Login("Mashudu", "Nedzi");
    }

    @After
    public void tearDown() {
    }

    // ==========================================
    // USERNAME TESTS
    // ==========================================

    @Test
    public void testCheckUserNameCorrect() {

        boolean result = login.checkUserName("M_01");

        assertTrue(result);
    }

    @Test
    public void testCheckUserNameIncorrect() {

        boolean result = login.checkUserName("Mashudu");

        assertFalse(result);
    }

    @Test
    public void testCheckUserNameTooLong() {

        boolean result = login.checkUserName("M_12345");

        assertFalse(result);
    }

    // ==========================================
    // PASSWORD TESTS
    // ==========================================

    @Test
    public void testCheckPasswordComplexityCorrect() {

        boolean result =
                login.checkPasswordComplexity("Password1!");

        assertTrue(result);
    }

    @Test
    public void testCheckPasswordComplexityIncorrect() {

        boolean result =
                login.checkPasswordComplexity("password");

        assertFalse(result);
    }

    @Test
    public void testPasswordWithoutCapitalLetter() {

        boolean result =
                login.checkPasswordComplexity("password1!");

        assertFalse(result);
    }

    @Test
    public void testPasswordWithoutNumber() {

        boolean result =
                login.checkPasswordComplexity("Password!");

        assertFalse(result);
    }

    @Test
    public void testPasswordWithoutSpecialCharacter() {

        boolean result =
                login.checkPasswordComplexity("Password1");

        assertFalse(result);
    }

    // ==========================================
    // CELLPHONE NUMBER TESTS
    // ==========================================

    @Test
    public void testCheckCellPhoneNumberCorrect() {

        boolean result =
                login.checkCellPhoneNumber("+27838968976");

        assertTrue(result);
    }

    @Test
    public void testCheckCellPhoneNumberIncorrect() {

        boolean result =
                login.checkCellPhoneNumber("0838968976");

        assertFalse(result);
    }

    // ==========================================
    // REGISTRATION TESTS
    // ==========================================

    @Test
    public void testRegisterUserSuccessful() {

        String result = login.registerUser(
                "M_01",
                "Password1!",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        assertEquals(
                "Registration successful.",
                result
        );
    }

    @Test
    public void testRegisterUserIncorrectUsername() {

        String result = login.registerUser(
                "Mashudu",
                "Password1!",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        assertEquals(
                "Username is not correctly formatted, please ensure that your username contains an underscore and is no more than five characters in length.",
                result
        );
    }

    @Test
    public void testRegisterUserIncorrectPassword() {

        String result = login.registerUser(
                "M_01",
                "password",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        assertEquals(
                "Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.",
                result
        );
    }

    @Test
    public void testRegisterUserIncorrectCellPhone() {

        String result = login.registerUser(
                "M_01",
                "Password1!",
                "0838968976",
                "Mashudu",
                "Nedzi"
        );

        assertEquals(
                "Cell phone number incorrectly formatted or does not contain international code.",
                result
        );
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    public void testLoginUserSuccessful() {

        login.registerUser(
                "M_01",
                "Password1!",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        boolean result =
                login.loginUser("M_01", "Password1!");

        assertTrue(result);
    }

    @Test
    public void testLoginUserIncorrectUsername() {

        login.registerUser(
                "M_01",
                "Password1!",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        boolean result =
                login.loginUser("Wrong_1", "Password1!");

        assertFalse(result);
    }

    @Test
    public void testLoginUserIncorrectPassword() {

        login.registerUser(
                "M_01",
                "Password1!",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        boolean result =
                login.loginUser("M_01", "WrongPass1!");

        assertFalse(result);
    }

    // ==========================================
    // LOGIN STATUS TESTS
    // ==========================================

    @Test
    public void testReturnLoginStatusSuccessful() {

        login.registerUser(
                "M_01",
                "Password1!",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        String result =
                login.returnLoginStatus(
                        "M_01",
                        "Password1!"
                );

        assertEquals(
                "Welcome Mashudu Nedzi it is great to see you again.",
                result
        );
    }

    @Test
    public void testReturnLoginStatusUnsuccessful() {

        login.registerUser(
                "M_01",
                "Password1!",
                "+27838968976",
                "Mashudu",
                "Nedzi"
        );

        String result =
                login.returnLoginStatus(
                        "Wrong_1",
                        "WrongPass1!"
                );

        assertEquals(
                "Username or password incorrect, please try again.",
                result
        );
    }

    // ==========================================
    // INTEGRATION TEST
    // ==========================================

    @Test
    public void testRegistrationAndLoginIntegration() {

        // Register the user

        String registrationResult =
                login.registerUser(
                        "M_01",
                        "Password1!",
                        "+27838968976",
                        "Mashudu",
                        "Nedzi"
                );

        assertEquals(
                "Registration successful.",
                registrationResult
        );

        // Use the registered username and password
        // to test the login process

        boolean loginResult =
                login.loginUser(
                        "M_01",
                        "Password1!"
                );

        assertTrue(loginResult);

        // Check the final login status

        String status =
                login.returnLoginStatus(
                        "M_01",
                        "Password1!"
                );

        assertEquals(
                "Welcome Mashudu Nedzi it is great to see you again.",
                status
        );
    }
}