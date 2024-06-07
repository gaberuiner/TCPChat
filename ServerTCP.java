import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerTCP{
    ArrayList<ClientTCP> clientTCPS;
    ServerSocket serverSocket;
    ArrayList<String> msgHistory = new ArrayList<>();

    public ServerTCP() throws IOException {
        clientTCPS = new ArrayList<>();
        serverSocket = new ServerSocket(8189);
    }
    public void sendMsgAll(String msg, String name){
        for (ClientTCP clientTCP : clientTCPS){

            if (clientTCP.getNickName().equals(name)){
                continue;
            }
            clientTCP.getOut().println(msg);
            clientTCP.getOut().flush();
        }
        msgHistory.add(msg);
        System.out.println(msg);
    }

    public void sendMessagePrivate(String msg, String name, String whom){
        for (ClientTCP clientTCP : clientTCPS){
            if (clientTCP.getNickName().equals(whom)){
                clientTCP.getOut().println("private msg from " + name + ": " + msg);
                System.out.println("private msg from " + name + " to " + whom + ": " + msg);
            }
        }
    }

    public void addClient() throws IOException {
        Socket socket = serverSocket.accept();

        ClientTCP clientTCP = new ClientTCP(socket);
        boolean isCorrectName = false;
        while (!isCorrectName){
            isCorrectName = nameIsCorrect(clientTCP);
        }
        clientTCPS.add(clientTCP);
        System.out.println(clientTCPS.get(0));

    }
    public void getAllClients(){
        System.out.println("All clients:");
        for (ClientTCP clientTCP : clientTCPS){
            System.out.println(clientTCP.getNickName());
        }
    }

    public void deleteClientByName(String name) throws IOException {
        boolean isFound = false;
        for (ClientTCP clientTCP : clientTCPS) {
            if (clientTCP.getNickName().equals(name)) {
                clientTCP.getSocket().close();
                clientTCPS.remove(clientTCP);
                sendMsgAll("User " + clientTCP.getNickName() + " has been kicked", "admin");
                isFound = true;
                break;
            }
        }
        if (!isFound) {
            System.out.println("No user with that name: " + name);
        }
    }

    public boolean nameIsCorrect(ClientTCP clientTCP) throws IOException {
        clientTCP.getOut().println("Write nickname: ");
        String name = String.valueOf(clientTCP.getIn().readLine());
        if (name.equals("")){
            clientTCP.getOut().println("name must containt at least 1 character");
            clientTCP.getOut().flush();
            return false;
        }
        if (clientTCPS.size() == 0){
            clientTCP.setNickName(name);
            return true;
        }
        for (ClientTCP tcp : clientTCPS) {
            if (tcp.getNickName() == null) {
                continue;
            }
            if (tcp.getNickName().equals(name)) {
                clientTCP.getOut().println("name is used try another one");
                clientTCP.getOut().flush();
                return false;
            }
        }

        clientTCP.setNickName(name);
        return true;
    }
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        ServerTCP serverTCP = new ServerTCP();
        System.out.println("сервер запущен на порту 8189...");
        AtomicInteger pos = new AtomicInteger();
        new Thread(() -> {
            System.out.println("admin thread started");
            System.out.println("You have commands /getAllUsers and /kickUserByName");
            while (!serverTCP.serverSocket.isClosed()){
                String command = scanner.nextLine();
                switch (command){
                    case "/getAllUsers":
                        serverTCP.getAllClients();
                        break;
                    case "/kickUserByName":

                        String name = scanner.nextLine();

                        try {
                            serverTCP.deleteClientByName(name);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    default:
                        System.out.println("no such command");

                }

            }
        }).start();
        while (!serverTCP.serverSocket.isClosed()){
            serverTCP.addClient();
            System.out.println("new client has connected");
            ClientTCP clientTCP = serverTCP.clientTCPS.get(pos.get());
            if (!serverTCP.msgHistory.isEmpty()){
                for (String msg : serverTCP.msgHistory){
                    clientTCP.getOut().println(msg);
                }
            }

            new Thread(() ->{
                System.out.println("new thread");

                try{
                    while (clientTCP.getSocket().isConnected()){
                        String message = clientTCP.getIn().readLine();
                        if (message == null) {
                            // Если сообщение null, это значит, что поток ввода закрыт
                            break;
                        }
                        switch (message){
                            case "/privateMessage":
                                clientTCP.getOut().println("type the user name: ");
                                String reciver = clientTCP.getIn().readLine();
                                boolean isFound = false;
                                for (ClientTCP clientTCP1 : serverTCP.clientTCPS){
                                    if (clientTCP1.getNickName().equals(reciver)){
                                        isFound = true;
                                        clientTCP.getOut().println("type message: ");
                                        String privateMsg = clientTCP.getIn().readLine();
                                        serverTCP.sendMessagePrivate(privateMsg, clientTCP.getNickName(), reciver);
                                    }
                                }
                                if (!isFound){
                                    clientTCP.getOut().println("user with name " + reciver + " not found");
                                }
                                break;
                            case "/whoIam":
                                clientTCP.getOut().println(clientTCP.getNickName());
                                break;
                            default:
                                serverTCP.sendMsgAll(clientTCP.getNickName() + ": " + message, clientTCP.getNickName());
                        }

                    }
                } catch (IOException e) {
                    clientTCP.getOut().println("error");
                    //throw new RuntimeException(e);
                } finally {
                    serverTCP.sendMsgAll(clientTCP.getNickName() + " has been disconnected", "server");
                    try {
                        clientTCP.getSocket().close();  // Закрытие сокета
                        serverTCP.clientTCPS.remove(clientTCP);
                    } catch (IOException e) {
                        System.out.println("Error closing socket: " + e.getMessage());
                    }
                    pos.getAndDecrement();

                }
            }).start();
            pos.getAndIncrement();



        }

    }
}
