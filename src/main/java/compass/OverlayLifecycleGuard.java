package compass;

/** Idempotence guard for sidebar overlay registration and removal. */
final class OverlayLifecycleGuard
{
    private boolean registered;

    boolean beginRegistration()
    {
        if (registered) return false;
        registered = true;
        return true;
    }

    boolean beginRemoval()
    {
        if (!registered) return false;
        registered = false;
        return true;
    }

    boolean isRegistered() { return registered; }
}
