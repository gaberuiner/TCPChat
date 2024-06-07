import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientTCP{


    private Socket socket;
    private String nickName;

    private OutputStream outputStream;

    private InputStream inputStream;

    private BufferedReader in;

    private  PrintWriter out;

    private Scanner scanner;


    public ClientTCP(Socket socket) throws IOException {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        this.in = new BufferedReader(new InputStreamReader(this.inputStream));
        this.out = new PrintWriter(this.outputStream, true);
        this.scanner = new Scanner(System.in);
    }

    public String chooseNickName(){
        return "A";
    }



    public void sendMsg() throws IOException {
        String msg = scanner.nextLine();
        out.write(nickName + ": " +  msg + "\n");
    }

    public Socket getSocket() {
        return socket;
    }


    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public BufferedReader getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public String chooseName() throws IOException {
        out.write("choose your name");
        String chosenName = in.readLine();
        return chosenName;
    }

    public static void main(String[] args) throws IOException {
        Socket socket1 = new Socket("localhost", 8189);
        OutputStream outputStream1 = socket1.getOutputStream();
        InputStream inputStream1 = socket1.getInputStream();
        Scanner scanner1 = new Scanner(System.in);
        while (socket1.isConnected()){
            String msg = scanner1.nextLine();
            outputStream1.write(msg.getBytes());
        }
    }
}
