package com.spiral.exam;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ClientApp extends JFrame{

    private JTextField textField;
    private JTextArea messageWindow;

    public ClientApp(String host) {
        super("SpiralChat Client");
        serverIP = host;
        textField = new JTextField();
        textField.setEditable(false);
        textField.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        if(isConnected) {
                            sendMessage(event.getActionCommand());
                        }
                        textField.setText("");
                    }
                }
        );
        add(textField, BorderLayout.SOUTH);
        textField.setEditable(true);
        messageWindow = new JTextArea();
        messageWindow.setEditable(false);
        add(new JScrollPane(messageWindow));
        setSize(300, 300);
        setVisible(true);
    }

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String message = "";
    private String serverIP;
    private Socket socket;
    boolean isConnected = false;

    public void runClient() {
        try {
            connectToServer();
            createStreams();
            receiveServerMessage();
        }
        catch(EOFException e) {
            printMessage("Shutting down client...");
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            closeConnection();
        }
    }

    private void connectToServer() throws IOException {
        printMessage("Connecting to server...");
        socket = new Socket(InetAddress.getByName(serverIP), 8787);
        isConnected = true;
    }

    private void createStreams() throws IOException {
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
    }

    private void receiveServerMessage() throws IOException {
        do {
            try {
                message = (String) input.readObject();
                printMessage(message);
            }
            catch(ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while(!message.equalsIgnoreCase("quit"));
    }

    private void closeConnection() {
        try {
            output.close();
            input.close();
            socket.close();

            System.exit(0);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message){
        try {
            output.writeObject(message);
            output.flush();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void printMessage(final String message) {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run(){
                    messageWindow.append(message+"\n");
                }
            }
        );
    }

    public static void main(String[] args) {
        ClientApp clientApp;
        clientApp = new ClientApp("localhost");
        clientApp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        clientApp.runClient();
    }
}