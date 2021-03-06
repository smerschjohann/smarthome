/**
 * Copyright (c) 2014,2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.internal.events;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.common.SafeCaller;
import org.eclipse.smarthome.core.events.EventFactory;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle Eclipse SmartHome events encapsulated by OSGi events in a separate thread.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class ThreadedEventHandler implements Closeable {

    /** The event subscribers indexed by the event type. */
    // Use a concurrent hash map because the map is written and read by different threads!
    private final Map<String, Set<EventSubscriber>> typedEventSubscribers = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(ThreadedEventHandler.class);

    private final Thread thread;

    private final Event notifyEvent = new Event("notify", Collections.emptyMap());
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Create a new threaded event handler.
     *
     * @param typedEventFactories the event factories indexed by the event type
     * @param safeCaller the safe caller to use
     */
    public ThreadedEventHandler(final Map<String, EventFactory> typedEventFactories, final SafeCaller safeCaller) {
        thread = new Thread(() -> {
            final EventHandler worker = new EventHandler(typedEventSubscribers, typedEventFactories, safeCaller);
            while (running.get()) {
                try {
                    final Event event = queue.poll(1, TimeUnit.HOURS);
                    if (event == null) {
                        logger.debug("Hey, you have really very few events.");
                    } else if (event == notifyEvent) {
                        // received an internal notification
                    } else {
                        worker.handleEvent(event);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ex) {
                    logger.error("Error on event handling.", ex);
                }
            }
        }, "ESH-OSGiEventManager");
        thread.start();
    }

    public void addEventSubscriber(final EventSubscriber eventSubscriber) {
        final Set<String> subscribedEventTypes = eventSubscriber.getSubscribedEventTypes();
        for (final String subscribedEventType : subscribedEventTypes) {
            final Set<EventSubscriber> entries = typedEventSubscribers.get(subscribedEventType);
            if (entries == null) {
                // Use a copy on write array set because the set is written and read by different threads!
                typedEventSubscribers.put(subscribedEventType,
                        new CopyOnWriteArraySet<>(Collections.singleton(eventSubscriber)));
            } else {
                entries.add(eventSubscriber);
            }
        }
    }

    public void removeEventSubscriber(EventSubscriber eventSubscriber) {
        final Set<String> subscribedEventTypes = eventSubscriber.getSubscribedEventTypes();
        for (final String subscribedEventType : subscribedEventTypes) {
            final Set<EventSubscriber> entries = typedEventSubscribers.get(subscribedEventType);
            if (entries != null) {
                entries.remove(eventSubscriber);
                if (entries.isEmpty()) {
                    typedEventSubscribers.remove(subscribedEventType);
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        queue.add(notifyEvent);
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void handleEvent(Event event) {
        queue.add(event);
    }
}
