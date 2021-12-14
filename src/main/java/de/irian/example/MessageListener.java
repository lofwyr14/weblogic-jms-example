package de.irian.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicLong;

public class MessageListener implements javax.jms.MessageListener {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final AtomicLong counter = new AtomicLong();

  private final JmsSender jmsSender;

  public MessageListener(final JmsSender jmsSender) {
    this.jmsSender = jmsSender;
    LOG.info("***********************************");
    LOG.info("AlphaMessageListener will be initialized.");
  }

  @PreDestroy
  public void logCounter() {
    LOG.info("Received {} messages in sum.", counter.get());
  }

  public void onMessage(Message message) {
    try {
      LOG.info("Receiving...");
      String content = ((TextMessage) message).getText();
      counter.getAndIncrement();
      LOG.info("The {}-th message will be processed. Content: '{}'", counter.get(), content);

    } catch (Exception exception) {
      LOG.error("Error", exception);
      throw new RuntimeException("Failed!", exception);
    }
  }
}
