package fr.uga.im2ag.m1info.chatservice.client.handlers;

import fr.uga.im2ag.m1info.chatservice.client.command.PendingCommandManager;

/**
 * Context providing dependencies for client packet handlers initialization.
 */
public class ClientHandlerContext {
    private final PendingCommandManager commandManager;

    private ClientHandlerContext(Builder builder) {
        this.commandManager = builder.commandManager;
    }

    public PendingCommandManager getCommandManager() {
        return commandManager;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PendingCommandManager commandManager;

        public Builder commandManager(PendingCommandManager commandManager) {
            this.commandManager = commandManager;
            return this;
        }

        public ClientHandlerContext build() {
            return new ClientHandlerContext(this);
        }
    }
}