package userregistrationsystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Populates and queries the five Part 3 message arrays. */
final class MessageHistory {
    enum Action {
        SENT("Sent"), DISREGARDED("Disregarded"), STORED("Stored");

        final String label;

        Action(String label) {
            this.label = label;
        }
    }

    private Message[] sent = new Message[0];
    private Message[] disregarded = new Message[0];
    private Message[] stored = new Message[0];
    private final Set<String> usedIds = new HashSet<>();
    private final MessageArchive archive;
    private String storageError;
    private int nextNumber;
    private int enteredCount;

    MessageHistory(Path path) {
        archive = new MessageArchive(path);
        try {
            updateStored(archive.load());
        } catch (IOException e) {
            storageError = e.getMessage();
        }
    }

    String storageError() {
        return storageError;
    }

    Message compose(String sender, String recipient, String text) {
        String id;
        do {
            id = Message.newId();
        } while (usedIds.contains(id));
        return new Message(id, nextNumber, sender, recipient, text);
    }

    void record(Message message, Action action) throws IOException {
        if (!message.checkMessageID() || usedIds.contains(message.getMessageID())
                || message.getRecipient() == null || message.getMessageText() == null) {
            throw new IllegalArgumentException("A new message needs an unused ID, recipient, and text.");
        }

        Message recorded = message.asStatus(action.label);
        switch (action) {
            case SENT -> sent = append(sent, recorded);
            case DISREGARDED -> disregarded = append(disregarded, recorded);
            case STORED -> updateStored(archive.change(current -> append(current, recorded)).after());
        }
        reserve(recorded);
        message.setStatus(action.label);
        enteredCount++;
    }

    private void updateStored(Message[] messages) {
        stored = messages;
        for (Message message : messages) {
            reserve(message);
        }
        storageError = null;
    }

    private void reserve(Message message) {
        usedIds.add(message.getMessageID());
        nextNumber = Math.max(nextNumber, message.getMessageNumber() + 1);
    }

    private static Message[] append(Message[] messages, Message added) {
        Message[] result = Arrays.copyOf(messages, messages.length + 1);
        result[messages.length] = added;
        return result;
    }

    Message[] sentMessages() {
        return sent.clone();
    }

    Message[] disregardedMessages() {
        return disregarded.clone();
    }

    Message[] storedMessages() {
        return stored.clone();
    }

    // Building the two index arrays from the same rows prevents stale IDs or hashes after deletion.
    String[] messageIds() {
        return values(merge(sent, disregarded, stored), Message::getMessageID);
    }

    String[] messageHashes() {
        return values(merge(sent, disregarded, stored), Message::getMessageHash);
    }

    private static Message[] merge(Message[]... groups) {
        return Arrays.stream(groups).flatMap(Arrays::stream)
                .sorted(Comparator.comparingInt(Message::getMessageNumber)).toArray(Message[]::new);
    }

    private static String[] values(Message[] messages, Function<Message, String> field) {
        return Arrays.stream(messages).map(field).toArray(String[]::new);
    }

    Optional<Message> longestStored() {
        return Arrays.stream(stored).max(Comparator.comparingInt(message -> message.getMessageText().length()));
    }

    Optional<Message> findById(String id) {
        Message[] messages = merge(sent, disregarded, stored);
        int index = Arrays.asList(values(messages, Message::getMessageID)).indexOf(id);
        return index < 0 ? Optional.empty() : Optional.of(messages[index]);
    }

    Message[] findForRecipient(String recipient, boolean includeSent) {
        Message[] candidates = includeSent ? merge(sent, stored) : stored;
        return Arrays.stream(candidates).filter(message -> message.getRecipient().equals(recipient))
                .toArray(Message[]::new);
    }

    Optional<Message> deleteStored(String hash) throws IOException {
        MessageArchive.Change change = archive.change(current -> {
            int index = Arrays.asList(values(current, Message::getMessageHash)).indexOf(hash);
            if (index < 0) {
                return current;
            }
            Message[] updated = new Message[current.length - 1];
            System.arraycopy(current, 0, updated, 0, index);
            System.arraycopy(current, index + 1, updated, index, current.length - index - 1);
            return updated;
        });
        updateStored(change.after());
        if (change.before() == change.after()) {
            return Optional.empty();
        }
        return Arrays.stream(change.before()).filter(message -> message.getMessageHash().equals(hash)).findFirst();
    }

    String storedSendersAndRecipients() {
        if (stored.length == 0) {
            return "No stored messages are available.";
        }
        return Arrays.stream(stored).map(message -> "Sender: "
                + (message.getSender().isBlank() ? "Unknown" : message.getSender())
                + " | Recipient: " + message.getRecipient()).collect(Collectors.joining("\n"));
    }

    String sentReport() {
        return report(sent, "No sent messages are available.");
    }

    String storedReport() {
        return report(stored, "No stored messages are available.");
    }

    private static String report(Message[] messages, String emptyText) {
        return messages.length == 0 ? emptyText : Arrays.stream(messages)
                .map(Message::details).collect(Collectors.joining("\n\n"));
    }

    int sentCount() {
        return sent.length;
    }

    int enteredCount() {
        return enteredCount;
    }
}
