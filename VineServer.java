import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class VineServer {
	private static int port = 8080;
	public static void main(String[] args) {
		ConcurrentHashMap clientScores = new ConcurrentHashMap();
		try {
			ServerSocket listenSocket = new ServerSocket(port);
			while(true) {

				// Socket connectionSocket = listenSocket.accept();
				ClientServer server = new ClientServer(connectionSocket);
				server.start();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
// Quiz class. Contains all the information about the current quiz. Including the path to the image,
// the path to the vine clip, the correct answer, and three incorrect answers.
// We need to figure out a process of randomly selecting three incorrect answers and ordering the four answers
// randomly.

class Quiz {
	// This hashmap holds all of the vine info. The keys are the name of the vine and the value is an
	// array of two Strings. The first element is the image path and the second is the video path.

	HashMap<String, String[]> vines = new HashMap<String, String[]>();
	private static prevIndex = -1;

	private String[] answers;
	private int correctIndex;
	private String imagePath;
	private String videoPath;
	public Quiz(String[] answers, int in, String iPath, String vPath) {
		this.answers = answers;
		this.correctIndex = in;
		this.imagePath = iPath;
		this.videoPath = vPath;
	}
	// Getter methods
	public int getCorrect() {
		return correctIndex;
	}
	public String[] getAnswers() {
		return answers();
	}
	pubic String getImagePath() {
		return imagePath;
	}
	public String getVideoPath() {
		return videoPath;
	}

	// Calculate score
	public int calcScore(int answeridx) {
		if(answeridx == correctIndex) {
			return 10;
		}
		else {
			return 0;
		}
	}

	// static method for create a new Quiz
	// probably could clean up a bit
	public static Quiz newQuiz() {
		// pick a random vine for the quiz that isn't the same as the previous one
		Random random = new Random();
		int randX = random.nextInt(vines.size());
		while(randX == prevIndex) {
			randX = random.nextInt(vines.size());
		}
		// pick random incorrect answers for the quiz
		int[] randAns = {randx, -1,-1,-1};
		for(int i = 1; i < randAns.length; i++) {
			int rand = random.nextInt(vines.size());
			// make sure there are no repeating answers
			while(rand == randX && rand == randAns[1] && rand == randAn[2] && rand == randAn[3]) {
				int rand = random.nextInt(vines.size());
			}
			ranAns[i] = rand;
		}
		// randomly order the answers
		int correctIdx = -1;
		int[] answers = {-1,-1,-1,-1}
		for(int i = 0; i < answers.length; i++) {
			int rand = new random.nextInt(vines.size());
			while(answers[rand] != -1) {
				int rand = random.nextInt(vines.size());
			}
			if(randAns[i] == randX) {
				correctIdx = rand;
			}
			answers[rand] = randAns[i];
		}
		// fill an array of strings with the possible answers to the quiz.
		String[] answerStrings = new String[answers.length];
		List<String> vineList = new ArrayList<String>(vines.keySet());
		for(int i = 0; i < answers.length; i++) {
			answerStrings[i] = vineList.get(answers[i]);
		}
		String correctAns = vineList.get(randX);
		// get the image and video paths for the correct answer
		String[] paths = vines.get(correctAns);
		// return a new Quiz object contain the possible answers, the correct answer's index, and the paths
		return new Quiz(answerStrings, correctIdx, paths[0], paths[1]);
	}
}

// a client class that contains all information about a connected user
class Client {
	private int id;
	private int score = 0;
	private InetAddress address;
	private int port;
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
class ClientServer implements Runnable {
	private Thread t;
	// Static Quiz that will be updated by our main loop and sent to the client
	private static Quiz currentQuiz = null;
	private static int currentID = 0;
	private String threadName;
	private Client client;
	private Socket socket;
	public ClientServer(Socket connection) {
		InetAddress address = connection.getInetAddress();
		int port = connection.getPort();
		int id = getNewID();
		this.threadName = String.valueOf(id);
		this.client = new Client(address, port , id);
		this.socket = connection;
	}
	// The run method will send the quiz info to the client.
	// We will also receive which answer the client choose, determine if the answer was correct, and give
	// an score accordingly.
	public void run() {
		try {
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getOutputStream()));
			Quiz prevQuiz = null;
			// We should probably add some kind of exit to this loop
			while(true) {
				String request = inFromClient.readLine();
				String[] requestWords = request.split(" ");
				if(requestWords.length == 3) {
					if(requestWords[0].equals("GET") && requestWords[1].equals("/")) {
						String page = getPage("index.html");
						outToClient.writeBytes(page);
					}
				}
				if(currentQuiz != prevQuiz) {
					// Send the quiz info to the client using xmlhttprequest
					sendGame();
					int answer = waitAndReceive(inFromClient);
					// Receive client's answer to the quiz and update score
					// Need to get variable string answer from client
					score = currentQuiz.calcScore(answer);
					client.addScore(score);
					prevQuiz = currentQuiz;
				}
			}
		} catch(IOException e) {
			e.printTraceStack();
		}
	}
	public void start() {
		if(t == null) {
			t = new Thread(this, threadName);
			t.start();
		}
	}
	// Wait to receive the client's answer to the Quiz. While we wait, check for page requests, ya know.
	public int waitAndReceive(BufferedReader inFromClient) {
		boolean drLupo = true;
		while(drLupo) {
			String request = inFromClient.readLine();
			String[] requestWords = request.split(" ");
			if(requestWords.length == 3) {
				if(requestWords[0].equals("GET") && requestWords[1].equals("/")) {
					String page = getPage("index.html");
					outToClient.writeBytes(page);
				}
				if(requestWords[0].equals("POST") && requestWords[1].equals("/quiz")) {
					// Code for parsing the POST request
					// Should be in x-www-form-urlencoded format
					boolean noBlank = true;
					String line = "";
					int contentLength = 0;
					while(noBlank) {
						line = inFromClient.readLine();
						if(s.toLowerCase().contains("Content-length"))
							contentLength = Integer.valueOf(s.split[1]);
						if(s.equals(""))
							noBlank = false;
					}
					char[] body = new char[contentLength];
					String body = String.valueOf(body);
					int answer = Integer.valueOf(body.split("=")[1]);
					return answer;
				}
			}
		}
	}
	public static int getNewID() {
		return currentID++;
	}
}
