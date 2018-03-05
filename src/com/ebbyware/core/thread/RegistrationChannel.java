package com.ebbyware.core.thread;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface RegistrationChannel {
    SelectionKey register(Selector selector, int operations, Object context)
            throws ClosedChannelException;
}
