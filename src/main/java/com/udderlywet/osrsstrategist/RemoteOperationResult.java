package com.udderlywet.osrsstrategist;

public final class RemoteOperationResult
{
    private final boolean accepted;
    private final String message;

    private RemoteOperationResult(boolean accepted, String message)
    {
        this.accepted = accepted;
        this.message = message;
    }

    public static RemoteOperationResult disabled(String message)
    {
        return new RemoteOperationResult(false, message);
    }

    public static RemoteOperationResult accepted(String message)
    {
        return new RemoteOperationResult(true, message);
    }

    public boolean isAccepted()
    {
        return accepted;
    }

    public String getMessage()
    {
        return message;
    }
}
