package de.irian.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@Service
@EnableScheduling
public class JmsSender {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${spring.wls.jms.connectionFactory}")
  private String connectionFactoryName;
  @Value("${spring.wls.jms.queue}")
  private String queueName;
  @Value("${spring.max}")
  private Integer max;

  @Resource(name = "webLogicInitialContextExt")
  public Context context;

  private static final AtomicLong counter = new AtomicLong();

  @Scheduled(fixedDelay = 5_000)
  public void sendIntoJMSQueue() throws Exception {

    if (counter.get() >= max) {
      LOG.info("Exit after maximum calls {}", max);
      System.exit(0);
    }

    LOG.info("Called by timer!");
    String content = "This message is message number #" + counter.getAndIncrement() + " created at " + new Date() + "!";
    LOG.info("New created message content = '{}'", content);
    send(content);
  }

  public void send(final String content) throws Exception {
    initSendClose(content, queueName);
  }

  protected void initSendClose(final String content, final String queueName) throws Exception {
    LOG.info("Initializing for queue name {}...", queueName);
    final QueueConnectionFactory qconFactory = (QueueConnectionFactory) context.lookup(connectionFactoryName);
    try (final QueueConnection qcon = qconFactory.createQueueConnection();
         final QueueSession qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
         final QueueSender qsender = qsession.createSender((Queue) context.lookup(queueName))) {
      qcon.start();
      final TextMessage message = qsession.createTextMessage();
      message.setText(content);
      qsender.send(message);
    }
  }
}
