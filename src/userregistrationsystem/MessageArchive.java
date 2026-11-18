package userregistrationsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import java.io.IOException;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/** Uses Gson's array serialization and deserialization as documented in [1] in README.md. */
final class MessageArchive {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().setStrictness(Strictness.STRICT).create();
    private final Path file;

    MessageArchive(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    Message[] load() throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            Message[] messages = JSON.fromJson(reader, Message[].class);
            validate(messages);
            for (int index = 0; index < messages.length; index++) {
                messages[index] = messages[index].asStatus("Stored");
            }
            return messages;
        } catch (NoSuchFileException e) {
            return new Message[0];
        } catch (JsonParseException e) {
            throw new IOException("Could not read the stored messages in " + file.getFileName(), e);
        }
    }

    record Change(Message[] before, Message[] after) {
    }

    Change change(UnaryOperator<Message[]> operation) throws IOException {
        // Re-read while holding the lock so a second QuickChat session can keep its changes.
        Path lockPath = file.resolveSibling(file.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE); FileLock lock = channel.tryLock()) {
            if (lock == null) {
                throw new IOException("Another QuickChat session is updating stored messages. Try again.");
            }
            Message[] current = load();
            Message[] updated = operation.apply(current);
            if (updated != current) {
                validate(updated);
                write(updated);
            }
            return new Change(current, updated);
        } catch (OverlappingFileLockException e) {
            throw new IOException("Another QuickChat session is updating stored messages. Try again.", e);
        }
    }

    private void validate(Message[] messages) throws IOException {
        if (messages == null) {
            throw new IOException("Expected a JSON array in " + file.getFileName());
        }
        Set<String> ids = new HashSet<>();
        for (Message message : messages) {
            if (message == null || !message.checkMessageID() || message.getMessageNumber() < 0
                    || message.getRecipient() == null || message.getMessageText() == null
                    || message.getMessageHash() == null || !ids.add(message.getMessageID())) {
                throw new IOException("Invalid or duplicate message in " + file.getFileName());
            }
        }
    }

    private void write(Message[] messages) throws IOException {
        Path draft = Files.createTempFile(file.getParent(), "quickchat-", ".tmp");
        try {
            Files.writeString(draft, JSON.toJson(messages) + System.lineSeparator());
            try {
                Files.move(draft, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(draft, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(draft);
        }
    }
}
