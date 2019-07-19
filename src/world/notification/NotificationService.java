package world.notification;

import client.Client;
import client.ClientRegistry;
import client.commands.DisconnectCommand;
import network.CommandExecutor;

import java.util.*;

public class NotificationService {
    private Collection<NotificationSubscriber> subscribers = new ArrayList<>();
    private Map<NotificationSubscriber, Long> lastTimestamps = new HashMap<>();

    private static final long TIMEOUT_INTERVAL = 1000 * 60 * 15; //1 minutes
    private ClientRegistry registry;
    private CommandExecutor executor;

    public NotificationService(ClientRegistry registry, CommandExecutor executor){
        this.registry = registry;
        this.executor = executor;
    }

    public void checkTimeouts(){
        long curTime = System.currentTimeMillis();

        List<NotificationSubscriber> toUnsubscribe = new ArrayList<>();
        for(NotificationSubscriber subscriber : subscribers){
            long lastPing = 0;
            if(lastTimestamps.containsKey(subscriber))
                lastPing = lastTimestamps.get(subscriber);
            if(lastPing + TIMEOUT_INTERVAL < curTime){
                subscriber.notify(new TimeoutNotification(registry));
                Client connectedClient = registry.getClientWithUsername(subscriber.getID());
                if(connectedClient != null){
                    connectedClient.tryLogOut(connectedClient, "");
                    toUnsubscribe.add(subscriber);
                    executor.scheduleCommand(new DisconnectCommand(connectedClient,registry));
                }
            }
        }
        for(NotificationSubscriber subscriber: toUnsubscribe) {
            unsubscribe(subscriber);
        }
    }

    public void notify(Notification notification, NotificationScope scope){
        Collection<NotificationSubscriber> toNotify = scope.filterSubscribers(subscribers);
        for(NotificationSubscriber subscriber : toNotify) {
            subscriber.notify(notification);
            lastTimestamps.put(subscriber,System.currentTimeMillis());
        }
    }

    public void subscribe(NotificationSubscriber subscriber){
        subscribers.add(subscriber);
        lastTimestamps.put(subscriber, System.currentTimeMillis());
    }

    public void unsubscribe(NotificationSubscriber subscriber){
        subscribers.remove(subscriber);
        lastTimestamps.remove(subscriber);
    }

    public void attachToExecutor(CommandExecutor executor){
        executor.scheduleCommand(new TimeoutCheckerCommand(this));
    }

    public class TimeoutNotification extends Notification{
        public TimeoutNotification(ClientRegistry registry) {
            super(registry);
        }

        @Override
        public String getAsMessage(NotificationSubscriber viewer) {
            return "You have been inactive for " + TIMEOUT_INTERVAL/1000/60 + " minutes and have been disconnected";
        }
    }

    private static class TimeoutCheckerCommand implements CommandExecutor.Command {
        private NotificationService service;
        private long nextCheck = 0;

        public TimeoutCheckerCommand(NotificationService service){
            this.service = service;
        }

        @Override
        public void execute() {
            service.checkTimeouts();
            nextCheck = System.currentTimeMillis() + 5000;
        }

        @Override
        public long getStartTimestamp() {
            return nextCheck;
        }

        @Override
        public boolean isComplete() {
            return false;
        }
    }
}
