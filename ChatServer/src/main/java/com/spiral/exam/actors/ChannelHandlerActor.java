package com.spiral.exam.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelHandlerActor extends AbstractActor {
    Map<String, Channel> channelList = new HashMap<>();

    public static class Channel {
        String channelName;
        String chatLog = "";
        List<ClientHandlerActor.Client> clientList = new ArrayList<>();

        public Channel(String name) {
            channelName = name;
        }

        private void messageClient(ClientHandlerActor.Client client, String message) {
            client.sendMessageToClient(message);
        }

        private void broadcast(String message) {
            clientList.forEach((client)->{messageClient(client, message);});
        }

        public void chatlogAppend(String message) {
            chatLog = chatLog+"\n"+message;
        }

        public String getChatLog() {
            return chatLog;
        }
    }

    public void addChannel(Channel channel) {
        System.out.println("Adding channel: "+channel.channelName);
        channelList.put (channel.channelName.toUpperCase(), channel);
    }

    public void addClientToChannel(ClientHandlerActor.Client client) {
        Channel channel = channelList.get(client.channelName.toUpperCase());

        String msg = client.nickname+" has entered the "+channel.channelName+" channel.";
        channel.broadcast(msg);
        channel.chatlogAppend(msg);

        channel.clientList.add(client);

        client.sendMessageToClient(channel.getChatLog());
    }

    public void receiveChat(ClientHandlerActor.Client.Message message) {
        String chatMessage = "";
        String channelName = "";

        if(message.message.equalsIgnoreCase("quit")) {
            chatMessage = message.clientName.toUpperCase()+" has disconnected from the server.";
        }
        else {
            chatMessage = message.clientName.toUpperCase()+": "+message.message;
        }
        channelName = message.channelName.toUpperCase();

        channelList.get(channelName).chatlogAppend(chatMessage);
        channelList.get(channelName).broadcast(chatMessage);
    }

    public Receive createReceive() {
        final Receive receive = receiveBuilder()
                .match(Channel.class, this::addChannel)
                .match(ClientHandlerActor.Client.class, this::addClientToChannel)
                .match(ClientHandlerActor.Client.Message.class, this::receiveChat)
                .build();

        return receive;
    }
    public static Props props() {
        return Props.create(ChannelHandlerActor.class);
    }
}
