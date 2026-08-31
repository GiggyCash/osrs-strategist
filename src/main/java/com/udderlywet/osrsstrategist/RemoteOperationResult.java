package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RemoteOperationResult
{
    private final boolean accepted;
    private final String message;


    public static RemoteOperationResult disabled(String message)
    {
        return new RemoteOperationResult(false, message);
    }

    public static RemoteOperationResult accepted(String message)
    {
        return new RemoteOperationResult(true, message);
    }


}
