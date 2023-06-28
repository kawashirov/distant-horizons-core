package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.network.messages.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MessageRegistry {
    public static final MessageRegistry INSTANCE = new MessageRegistry() {{
        // Note: Make sure IDs are unique
        // Also note: Removing IDs will break backwards compatibility
        registerMessage(1, HelloMessage.class, HelloMessage::new);
    }};

    private final Map<Integer, Supplier<Message>> idToSupplier = new HashMap<>();
    private final Map<Class<?>, Integer> classToId = new HashMap<>();

    private MessageRegistry() { }

    public <T extends Message> void registerMessage(int id, Class<T> clazz, Supplier<T> supplier) {
        idToSupplier.put(id, (Supplier<Message>) supplier);
        classToId.put(clazz, id);
    }

    public Message createMessage(int id) throws IllegalArgumentException {
        try {
            return idToSupplier.get(id).get();
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid message ID");
        }
    }

    public int getMessageId(Message message) {
        return classToId.get(message.getClass());
    }
}
