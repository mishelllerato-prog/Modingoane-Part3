package userregistrationsystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One QuickChat message, including the fields used by the Part 2 JSON archive. */
public class Message {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private final String messageID;
    private final int messageNumber;
    private final String recipient;
    private final String messageText;
    private final String messageHash;
    private final String sender;
    private String status;

    public Message() {
        this(0, null, null);
    }

    public Message(int messageNumber, String recipient, String messageText) {
        this(newId(), messageNumber, "", recipient, messageText);
    }

    Message(String id, int number, String sender, String recipient, String text) {
        this.messageID = id;
        this.messageNumber = number;
        this.recipient = recipient;
        this.messageText = text;
        this.sender = sender;
        this.status = "Not processed";
        this.messageHash = createMessageHash();
    }

    private Message(Message source, String status) {
        this.messageID = source.messageID;
        this.messageNumber = source.messageNumber;
        this.recipient = source.recipient;
        this.messageText = source.messageText;
        this.messageHash = source.messageHash;
        this.sender = source.sender;
        this.status = status;
    }

    static String newId() {
        return String.format(Locale.ROOT, "%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
    }

    public boolean checkMessageID() {
        return messageID != null && messageID.matches("[0-9]{10}");
    }

    public boolean checkRecipientCell() {
        return isRecipientCell(recipient);
    }

    static boolean isRecipientCell(String number) {
        return number != null && (number.matches("\\+27[6-8][0-9]{8}")
                || number.matches("0[6-8][0-9]{8,9}"));
    }

    public String createMessageHash() {
        if (messageID == null || messageID.length() < 2) {
            return "";
        }

        String first = "";
        String last = "";
        if (messageText != null) {
            Matcher words = WORD.matcher(messageText);
            while (words.find()) {
                last = words.group();
                if (first.isEmpty()) {
                    first = last;
                }
            }
        }
        return messageID.substring(0, 2) + ":" + messageNumber + ":"
                + (first + last).toUpperCase(Locale.ROOT);
    }

    Message asStatus(String status) {
        return new Message(this, status);
    }

    void setStatus(String status) {
        this.status = status;
    }

    public String getMessageID() {
        return messageID;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public String getStatus() {
        return status;
    }

    public String getSender() {
        return sender == null ? "" : sender;
    }

    String details() {
        return "Message ID: " + messageID + "\nMessage Hash: " + messageHash
                + "\nRecipient: " + recipient + "\nMessage: " + messageText
                + "\nSender: " + (getSender().isBlank() ? "Unknown" : getSender())
                + "\nMessage Number: " + messageNumber + "\nStatus: " + status;
    }

    // Retain the public Part 2 console methods alongside the testable session.
    private static class DefaultSession {
        static final MessageHistory HISTORY = new MessageHistory(Path.of("messages.json"));
    }

    public static void startQuickChat(Scanner input) {
        startQuickChat(input, "");
    }

    public static void startQuickChat(Scanner input, String sender) {
        new QuickChat(input, DefaultSession.HISTORY, sender).run();
    }

    public void SentMessage(Scanner input) {
        new QuickChat(input, DefaultSession.HISTORY, getSender()).chooseAction(this);
    }

    public void printMessageDetails() {
        System.out.println(details());
    }

    public static void printMessages() {
        System.out.println(DefaultSession.HISTORY.sentReport());
    }

    public static int returnTotalMessages() {
        return DefaultSession.HISTORY.sentCount();
    }

    public static int returnTotalMessagess() {
        return returnTotalMessages();
    }

    public static int getTotalMessagesEntered() {
        return DefaultSession.HISTORY.enteredCount();
    }

    public void storeMessage() {
        try {
            DefaultSession.HISTORY.record(this, MessageHistory.Action.STORED);
            System.out.println("Message successfully stored.");
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not store message: " + e.getMessage());
        }
    }
}
