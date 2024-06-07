import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerTCP{
    ArrayList<ClientTCP> clientTCPS;
    ServerSocket serverSocket;

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
        System.out.println(msg);
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
        for (ClientTCP clientTCP : clientTCPS){
            if (clientTCP.getNickName().equals(name)){
                clientTCP.getSocket().close();
                sendMsgAll("User " + clientTCP.getNickName() + " has been kicked", "admin");

            }
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
                    case "/kickUserByName":
                        scanner.nextLine();
                        String name = scanner.nextLine();
                        Boolean isFound = false;
                        for (ClientTCP clientTCP : serverTCP.clientTCPS){
                            if (clientTCP.getNickName().equals(name)){
                                try {
                                    serverTCP.sendMsgAll("User " + clientTCP.getNickName() + " has been kicked", "admin");
                                    clientTCP.getSocket().close();
                                    isFound = true;
                                    break;
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                        if (!isFound){
                            System.out.println("no user with that name: " + name);
                        }
                    default:
                        System.out.println("no such command");

                }

            }
        }).start();
        while (!serverTCP.serverSocket.isClosed()){
            serverTCP.addClient();
            System.out.println("new client has connected");
            ClientTCP clientTCP = serverTCP.clientTCPS.get(pos.get());

            new Thread(() ->{
                System.out.println("new thread");
                try{
                    while (clientTCP.getSocket().isConnected()){
                        String message = clientTCP.getIn().readLine();
                        if (message == null) {
                            // Если сообщение null, это значит, что поток ввода закрыт
                            break;
                        }
                        serverTCP.sendMsgAll(clientTCP.getNickName() + ": " + message, clientTCP.getNickName());
                    }
                } catch (IOException e) {
                    clientTCP.getOut().println("error");
                    //throw new RuntimeException(e);
                } finally {
                    serverTCP.sendMsgAll(clientTCP.getNickName() + " has been disconnected", "server");
                    try {
                        clientTCP.getSocket().close();  // Закрытие сокета
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
