package forklift.producers;

import forklift.connectors.ForkliftMessage;
import forklift.message.Header;
import forklift.producers.ForkliftProducerI;
import forklift.producers.ProducerException;

import com.google.common.base.Strings;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class ActiveMQProducer implements ForkliftProducerI {

    private MessageProducer producer;
    private Destination destination;
    private Map<Header, String> headers;
    private Map<String, Object> properties;
    private Session session;

    public ActiveMQProducer(MessageProducer producer, Session session) {
        this.producer = producer;
        this.session = session;
    }

    @Override
    public String send(String message) throws ProducerException {
        try {
            return send(new ForkliftMessage(message));
        } catch (Exception e) {
            throw new ProducerException("Failed to send message", e);
        }
    }

    @Override
    public String send(ForkliftMessage message) throws ProducerException {
        try {
            Message msg = prepAndValidate(message);
            if (msg != null) {
                producer.send(msg);
                return msg.getJMSCorrelationID();
            } else {
                throw new ProducerException("Forklift MSG is null");
            }
        } catch (Exception e) {
            throw new ProducerException("Failed to send message", e);
        }
    }

    @Override
    public String send(Map<Header, String> headers, 
                       Map<String, Object> properties,
                       ForkliftMessage message) throws ProducerException {
        Message msg = prepAndValidate(message);
        try {
            setMessageHeaders(msg, headers);
        } catch (Exception e) {
            throw new ProducerException("Failed to set message header", e);
        }
        try {
            setMessageProperties(msg, properties);
        } catch (Exception e) {
            throw new ProducerException("Failed to set message property", e);
        }
        try {
            producer.send(msg);
            return msg.getJMSCorrelationID();
        } catch (Exception e) {
            throw new ProducerException("Failed to send message", e);
        }
    }

    /**
    * validate message and prepare for sending.
    * @param msg - ForkliftMessage to be checked
    * @return - jms message ready to be sent to endpoint
    **/
    private Message prepAndValidate(ForkliftMessage message) throws ProducerException {
        Message msg = forkliftToJms(message);
        // check the ID
        try {
            if (Strings.isNullOrEmpty(msg.getJMSCorrelationID())) {
                msg.setJMSCorrelationID(UUID.randomUUID().toString().replaceAll("-", ""));
            }
        } catch (Exception e) {
            throw new ProducerException("Failed to set setJMSCorrelationID");
        }

        return msg;
    }

    /**
    * Generate javax.jms.Message from a Forklift Message
    * @param message - Forklift message to be converted
    * @return - a new javax.jms.Message
    **/
    private Message forkliftToJms(ForkliftMessage message) throws ProducerException {
        
        try {
            Message msg = null;
            if (message.getJmsMsg() != null ) {
                try {
                    msg = message.getJmsMsg();
                } catch (Exception e) {
                    throw new ProducerException("Error assigning Forklift JMS message to new MSG", e);
                }
            } else if (message.getMsg() != null ) {
                try {
                    msg = session.createTextMessage(message.getMsg());
                } catch (Exception e) {
                    throw new ProducerException("Error creating new jms TextMessage", e);
                }
            }
            setMessageHeaders(msg, message.getHeaders());
            setMessageProperties(msg, message.getProperties());
            return msg;
        } catch (Exception e) {
            throw new ProducerException("Error creating JMS Message, Forklift message was null", e);
        }
    }

    private void setMessageHeaders(Message msg, Map<Header, String> headers) throws ProducerException {
        if (msg != null && headers != null) {
            for (Map.Entry<Header,String> header : headers.entrySet()) {
                Method method;
                try {
                    method = msg.getClass().getMethod("set" + header.getKey().getJmsMessage() , String.class);
                    method.invoke(msg, header.getValue());
                } catch (Exception e) {
                    throw new ProducerException("Failed to set message header", e);
                }
            }
        }
    }

    private void setMessageProperties(Message msg, Map<String, Object> properties) throws ProducerException {
        if (msg != null && properties != null) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                try {
                    msg.setObjectProperty(property.getKey(), property.getValue());
                } catch (Exception e) {
                    throw new ProducerException("Failed to set message properties", e);
                }
            }
        }
    }

    public int getDeliveryMode() throws ProducerException {
        try {
            return this.producer.getDeliveryMode();
        } catch (Exception e) {
            throw new ProducerException("Failed to get DelieveryMode");
        }
    }

    public void setDeliveryMode(int mode) throws ProducerException {
        try {
            this.producer.setDeliveryMode(mode);
        } catch (Exception e) {
            throw new ProducerException("Failed to set DelieveryMode");
        }
    }

    public long getTimeToLive() throws ProducerException {
        try {
            return this.producer.getTimeToLive();
        } catch (Exception e) {
            throw new ProducerException("Failed to get TimeToLive");
        }
    }

    public void setTimeToLive(long timeToLive) throws ProducerException {
        try {
            this.producer.setTimeToLive(timeToLive);
        } catch (Exception e) {
            throw new ProducerException("Failed to set TimeToLive");
        }   
    }

    @Override
    public Map<Header, String> getHeaders() throws ProducerException {
        try {
            return this.headers;
        } catch (Exception e) {
            throw new ProducerException("Failed to get headers");   
        }
        
    }

    @Override
    public void setHeaders(Map<Header, String> headers) throws ProducerException {
        try {
            this.headers = headers;
        } catch (Exception e) {
            throw new ProducerException("Failed to set headers");
        }
    }

    @Override
    public Map<String, Object> getProperties() throws ProducerException {
        try {
            return this.properties;
        } catch (Exception e) {
            throw new ProducerException("Failed to get properties");
        }
    }

    @Override
    public void setProperties(Map<String , Object> properties) throws ProducerException {
        try {
            this.properties = properties;
        } catch (Exception e) {
            throw new ProducerException("Failed to set properties");
        }
    }
}