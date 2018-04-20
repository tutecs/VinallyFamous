// VineQuiz server program.
// Ethan Duryea, Elliot Spicer, Joshua Weller, John Zamites
// CSCI 420: Networking, Project 2018, April 2018


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
	private String name;
	public Client(InetAddress address, int port, int id, String name) {
		this.address = address;
		this.port = port;
		this.id = id;
		this.name = name;
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
	public String getName() {
		return name;
	}
	// Score increment method
	public void addScore(int value) {
		score += value;
	}
}

// Thread class for each connected client
class ClientServer implements Runnable {
	// Static HashMap will be shared by every thread.
	// New user:scores pairs will be updated by the main loop
	// the scores will be updated by the threads
	private static Map<String,Integer> scores = new ConcurrentHashMap<String, Integer>();

	// Static Quiz that will be updated by our main loop and sent to the client
	private static Quiz currentQuiz = null;
	private static int currentID = 0;
	private Thread t;
	private String threadName;
	private Client client;
	private Socket socket;
	private volatile boolean active;
	public ClientServer(Socket connection) {
		InetAddress address = connection.getInetAddress();
		int port = connection.getPort();
		int id = getNewID();
		this.threadName = String.valueOf(id);
		this.client = new Client(address, port , id);
		this.socket = connection;
		this.active = true;
	}
	// The run method will send the quiz info to the client.
	// We will also receive which answer the client choose, determine if the answer was correct, and give
	// an score accordingly.
	public void run() {
		try {
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getOutputStream()));
			Quiz prevQuiz = null;
			// We should probably add some kind of exit to this loop
			// Like when the client disconnects
			while(active) {
				String request = inFromClient.readLine();
				if(request == null) {
					active = false;
					break;
				}
				String[] requestWords = request.split(" ");
				if(requestWords.length == 3) {
					if(requestWords[0].equals("GET") && requestWords[1].equals("/")) {
						String page = getPage("index.html");
						outToClient.writeBytes(page);
					}
				}
				if(currentQuiz != prevQuiz) {
					// Send the quiz info to the client using xmlhttprequest
					sendQuiz();
					int answer = waitAndReceive(inFromClient);
					// Receive client's answer to the quiz and update score
					// Need to get variable string answer from client
					score = currentQuiz.calcScore(answer);
					client.addScore(score);
					// Update the scores map
					scores.put(client.getName(), client.getScore());

					// Send the scores of each client to this client
					sendScores();

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
	// The sendQuiz function and the waitAndReceive function have redundant code. Can we fix this?

	// Send the quiz in json format.
	// Wait for a GET request
	public void sendQuiz() {
		try {
			while(true) {
				String request = inFromClient.readLine();
				String[] requestWords = request.split(" ");
				if(requestWords.length == 3) {
					if(requestWords[0].equals("GET") && requestWords[1].equals("/")) {
						String page = getPage("index.html");
						outToClient.writeBytes(page);
					}
					if(requestWords[0].equals("GET") && requestWords[1].equals("/quiz")) {
						String[] answers = currentQuiz.getAnswers();
						String imagePath = currentQuiz.getImagePath();
						String videoPath = currentQuiz.getVideoPath();
						String json = String.format("\"item1\":\"%s\", \"item2\":\"%s\", \"item3\":\"%s\", \"item4\":\"%s\", \"imgPath\":\"%s\", \"vidPath\":\"%s\"",
							answer[0], answer[1], answer[2], answer[3], imagePath, videoPath);

						outToClient.writeBytes(json);
						return;
					}
				}
			}
		} catch (IOException e) {
			active = false;
			e.printTraceStack();
		}
	}
	// Wait to receive the client's answer to the Quiz. While we wait, check for page requests, ya know.
	public int waitAndReceive(BufferedReader inFromClient) {
		try {
			while(true) {
				String request = inFromClient.readLine();
				String[] requestWords = request.split(" ");
				if(requestWords.length == 3) {
					if(requestWords[0].equals("GET") && requestWords[1].equals("/")) {
						String page = getPage("index.html");
						outToClient.writeBytes(page);
					}
					if(requestWords[0].equals("POST") && requestWords[1].equals("/submit")) {
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
		} catch (IOException e) {
			active = false;
			e.printTraceStack();
		}
	}
	public void sendScores() {
		int nClients = scores.size();
		String json = String.format("{\"clients\":\"%d\"", nClients)
		for(Map.Entry<String,Integer> entry : scores.entrySet()) {
			String username = entry.getKey();
			int score = entry.getValue();
			json = String.format("%s,\"%s\":\"%d\"", json, username, score);
		}
		json = json + "}";
		try {
			outToClient.writeBytes(json);
		} catch (IOException e) {
			e.printTraceStack();
			active = false;
		}
	}

	// Get the html code for a given page name
	// returns a single string with all the html code
	public String getPage(String pageName) {
		String body = getHTML(webPage);
		if(body.equals("Not Found") || body.equals("Something really went wrong")) {
			String header = "HTTP/1.0 404 Not Found\n Server: VineQuiz/1.0 Java/9.0.0\n";
			SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
			Date current_time = new Date();
			String date = formatter.format(current_time).toString();
			header = header + date;
			return header;
		}
		else {
			String header = "HTTP/1.0 200 OK\n Server: VineQuiz/1.0 Java/9.0.0\n";
			SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
			Date current_time = new Date();
			String date = formatter.format(current_time).toString();
			int bodyLength = body.getBytes().length;
			header = header + date + "\nContent-type: text/html; charset=utf-8\nContent-Length: ";
			header = header + String.valueOf(bodyLength);
			String headerBody = header + "\n\n" + body;
			return headerBody;
		}
	}
	// get the html code from the file
	private static String getHTML(String filename)
	{
		try
		{
			String body = new String(Files.readAllBytes(Paths.get(filename)));
			return body;
		}
		catch (FileNotFoundException ex)
		{
			System.out.printf("Unable to open file: %s", filename);
			return "Not Found";
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		return "Something really went wrong";
	}
	public static int getNewID() {
		return currentID++;
	}
}
