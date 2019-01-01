/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 See AUTHORS file
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.mini2Dx.minibus.exchange;

import java.util.concurrent.atomic.AtomicBoolean;

import org.mini2Dx.minibus.MessageBus;
import org.mini2Dx.minibus.MessageData;
import org.mini2Dx.minibus.MessageExchange;
import org.mini2Dx.minibus.MessageHandler;
import org.mini2Dx.minibus.transmission.MessageTransmission;

/**
 * Processes {@link MessageData}s on its own {@link Thread}. This thread will
 * not consume CPU if no messages are available. The exchange/thread can be
 * stopped by calling {@link ConcurrentMessageExchange#dispose()}
 */
public class ConcurrentMessageExchange extends MessageExchange implements Runnable {
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ConcurrentMessageExchange(final MessageBus messageBus, final MessageHandler... messageHandlers) {
	super(messageBus, messageHandlers);
	new Thread(this).start();
    }

    @Override
    public void update(final float delta) {
    }

    @Override
    public boolean isImmediate() {
	return false;
    }

    @Override
    public void run() {
	while (this.running.get()) {
	    try {
		final MessageTransmission messageTransmission = this.messageQueue.take();
		if (messageTransmission.getSource() == null) {
		    return;
		}
		for (final MessageHandler messageHandler : this.messageHandlers) {
		    messageHandler.onMessageReceived(messageTransmission.getMessageType(),
			    messageTransmission.getSource(), this, messageTransmission.getMessage());
		}
		messageTransmission.release();
	    } catch (final InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    @Override
    public void dispose() {
	this.running.set(false);
	try {
	    // Use null to signal thread to stop
	    this.messageQueue.put(new MessageTransmission(null));
	} catch (final InterruptedException e) {
	    e.printStackTrace();
	}
	super.dispose();
    }
}