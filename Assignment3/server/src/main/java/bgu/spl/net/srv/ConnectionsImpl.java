package bgu.spl.net.srv;

import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> clientAndHandler = new ConcurrentHashMap<>(); //map with client and it's handler
    private ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> channelsClientSubscribedTo = new ConcurrentHashMap<>(); //every client has a map with the channels it subscribed to
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer >> clientsSubscribedToChannel = new ConcurrentHashMap<>(); //every channel has a map with the clients that subscribed to this channel
    private ConcurrentHashMap<Integer, User> clients = new ConcurrentHashMap<>(); //map with clients id's and the user

    //private ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>(); //map with connectionId and it's user
    private LinkedList<User> users = new LinkedList<>(); //all users

    private static class singeltonConnections{
        private static ConnectionsImpl<String> instance = new ConnectionsImpl<>();
    }

    public static ConnectionsImpl<String> getInstance() {
        return singeltonConnections.instance;
    }

    //sends a message T to client represented by the given connectionId
    public boolean send(int connectionId, T msg){
        ConnectionHandler<T> handler = clientAndHandler.get(connectionId); //get connection handler of client
        if(handler == null) //if no such handler
           return false;
        else {
            handler.send(msg);
            return true;
        }
    }

    //Sends a message T to clients subscribed to channel
    public void send(String channel, T msg){
        ConcurrentHashMap<Integer, Integer> clientsinChannel = clientsSubscribedToChannel.get(channel.toLowerCase());
        Set<Integer> keys = clientsinChannel.keySet();
        for(Integer connectionId : keys){
             send(connectionId, msg);
        }
    }

    //Removes an active client connectionId from the map
    public void disconnect(int connectionId){
        clientAndHandler.remove(connectionId);
    }

    public LinkedList<User> getUsers(){
        if(!users.isEmpty())
           return users;
        return null;
    }

    public void addUser(User user) {
        users.addFirst(user);
    }

    //returns hash map of clients(=connection id, subscription id) that subscribed to a ceratin channel
    public ConcurrentHashMap<Integer, Integer> getClientsThatSubscribedToTheChannel(String channel) {
        if(!clientsSubscribedToChannel.containsKey(channel.toLowerCase()))
           return null;
        return clientsSubscribedToChannel.get(channel.toLowerCase());
    }

    //returns hash map of all of a clients subscriptions
    public ConcurrentHashMap<String, Integer> getClientsSubscribtions(Integer connectionId) {
        return channelsClientSubscribedTo.get(connectionId);
    }

    //returns the subscription id of a client that is subscribed to a certain topic
    public Integer getClientsSubscriptionId(String channel, Integer connectionId) {
        ConcurrentHashMap<Integer, Integer> clientsSubscribedToChannel =  getClientsThatSubscribedToTheChannel(channel.toLowerCase());
        if(clientsSubscribedToChannel == null)
            return null;
        return clientsSubscribedToChannel.get(connectionId); //returns subscription id of client
    } 

    //adds a channel to the hash map of channels that a client is subscribed to
    public void addChannelToClientsSubscription(Integer connectionId, String channel, Integer subcsriptionId) {
        ConcurrentHashMap<String, Integer> clientsSubscriptions = channelsClientSubscribedTo.get(connectionId);
        if(clientsSubscriptions != null) {
            //if(!clientsSubscriptions.contains(channel)) {
                clientsSubscriptions.put(channel.toLowerCase(), subcsriptionId);
           // }
        }
        else {
            clientsSubscriptions = new ConcurrentHashMap<>();
            clientsSubscriptions.put(channel.toLowerCase(), subcsriptionId);
            channelsClientSubscribedTo.put(connectionId, clientsSubscriptions);
        }
    }

    //adds a client to the channels hash map of clients that subscribed to it
    public void addClientToChannelsSubscribedClients(String channel, Integer connectionId, Integer subscriptionId){

        ConcurrentHashMap<Integer, Integer> channelsClients = getClientsThatSubscribedToTheChannel(channel.toLowerCase());
        if(channelsClients != null) {
            channelsClients.putIfAbsent(connectionId, subscriptionId); //TODO check if need to send error if already subscribed
        }
        else {
            channelsClients = new ConcurrentHashMap<>();
            channelsClients.put(connectionId, subscriptionId);
            clientsSubscribedToChannel.put(channel.toLowerCase(), channelsClients);
            if(channelsClientSubscribedTo.contains(connectionId)){ // if user is already in the list
                channelsClientSubscribedTo.get(connectionId).putIfAbsent(channel.toLowerCase(), subscriptionId);
            } else { // user not in the list, create client, create hashmap, add to hashmap.
                channelsClientSubscribedTo.putIfAbsent(connectionId, new ConcurrentHashMap<String, Integer>());
                channelsClientSubscribedTo.get(connectionId).putIfAbsent(channel.toLowerCase(), subscriptionId);
            }
        }

    }

    //removes a channel from a clients hash map of channels it subscribed to
    public void removeChannelFromClientsSubscription( Integer connectionId, String channel, Integer subcsriptionId){
        ConcurrentHashMap<String, Integer> clientsSubscriptions = channelsClientSubscribedTo.get(connectionId);
        clientsSubscriptions.remove(channel.toLowerCase(), subcsriptionId);
    }

    //removes a client from a channels hash map of clients that subscribed to it
    public void removeClientFromChannelsSubscribedClients(String channel, Integer connectionId, Integer subscriptionId){
        ConcurrentHashMap<Integer, Integer> channelsClients = getClientsThatSubscribedToTheChannel(channel.toLowerCase());
        channelsClients.remove(connectionId, subscriptionId);
    }

    //removes all of a clients subscriptions
    public void removeAllOfClientsSubscriptions(Integer connectionId){
        ConcurrentHashMap<String, Integer> clientsSubscriptions = channelsClientSubscribedTo.get(connectionId);
        clientsSubscriptions.clear();
    }

    //Unsubscribes client from all channels
    public void UnsubscribeClientFromAllChannels(Integer connectionId){
        Set<String> channels = clientsSubscribedToChannel.keySet(); //get all channels as set
        for(String channel : channels) { //each channel individually
            ConcurrentHashMap<Integer, Integer> channelsClients = getClientsThatSubscribedToTheChannel(channel.toLowerCase());
            if(channelsClients.contains(connectionId))
                channelsClients.remove(connectionId);
        }       
    }

    //get a user by it's id
    public User getUserByConnectionId(Integer connectionId){
        return clients.get(connectionId);
    }

    public void addClientToMap(Integer connectionId, User user){
        clients.put(connectionId, user);
    }

    //connects between a client and a handler
    public void connect(Integer connectionId, ConnectionHandler<T> handler) {
        clientAndHandler.put(connectionId, handler);
    }
}