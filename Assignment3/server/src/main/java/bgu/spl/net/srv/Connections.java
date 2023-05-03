package bgu.spl.net.srv;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    void disconnect(int connectionId);

    LinkedList<User> getUsers();

    void addUser(User user);

    ConcurrentHashMap<Integer, Integer> getClientsThatSubscribedToTheChannel(String channel);

    ConcurrentHashMap<String, Integer> getClientsSubscribtions(Integer connectionId);

    Integer getClientsSubscriptionId(String channel, Integer connectionId);

    void addChannelToClientsSubscription( Integer connectionId, String channel, Integer subcsriptionId);

    void addClientToChannelsSubscribedClients(String channel, Integer connectionId, Integer subscriptionId);

    void removeChannelFromClientsSubscription( Integer connectionId, String channel, Integer subcsriptionId);

    void removeClientFromChannelsSubscribedClients(String channel, Integer connectionId, Integer subscriptionId);

    void removeAllOfClientsSubscriptions(Integer connectionId);
    
    void UnsubscribeClientFromAllChannels(Integer connectionId);

    User getUserByConnectionId(Integer connectionId);

    void addClientToMap(Integer connectionId, User user);

    void connect(Integer connectionId, ConnectionHandler<T> handler);
}
