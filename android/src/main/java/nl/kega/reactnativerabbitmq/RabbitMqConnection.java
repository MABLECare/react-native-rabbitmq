package nl.kega.reactnativerabbitmq;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ShutdownListener; 
import com.rabbitmq.client.ShutdownSignalException; 

class RabbitMqConnection extends ReactContextBaseJavaModule {

    private ReactApplicationContext context;

    public ReadableMap config;

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    private Callback status;

    private ArrayList<RabbitMqQueue> queues = new ArrayList<RabbitMqQueue>();
    private ArrayList<RabbitMqExchange> exchanges = new ArrayList<RabbitMqExchange>(); 

    public RabbitMqConnection(ReactApplicationContext reactContext) {
        super(reactContext);

        this.context = reactContext;

    }

    @Override
    public String getName() {
        return "RabbitMqConnection";
    }
    
    @ReactMethod
    public void initialize(ReadableMap config) {
        this.config = config;
    }

    @ReactMethod
    public void status(Callback onStatus) {
        this.status = onStatus;
    }

    @ReactMethod
    public void connect() {

        this.factory = new ConnectionFactory();
        this.factory.setUsername(this.config.getString("username"));
        this.factory.setPassword(this.config.getString("password"));
        this.factory.setVirtualHost(this.config.getString("virtualhost"));
        this.factory.setHost(this.config.getString("host"));
        this.factory.setPort(this.config.getInt("port"));
        this.factory.setAutomaticRecoveryEnabled(true);

        try {
            
            this.connection = factory.newConnection();

            this.connection.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException cause) {
                    Log.e("RabbitMqConnection", "Shutdown signal received " + cause);
                    onClose(cause);
                }
            });

            this.channel = connection.createChannel();
            this.channel.basicQos(1);

        } catch (Exception e){

            Log.e("RabbitMqConnection", "Create channel error " + e);
            e.printStackTrace();

        } finally { 
            this.connection = null; 
        } 

    }

    @ReactMethod
    public void addQueue(ReadableMap queue_condig) {
        RabbitMqQueue queue = new RabbitMqQueue(this.context, this.channel, queue_condig);
        this.queues.add(queue);
    }

    @ReactMethod
    public void bindQueue(String exchange_name, String queue_name, String routing_key) {
        
        RabbitMqQueue found_queue = null;
        for (RabbitMqQueue queue : queues) {
		    if (Objects.equals(queue_name, queue.name)){
                found_queue = queue;
            }
        }

        RabbitMqExchange found_exchange = null;
        for (RabbitMqExchange exchange : exchanges) {
		    if (Objects.equals(exchange_name, exchange.name)){
                found_exchange = exchange;
            }
        }

        if (!found_queue.equals(null) && !found_exchange.equals(null)){
            found_queue.bind(found_exchange, routing_key);
        }
    }

    @ReactMethod
    public void unbindQueue(String exchange_name, String queue_name) {
        
        RabbitMqQueue found_queue = null;
        for (RabbitMqQueue queue : queues) {
		    if (Objects.equals(queue_name, queue.name)){
                found_queue = queue;
            }
        }

        RabbitMqExchange found_exchange = null;
        for (RabbitMqExchange exchange : exchanges) {
		    if (Objects.equals(exchange_name, exchange.name)){
                found_exchange = exchange;
            }
        }

        if (!found_queue.equals(null) && !found_exchange.equals(null)){
            found_queue.unbind();
        }
    }

    @ReactMethod
    public void removeQueue() {
      
    }
    /*
    @ReactMethod
    public void publishToQueue(String message, String exchange_name, String routing_key) {

        for (RabbitMqQueue queue : queues) {
		    if (Objects.equals(exchange_name, queue.exchange_name)){
                Log.e("RabbitMqConnection", "publish " + message);
                queue.publish(message, exchange_name);
                return;
            }
		}

    }
    */

    @ReactMethod
    public void addExchange(ReadableMap exchange_condig) {

        RabbitMqExchange exchange = new RabbitMqExchange(this.context, this.channel, exchange_condig);

        this.exchanges.add(exchange);
    }

    @ReactMethod
    public void publishToExchange(String message, String exchange_name, String routing_key) {

         for (RabbitMqExchange exchange : exchanges) {
		    if (Objects.equals(exchange_name, exchange.name)){
                Log.e("RabbitMqConnection", "Exchange publish: " + message);
                exchange.publish(message, routing_key);
                return;
            }
		}

    }

    @ReactMethod
    public void deleteExchange(String exchange_name, Boolean if_unused) {

        for (RabbitMqExchange exchange : exchanges) {
		    if (Objects.equals(exchange_name, exchange.name)){
                exchange.delete(if_unused);
                return;
            }
		}

    }

    @ReactMethod
    public void close() {
        try {
            this.connection.close();
         } catch (Exception e){
            Log.e("RabbitMqConnection", "Connection closing error " + e);
            e.printStackTrace();
        } finally { 
            this.connection = null; 
        } 
    }

    private void onClose(ShutdownSignalException cause) { 
        Log.e("RabbitMqConnection", "Closed");

        WritableMap event = Arguments.createMap();
        event.putString("name", "closed");

        this.context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("RabbitMqConnectionEvent", event);
    } 


}