/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static org.mule.runtime.core.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.message.CompatibilityMessage;
import org.mule.compatibility.core.session.SerializeAndEncodeSessionHandler;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.message.SessionHandler;
import org.mule.runtime.core.transaction.TransactionCoordination;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.resource.spi.work.Work;


/**
 * A base Worker used by Transport {@link org.mule.compatibility.core.api.transport.MessageReceiver} implementations.
 */
public abstract class AbstractReceiverWorker implements Work {

  protected List<Object> messages;
  protected InboundEndpoint endpoint;
  protected AbstractMessageReceiver receiver;
  protected OutputStream out;

  public AbstractReceiverWorker(List<Object> messages, AbstractMessageReceiver receiver) {
    this(messages, receiver, null);
  }

  public AbstractReceiverWorker(List<Object> messages, AbstractMessageReceiver receiver, OutputStream out) {
    this.messages = messages;
    this.receiver = receiver;
    this.endpoint = receiver.getEndpoint();
    this.out = out;
  }

  /**
   * This will run the receiver logic and call {@link #release()} once {@link #doRun()} completes.
   *
   */
  @Override
  public final void run() {
    doRun();
    release();
  }

  protected void doRun() {
    try {
      processMessages();
    } catch (MessagingException e) {
      // already managed by TransactionTemplate
    } catch (Exception e) {
      endpoint.getMuleContext().getExceptionListener().handleException(e);
    }
  }

  /**
   * The actual logic used to receive messages from the underlying transport. The default implementation will execute the
   * processing of messages within a TransactionTemplate. This template will manage the transaction lifecycle for the list of
   * messages associated with this receiver worker.
   */
  public void processMessages() throws Exception {
    // No need to do error handling. It will be done by inner TransactionTemplate per Message
    ExecutionTemplate<List<Event>> executionTemplate =
        createTransactionalExecutionTemplate(receiver.getEndpoint().getMuleContext(), endpoint.getTransactionConfig());

    // Receive messages and process them in a single transaction
    // Do not enable threading here, but serveral workers
    // may have been started
    ExecutionCallback<List<Event>> processingCallback = () -> {
      final Transaction tx = TransactionCoordination.getInstance().getTransaction();
      if (tx != null) {
        bindTransaction(tx);
      }
      List<Event> results = new ArrayList<>(messages.size());

      for (final Object payload : messages) {
        ExecutionTemplate<Event> perMessageExecutionTemplate =
            createMainExecutionTemplate(endpoint.getMuleContext(), receiver.flowConstruct,
                                        receiver.flowConstruct.getExceptionListener());
        Event resultEvent;
        try {
          resultEvent = perMessageExecutionTemplate.execute(() -> {
            Object preProcessedPayload = preProcessMessage(payload);
            if (preProcessedPayload != null) {
              CompatibilityMessage muleMessage = receiver.createMuleMessage(preProcessedPayload, endpoint.getEncoding());
              muleMessage = preRouteMuleMessage(muleMessage);
              // TODO Move getSessionHandler() to the Connector interface
              SessionHandler handler;
              if (endpoint.getConnector() instanceof AbstractConnector) {
                handler = ((AbstractConnector) endpoint.getConnector()).getSessionHandler();
              } else {
                handler = new SerializeAndEncodeSessionHandler();
              }
              MuleSession session = handler.retrieveSessionInfoFromMessage(muleMessage, endpoint.getMuleContext());

              Event resultEvent1;
              if (session != null) {
                resultEvent1 = receiver.routeMessage(muleMessage, session, tx, out);
              } else {
                resultEvent1 = receiver.routeMessage(muleMessage, tx, out);
              }
              return resultEvent1;
            } else {
              return null;
            }
          });
        } catch (MessagingException e) {
          if (e.getEvent().getError().isPresent()) {
            throw e;
          }
          resultEvent = e.getEvent();
        }

        if (resultEvent != null) {
          results.add(resultEvent);
        }
      }
      return results;
    };

    ClassLoader originalCl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(endpoint.getMuleContext().getExecutionClassLoader());

      List<Event> results = executionTemplate.execute(processingCallback);
      handleResults(handleEventResults(results));
    } finally {
      messages.clear();
      setCurrentEvent(null);
      currentThread().setContextClassLoader(originalCl);
    }
  }

  protected List<Object> handleEventResults(List<Event> events) throws Exception {
    List<Object> payloads = new ArrayList<>(events.size());
    for (Event muleEvent : events) {
      InternalMessage result = muleEvent == null ? null : muleEvent.getMessage();
      if (result != null) {
        Object payload = postProcessMessage(result);
        if (payload != null) {
          payloads.add(payload);
        }
      }
    }
    return payloads;
  }

  /**
   * This callback is called before a message is routed into Mule and can be used by the worker to set connection specific
   * properties to message before it gets routed
   *
   * @param message the next message to be processed
   * @throws Exception
   */
  protected CompatibilityMessage preRouteMuleMessage(CompatibilityMessage message) throws Exception {
    // no op
    return message;
  }

  /**
   * Template method used to bind the resources of this receiver to the transaction. Only transactional transports need implment
   * this method
   * 
   * @param tx the current transaction or null if there is no transaction
   * @throws TransactionException
   */
  protected abstract void bindTransaction(Transaction tx) throws TransactionException;

  /**
   * When Mule has finished processing the current messages, there may be zero or more messages to process by the receiver if
   * request/response messaging is being used. The result(s) should be passed back to the callee.
   * 
   * @param messages a list of messages. This argument will not be null
   * @throws Exception
   */
  protected void handleResults(List messages) throws Exception {
    // no op
  }

  /**
   * Before a message is passed into Mule this callback is called and can be used by the worker to inspect the message before it
   * gets sent to Mule
   * 
   * @param message the next message to be processed
   * @return the message to be processed. If Null is returned the message will not get processed.
   * @throws Exception
   */
  protected Object preProcessMessage(Object message) throws Exception {
    // no op
    return message;
  }

  /**
   * If a result is returned back this method will get called before the message is added to te list of results (these are later
   * passed to {@link #handleResults(java.util.List)})
   * 
   * @param message the result message, this will never be null
   * @return the message to add to the list of results. If null is returned nothing is added to the list of results
   * @throws Exception
   */
  protected InternalMessage postProcessMessage(InternalMessage message) throws Exception {
    // no op
    return message;
  }


  /**
   * This method is called once this worker is no longer required. Any resources *only* associated with this worker should be
   * cleaned up here.
   */
  @Override
  public void release() {
    // no op
  }
}
