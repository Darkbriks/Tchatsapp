package fr.uga.im2ag.m1info.chatservice.client.event.types;

import fr.uga.im2ag.m1info.chatservice.client.event.system.Event;
import fr.uga.im2ag.m1info.chatservice.common.messagefactory.ErrorMessage;

/**
 * Event representing an error received from the server.
 */
public class ErrorEvent extends Event {
    private final ErrorMessage.ErrorLevel errorLevel;
    private final String errorType;
    private final String errorMessage;

    /** Constructor for the ErrorEvent class.
     *
     * @param source The source object that generated the event.
     * @param errorLevel The severity level of the error.
     * @param errorType The type/category of the error.
     * @param errorMessage The detailed error message.
     */
    public ErrorEvent(Object source, ErrorMessage.ErrorLevel errorLevel, String errorType, String errorMessage) {
        super(source);
        this.errorLevel = errorLevel;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    /** Gets the error level.
     *
     * @return The error level.
     */
    public ErrorMessage.ErrorLevel getErrorLevel() {
        return errorLevel;
    }

    /** Gets the error type.
     *
     * @return The error type.
     */
    public String getErrorType() {
        return errorType;
    }

    /** Gets the error message.
     *
     * @return The error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /** Gets the specific type of the event.
     *
     * @return The class type of the event.
     */
    @Override
    public Class<? extends Event> getEventType() {
        return ErrorEvent.class;
    }
}