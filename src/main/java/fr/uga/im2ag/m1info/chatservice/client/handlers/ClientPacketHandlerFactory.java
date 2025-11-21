package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.handlers.providers.ClientPacketHandlerProvider;
import fr.uga.im2ag.m1info.chatservice.common.MessageType;

import java.util.*;
import java.util.logging.Logger;

/**
 * Factory for loading and initializing client packet handlers via ServiceLoader.
 */
public class ClientPacketHandlerFactory {
    private static final Logger LOG = Logger.getLogger(ClientPacketHandlerFactory.class.getName());

    private ClientPacketHandlerFactory() {}

    /**
     * Loads all client packet handlers using ServiceLoader and initializes them.
     *
     * @param context the context providing dependencies for initialization
     * @return list of initialized handlers
     */
    public static List<ClientPacketHandler> loadHandlers(ClientHandlerContext context) {
        List<ClientPacketHandler> handlers = new ArrayList<>();
        Set<MessageType> registeredTypes = new HashSet<>();

        ServiceLoader<ClientPacketHandlerProvider> loader = ServiceLoader.load(ClientPacketHandlerProvider.class);

        for (ClientPacketHandlerProvider provider : loader) {
            Set<MessageType> providerTypes = provider.getHandledTypes();

            // Check for conflicts
            for (MessageType type : providerTypes) {
                if (registeredTypes.contains(type)) {
                    LOG.warning(String.format(
                            "Handler conflict: MessageType %s already registered. " +
                                    "Provider %s will be ignored for this type.",
                            type, provider.getClass().getSimpleName()
                    ));
                }
            }

            List<ClientPacketHandler> providerHandlers = provider.createHandlers();
            for (ClientPacketHandler handler : providerHandlers) {
                handler.initialize(context);
                handlers.add(handler);
                LOG.info(String.format(
                        "Registered client handler: %s for types: %s",
                        handler.getClass().getSimpleName(), providerTypes
                ));
            }

            registeredTypes.addAll(providerTypes);
        }

        if (handlers.isEmpty()) {
            LOG.warning("No client packet handler providers found! Check META-INF/services configuration.");
        }

        return handlers;
    }
}