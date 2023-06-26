package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.network.messages.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MessageRegistry {
    private final Map<Integer, Supplier<Message>> idToConstructor = new HashMap<Integer, Supplier<Message>>() {{
        // Note: Make sure IDs are unique
        // Also note: Removing IDs will break backwards compatibility
        put(1, HelloMessage::new);
    }};

    private final Map<Class<?>, Integer> classToId = idToConstructor.entrySet().stream()
            .collect(Collectors.toMap(
                    e -> e.getValue().getClass(),
                    Map.Entry::getKey
            ));

    public Message createMessage(int id) throws IllegalArgumentException {
        try {
            return idToConstructor.get(id).get();
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid message ID");
        }
    }

    public int getMessageId(Message message) {
        return classToId.get(message.getClass());
    }
}
