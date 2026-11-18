package userregistrationsystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class QuickChatPart3Test {
    private static final String SENDER = "+27716774402";
    private static final String RECIPIENT = "+27838884567";
    private static final String LATE = "Where are you? You are late! I have asked you to be on time.";
    private static final String LEAVING = "Ok, I am leaving without you.";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path file;
    private MessageHistory history;
    private Message cake;
    private Message late;
    private Message gate;
    private Message dinner;
    private Message leaving;

    @Before
    public void setUpMessages() throws IOException {
        file = temporaryFolder.newFolder().toPath().resolve("messages.json");
        history = new MessageHistory(file);
        cake = fixture(0, "+27834557896", "Did you get the cake?");
        late = fixture(1, RECIPIENT, LATE);
        gate = fixture(2, "+27834484567", "Yohoooo, I am at your gate.");
        // Page 18's fixture uses the local number and includes a space before the exclamation mark.
        dinner = fixture(3, "0838884567", "It is dinner time !");
        leaving = fixture(4, RECIPIENT, LEAVING);

        history.record(cake, MessageHistory.Action.SENT);
        history.record(late, MessageHistory.Action.STORED);
        history.record(gate, MessageHistory.Action.DISREGARDED);
        history.record(dinner, MessageHistory.Action.SENT);
        history.record(leaving, MessageHistory.Action.STORED);
    }

    @Test
    public void fillsAllFiveArraysInMessageOrder() {
        assertArrayEquals(new String[]{cake.getMessageText(), dinner.getMessageText()}, text(history.sentMessages()));
        assertArrayEquals(new String[]{gate.getMessageText()}, text(history.disregardedMessages()));
        assertArrayEquals(new String[]{LATE, LEAVING}, text(history.storedMessages()));

        Message[] fixtures = {cake, late, gate, dinner, leaving};
        assertArrayEquals(Arrays.stream(fixtures).map(Message::getMessageID).toArray(String[]::new),
                history.messageIds());
        assertArrayEquals(Arrays.stream(fixtures).map(Message::getMessageHash).toArray(String[]::new),
                history.messageHashes());
        assertEquals(2, history.sentCount());
        assertEquals(5, history.enteredCount());
        assertEquals("Disregarded", gate.getStatus());
    }

    @Test
    public void selectsTheLongestStoredMessage() {
        assertEquals(LATE, history.longestStored().orElseThrow().getMessageText());
    }

    @Test
    public void findsDinnerByIdAndKeepsTheOriginalNumberAndText() {
        Message found = history.findById(dinner.getMessageID()).orElseThrow();
        assertEquals("0838884567", found.getRecipient());
        assertEquals("It is dinner time !", found.getMessageText());
        assertFalse(history.findById("9999999999").isPresent());
    }

    @Test
    public void findsTheExpectedStoredAndSentMessagesForARecipient() {
        assertArrayEquals(new String[]{LATE, LEAVING}, text(history.findForRecipient(RECIPIENT, false)));
        assertArrayEquals(new String[]{LATE, LEAVING}, text(history.findForRecipient(RECIPIENT, true)));
        assertArrayEquals(new String[]{cake.getMessageText()}, text(history.findForRecipient(cake.getRecipient(), true)));
        assertEquals(0, history.findForRecipient(cake.getRecipient(), false).length);
        assertEquals(0, history.findForRecipient(gate.getRecipient(), true).length);
    }

    @Test
    public void deletesByHashAndFindsOnlyTheRemainingMessageAfterRestart() throws IOException {
        assertEquals(LATE, history.deleteStored(late.getMessageHash()).orElseThrow().getMessageText());
        assertFalse(history.findById(late.getMessageID()).isPresent());
        assertFalse(Arrays.asList(history.messageIds()).contains(late.getMessageID()));
        assertFalse(Arrays.asList(history.messageHashes()).contains(late.getMessageHash()));
        assertFalse(history.deleteStored(late.getMessageHash()).isPresent());
        MessageHistory reloaded = new MessageHistory(file);
        assertArrayEquals(new String[]{LEAVING}, text(reloaded.storedMessages()));
    }

    @Test
    public void showsBothSentAndStoredReportsWithRequiredDetails() {
        String sent = history.sentReport();
        assertContainsDetails(sent, cake);
        assertContainsDetails(sent, dinner);
        assertFalse(sent.contains(late.getMessageHash()));

        String stored = new MessageHistory(file).storedReport();
        assertContainsDetails(stored, late);
        assertContainsDetails(stored, leaving);
        assertFalse(stored.contains(cake.getMessageHash()));
    }

    @Test
    public void reloadsSendersAndRecipientsFromTheArchive() {
        MessageHistory reloaded = new MessageHistory(file);
        assertEquals(SENDER, reloaded.storedMessages()[0].getSender());
        assertEquals("Sender: " + SENDER + " | Recipient: " + RECIPIENT + "\nSender: "
                + SENDER + " | Recipient: " + RECIPIENT, reloaded.storedSendersAndRecipients());
    }

    @Test
    public void reopensPart2JsonWithTheOriginalFieldsAndNoSender() throws IOException {
        Path legacy = file.resolveSibling("legacy.json");
        Files.writeString(legacy, """
                [{"messageID":"0234567890","messageNumber":8,"recipient":"+27838884567",
                "messageText":"He said \\"hello\\".","messageHash":"02:8:HEHELLO","status":"Stored"}]
                """);
        MessageHistory reloaded = new MessageHistory(legacy);
        Message message = reloaded.findById("0234567890").orElseThrow();
        assertEquals("He said \"hello\".", message.getMessageText());
        assertEquals("02:8:HEHELLO", message.getMessageHash());
        assertTrue(reloaded.storedSendersAndRecipients().contains("Sender: Unknown"));
        assertEquals(9, reloaded.compose(SENDER, RECIPIENT, "Another message").getMessageNumber());
    }

    @Test
    public void twoSessionsKeepEachOthersStoredChanges() throws IOException {
        MessageHistory second = new MessageHistory(file);
        history.deleteStored(late.getMessageHash());
        Message additional = fixture(5, RECIPIENT, "I am on my way.");
        second.record(additional, MessageHistory.Action.STORED);
        assertArrayEquals(new String[]{LEAVING, additional.getMessageText()},
                text(new MessageHistory(file).storedMessages()));
    }

    @Test
    public void doesNotOverwriteMalformedJson() throws IOException {
        String damaged = "[incomplete";
        Files.writeString(file, damaged);
        MessageHistory broken = new MessageHistory(file);
        assertNotNull(broken.storageError());
        assertThrows(IOException.class,
                () -> broken.record(fixture(5, RECIPIENT, "Keep the archive."), MessageHistory.Action.STORED));
        assertEquals(damaged, Files.readString(file));
    }

    @Test
    public void preservesPart2RecipientValidationAndFormatsTheMessageHash() {
        Message part2 = fixture(0, "+27718693002", "Hi Mike, can you join us for dinner tonight?");
        assertTrue(part2.checkMessageID());
        assertTrue(part2.checkRecipientCell());
        assertTrue(dinner.checkRecipientCell());
        assertFalse(new Message(0, "12345", "Invalid number").checkRecipientCell());
        assertEquals("00:0:HITONIGHT", part2.createMessageHash());
    }

    private static Message fixture(int number, String recipient, String message) {
        return new Message("001234567" + number, number, SENDER, recipient, message);
    }

    private static String[] text(Message[] messages) {
        return Arrays.stream(messages).map(Message::getMessageText).toArray(String[]::new);
    }

    private static void assertContainsDetails(String report, Message message) {
        assertTrue(report.contains("Message Hash: " + message.getMessageHash()));
        assertTrue(report.contains("Recipient: " + message.getRecipient()));
        assertTrue(report.contains("Message: " + message.getMessageText()));
    }
}
