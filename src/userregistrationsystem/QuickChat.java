package userregistrationsystem;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

/** Console interface entered after successful registration and login. */
final class QuickChat {
    private final Scanner input;
    private final MessageHistory history;
    private final String sender;

    QuickChat(Scanner input, MessageHistory history, String sender) {
        this.input = input;
        this.history = history;
        this.sender = sender;
    }

    void run() {
        if (history.storageError() != null) {
            System.out.println(history.storageError());
        }
        while (true) {
            System.out.println("\n========== QuickChat ==========");
            System.out.println("1. Send Messages");
            System.out.println("2. Show recently sent messages");
            System.out.println("3. Quit");
            System.out.println("4. Stored Messages");
            String choice = read("Choose an option: ");
            if (choice == null) {
                return;
            }
            switch (choice) {
                case "1" -> sendMessages();
                case "2" -> System.out.println(history.sentReport());
                case "3" -> {
                    System.out.println("Thank you for using QuickChat.");
                    System.out.println("Total messages sent: " + history.sentCount());
                    return;
                }
                case "4" -> storedMenu();
                default -> System.out.println("Invalid option. Choose 1, 2, 3 or 4.");
            }
        }
    }

    private void sendMessages() {
        int count;
        while (true) {
            String value = read("How many messages would you like to enter? ");
            if (value == null) {
                return;
            }
            try {
                count = Integer.parseInt(value);
                if (count > 0) {
                    break;
                }
            } catch (NumberFormatException ignored) {
                // The same prompt handles non-numeric and non-positive input.
            }
            System.out.println("Please enter a positive whole number.");
        }

        for (int index = 0; index < count; index++) {
            System.out.println("\n--- Message " + (index + 1) + " of " + count + " ---");
            String recipient = readRecipient();
            if (recipient == null) {
                return;
            }
            String text = readMessageText();
            if (text == null) {
                return;
            }
            Message message = history.compose(sender, recipient, text);
            System.out.println("Message ID generated: " + message.getMessageID());
            System.out.println("Message Hash: " + message.getMessageHash());
            chooseAction(message);
        }
        System.out.println("Total messages sent: " + history.sentCount());
    }

    private String readRecipient() {
        while (true) {
            String number = read("Enter recipient cellphone number: ");
            if (number == null) {
                return null;
            }
            if (Message.isRecipientCell(number)) {
                System.out.println("Cell phone number successfully captured.");
                return number;
            }
            System.out.println("Use a South African mobile number, for example +27716774402.");
        }
    }

    private String readMessageText() {
        while (true) {
            System.out.print("Enter your message (maximum 250 characters): ");
            if (!input.hasNextLine()) {
                return null;
            }
            String text = input.nextLine();
            if (text.isBlank()) {
                System.out.println("Message cannot be empty.");
            } else if (text.length() > 250) {
                System.out.println("Message exceeds 250 characters by " + (text.length() - 250)
                        + "; please reduce the size.");
            } else {
                System.out.println("Message ready to send.");
                return text;
            }
        }
    }

    void chooseAction(Message message) {
        while (true) {
            System.out.println("1. Send Message\n2. Disregard Message\n3. Store Message to send later");
            System.out.println("Press 0 to delete the message.");
            String choice = read("Enter your choice: ");
            if (choice == null) {
                return;
            }
            MessageHistory.Action action = switch (choice) {
                case "1" -> MessageHistory.Action.SENT;
                case "0", "2" -> MessageHistory.Action.DISREGARDED;
                case "3" -> MessageHistory.Action.STORED;
                default -> null;
            };
            if (action == null) {
                System.out.println("Invalid choice. Choose 0, 1, 2 or 3.");
                continue;
            }
            try {
                history.record(message, action);
                switch (action) {
                    case SENT -> {
                        System.out.println("Message successfully sent.");
                        message.printMessageDetails();
                    }
                    case DISREGARDED -> System.out.println("Message disregarded.");
                    case STORED -> System.out.println("Message successfully stored.");
                }
                return;
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Could not record the message: " + e.getMessage());
                return;
            }
        }
    }

    private void storedMenu() {
        while (true) {
            System.out.println("\n========== Stored Messages ==========");
            System.out.println("1. Show senders and recipients");
            System.out.println("2. Show longest stored message");
            System.out.println("3. Find a message by ID");
            System.out.println("4. Find stored messages for a recipient");
            System.out.println("5. Delete a stored message by hash");
            System.out.println("6. Show stored messages report");
            System.out.println("7. Find sent and stored messages for a recipient");
            System.out.println("8. Back to QuickChat");
            String choice = read("Choose an option: ");
            if (choice == null || choice.equals("8")) {
                return;
            }
            switch (choice) {
                case "1" -> System.out.println(history.storedSendersAndRecipients());
                case "2" -> System.out.println(history.longestStored().map(Message::details)
                        .orElse("No stored messages are available."));
                case "3" -> findById();
                case "4" -> findForRecipient(false);
                case "5" -> deleteByHash();
                case "6" -> System.out.println(history.storedReport());
                case "7" -> findForRecipient(true);
                default -> System.out.println("Invalid option. Choose a number from 1 to 8.");
            }
        }
    }

    private void findById() {
        String id = read("Enter the message ID: ");
        if (id != null) {
            System.out.println(history.findById(id).map(message -> "Recipient: " + message.getRecipient()
                    + "\nMessage: " + message.getMessageText()).orElse("No message found."));
        }
    }

    private void findForRecipient(boolean includeSent) {
        String recipient = read("Enter the recipient's cellphone number: ");
        if (recipient == null) {
            return;
        }
        Message[] found = history.findForRecipient(recipient, includeSent);
        if (found.length == 0) {
            System.out.println("No messages found for that recipient.");
        }
        for (Message message : found) {
            System.out.println("Message: " + message.getMessageText());
        }
    }

    private void deleteByHash() {
        String hash = read("Enter the message hash: ");
        if (hash == null) {
            return;
        }
        try {
            Optional<Message> deleted = history.deleteStored(hash);
            System.out.println(deleted.map(message -> "Message: \"" + message.getMessageText()
                    + "\" successfully deleted.").orElse("No stored message has that hash."));
        } catch (IOException e) {
            System.out.println("Could not delete the message: " + e.getMessage());
        }
    }

    private String read(String prompt) {
        System.out.print(prompt);
        return input.hasNextLine() ? input.nextLine().trim() : null;
    }
}
