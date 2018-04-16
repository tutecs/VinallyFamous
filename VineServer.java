import java.io.*;
import java.net.*;
import java.util.*;

public class VineServer {
	private static int port = 8080;
	public static void main(String[] args) {
		ConcurrentHashMap clientScores = new ConcurrentHashMap();
		try {
			ServerSocket listenSocket = new ServerSocket(port);
			while(true) {
				Socket connectionSocket = listenSocket.accept();
				ClientServer server = new ClientServer(connectionSocket);
				server.start();

			}
		}
	}
}
// a client class that contains all information about a connected user
public class Client {
	private int id;
	private int score = 0;
	private InetAddress address;
	private int outPort;
	public Client(InetAddress address, int port, int id) {
		this.address = address;
		this.port = port;
		this.id = id;
	}
	// Getter methods for score, address and port
	public int getScore() {
		return score;
	}
	public InetAddress getAddress() {
		return address;
	}
	public int getPort() {
		return port;
	}
	public int getID() {
		return id;
	}
	// Score increment method
	public void addScore(int value) {
		score += value;
	}
}

// Thread class for each connected client
public class ClientServer implements Runnable {
	private Thread t;
	public static String currentMessage = "";
	private static int currentID = 0;
	private Client client;
	private Socket socket;
	public ClientServer(Socket connection) {
		InetAddress address = connection.getInetAddress();
		int port = connection.getPort();
		int id = getNewID();
		this.client = new Client(address, port , id);
		this.socket = connection;
	}
	public void run() {
		String prevMessage = "";
		while(true) {
			if(!prevMessage.equals(currentMessage)) {
				System.out.println(currentMessage);
			}
			prevMessage = currentMessage;
		}
	}
	public void start() {
		if(t == null) {
			t = new Thread(this, String.valueOf(currentID));
			t.start();
		}
	}
	// static id methods
	public static int getNewID() {
		return currentID++;
	}
}
