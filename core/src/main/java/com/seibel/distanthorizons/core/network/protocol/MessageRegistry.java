package com.seibel.distanthorizons.core.network.protocol;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.seibel.distanthorizons.core.network.messages.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MessageRegistry {
    public static final MessageRegistry INSTANCE = new MessageRegistry() {{
        // Note: Messages must have parameterless constructors

        // Keep messages below intact so client/server can disconnect if version does not match
        registerMessage(HelloMessage.class, HelloMessage::new);
        registerMessage(CloseReasonMessage.class, CloseReasonMessage::new);

        // Define your messages after this line
        registerMessage(AckMessage.class, AckMessage::new);
        registerMessage(PlayerUUIDMessage.class, PlayerUUIDMessage::new);
        registerMessage(LodConfigMessage.class, LodConfigMessage::new);
        registerMessage(RequestChunksMessage.class, RequestChunksMessage::new);
    }};

    private final Map<Integer, Supplier<? extends INetworkMessage>> idToSupplier = new HashMap<>();
    private final BiMap<Class<? extends INetworkMessage>, Integer> classToId = HashBiMap.create();

    private MessageRegistry() { }

    public <T extends INetworkMessage> void registerMessage(Class<T> clazz, Supplier<T> supplier) {
        int id = idToSupplier.size() + 1;
        idToSupplier.put(id, supplier);
        classToId.put(clazz, id);
    }

    public Class<? extends INetworkMessage> getClassById(int id) {
        return classToId.inverse().get(id);
    }

    public INetworkMessage createMessage(int id) throws IllegalArgumentException {
        try {
            return idToSupplier.get(id).get();
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid message ID");
        }
    }

    public int getMessageId(INetworkMessage message) {
        return getMessageId(message.getClass());
    }

    public int getMessageId(Class<? extends INetworkMessage> clazz) {
        return classToId.get(clazz);
    }
}
