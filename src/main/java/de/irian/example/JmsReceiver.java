package de.irian.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.invoke.MethodHandles;
import java.util.Properties;

@Configuration
@EnableJms
public class JmsReceiver {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${spring.wls.jms.url}")
  private String url;
  @Value("${spring.wls.jms.username}")
  private String username;
  @Value("${spring.wls.jms.password}")
  private String password;
  @Value("${spring.wls.jms.connectionFactory}")
  private String connectionFactoryName;
  @Value("${spring.wls.jms.queue}")
  private String queueName;

  private Properties getJNDiProperties() {
    final Properties jndiProps = new Properties();
    LOG.info("Initializing JNDI-Configuration.");
    LOG.info("The URL is {}. User: '{}'", url, username);
    jndiProps.setProperty(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
    jndiProps.setProperty(Context.PROVIDER_URL, url);
    jndiProps.setProperty(Context.SECURITY_PRINCIPAL, username);
    jndiProps.setProperty(Context.SECURITY_CREDENTIALS, password);
    return jndiProps;
  }

  @Bean
  public JndiTemplate jndiTemplate() {
    final JndiTemplate jndiTemplate = new JndiTemplate();
    jndiTemplate.setEnvironment(getJNDiProperties());
    return jndiTemplate;
  }

  @Bean(name = "jmsJndiConnectionFactory")
  public JndiObjectFactoryBean jndiObjectFactoryBean(final JndiTemplate jndiTemplate) {
    return getJndiObjectFactoryBean(jndiTemplate, connectionFactoryName);
  }

  @Primary
  @Bean(name = "jmsWlsConnectionFactory")
  public ConnectionFactory jmsConnectionFactory(
      @Qualifier("jmsJndiConnectionFactory") final JndiObjectFactoryBean jndiObjectFactoryBean) {
    return (ConnectionFactory) jndiObjectFactoryBean.getObject();
  }

  @Bean(name = "jndiQueue")
  public JndiObjectFactoryBean jndiQueue(final JndiTemplate jndiTemplate) {
    LOG.info("The Queue is '{}'.", queueName);
    return getJndiObjectFactoryBean(jndiTemplate, queueName);
  }

  @Bean(name = "alphaDestination")
  public Destination queue(@Qualifier("jndiQueue") final JndiObjectFactoryBean jndiObjectFactoryBean) {
    return (Destination) jndiObjectFactoryBean.getObject();
  }

  /**
   * Spring Utility für JMS Listener, übernimmt Dinge wie automatisches Reconnect, JMS Transaction Handling, Connection
   * Caching usw. Siehe auch {@link DefaultMessageListenerContainer}.
   */
  @Bean
  public DefaultMessageListenerContainer alphaMessageListenerContainer(
      @Qualifier("alphaMessageListener") final javax.jms.MessageListener messageListener,
      final ConnectionFactory connectionFactory,
      @Qualifier("alphaDestination") final Destination destination) {
    DefaultMessageListenerContainer listenerContainer = new DefaultMessageListenerContainer();
    listenerContainer.setMessageListener(messageListener);
    listenerContainer.setDestination(destination);
    listenerContainer.setConnectionFactory(connectionFactory);
    listenerContainer.setAcceptMessagesWhileStopping(false);
    listenerContainer.setSessionTransacted(true);
    listenerContainer.setConcurrentConsumers(1);
    listenerContainer.setMaxMessagesPerTask(1);
    listenerContainer.setReceiveTimeout(1000);
    listenerContainer.setErrorHandler(throwable -> LOG.error("", throwable));
    LOG.info("The message listener container was started for queue '{}'.", listenerContainer.getDestination());
    return listenerContainer;
  }

  @Bean("alphaMessageListener")
  public javax.jms.MessageListener alphaMessageListener(final JmsSender jmsSender) {
    return new MessageListener(jmsSender);
  }

  private JndiObjectFactoryBean getJndiObjectFactoryBean(JndiTemplate jndiTemplate, String jndiName) {
    final JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
    jndiObjectFactoryBean.setJndiTemplate(jndiTemplate);
    jndiObjectFactoryBean.setJndiName(jndiName);
    return jndiObjectFactoryBean;
  }

 @Bean
  public InitialContext webLogicInitialContextExt() {
    try {
      return new InitialContext(getJNDiProperties());
    } catch (NamingException e) {
      LOG.error("Error creating InitialContext", e);
      throw new RuntimeException(e);
    }
  }
}
