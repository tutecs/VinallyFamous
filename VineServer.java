// VineQuiz server program.
// Ethan Duryea, Elliot Spicer, Joshua Weller, John Zamites
// CSCI 420: Networking, Project 2018, April 2018
//
// TODO:
// Need to get the username from the client
// Should send a startup page when the client connects with a prompt that asks them to enter a name
// Once they submit, we can send the client the main game page.
// We also need to send the scores with the main game page.

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class VineServer {
	private static int port = 8080;
	public static void main(String[] args) {
		ArrayList<ClientServer> clients = new ArrayList<ClientServer>();
		try {

			QuizGetter quizGetter = null;

			ServerSocket listenSocket = new ServerSocket(port);
			// This represents the previous number connected clients.
			// This variable will be used to trigger the quizMaker thread.
			Socket connectionSocket = null;
			int prevN = 0;
			while(true) {
				for(int i = 0; i < clients.size(); i++) {
					if(clients.get(i).isActive() == false) {
						clients.remove(i);
					}
				}
				connectionSocket = listenSocket.accept();
				System.out.println(listenSocket.getInetAddress().getCanonicalHostName());
				ClientServer server = new ClientServer(connectionSocket);
				server.start();
				clients.add(server);
				System.out.printf("Connected Clients: %d\n", clients.size());
				if(clients.size() == 1 && clients.size() - prevN > 0) {
					System.out.println("Starting QuizGetter Thread");
					quizGetter = new QuizGetter("QuizGetter", 13);
					quizGetter.start();
				}
				if(clients.size() == 0 && clients.size() - prevN < 0) {
					System.out.println("Stopping QuizGetter Thread");
					quizGetter.stop();
				}
				prevN = clients.size();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
// QuizGetter Class. Updates the currentQuiz of the ClientServer class every x seconds
class QuizGetter implements Runnable {
	private volatile boolean active;
	private String threadName;
	private Thread t;
	private int updateTime;
	public QuizGetter(String threadName, int updateTime) {
		this.threadName = threadName;
		this.updateTime = updateTime;
		this.active = true;
	}
	public void run() {
		System.out.println("Making a new quiz");
		Quiz quiz = Quiz.newQuiz();
		ClientServer.updateQuiz(quiz);
	 	Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("Making a new quiz");
				Quiz quiz = Quiz.newQuiz();
				ClientServer.updateQuiz(quiz);
			}
		}, 0, updateTime*1000);
	}
	public void start() {
		if(t == null) {
			t = new Thread(this, threadName);
			t.start();
		}
	}
	public void stop() {
		active = false;
	}
}


// Quiz class. Contains all the information about the current quiz. Including the path to the image,
// the path to the vine clip, the correct answer, and three incorrect answers.
// We need to figure out a process of randomly selecting three incorrect answers and ordering the four answers
// randomly.

class Quiz {
	// This hashmap holds all of the vine info. The keys are the name of the vine and the value is an
	// array of two Strings. The first element is the image path and the second is the video path.

	private static HashMap<String, String[]> vines = readVineFile();
	private static int prevIndex = -1;

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
		return answers;
	}
	public String getImagePath() {
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
		int[] randAns = {randX, -1,-1,-1};
		int rand = 0;
		for(int i = 1; i < randAns.length; i++) {
			rand = random.nextInt(vines.size());
			// make sure there are no repeating answers
			while(rand == randX || rand == randAns[1] || rand == randAns[2] || rand == randAns[3]) {
				rand = random.nextInt(vines.size());
			}
			randAns[i] = rand;
		}
		// randomly order the answers
		int correctIdx = -1;
		int[] answers = {-1,-1,-1,-1};
		for(int i = 0; i < answers.length; i++) {
			rand = random.nextInt(answers.length);
			while(answers[rand] != -1) {
				rand = random.nextInt(answers.length);
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
	public static HashMap<String, String[]> readVineFile() {
		HashMap<String, String[]> vines = new HashMap<String, String[]>();
		try {
			FileReader fileReader = new FileReader("vines.txt");
			BufferedReader reader = new BufferedReader(fileReader);
			String line = "";
			while((line = reader.readLine()) != null) {
				String[] elements = line.split(",");
				System.out.printf("Elements size %d\n", elements.length);
				String vineName = elements[0];
				String[] paths = {elements[1], elements[2]};
				vines.put(vineName, paths);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return vines;
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
	public void setName(String username) {
		this.name = username;
	}
}

// Thread class for each connected client
class ClientServer implements Runnable {
	// Static HashMap will be shared by every thread.
	// New user:scores pairs will be updated by the main loop
	// the scores will be updated by the threads
	private static Map<String,Integer> scores = new ConcurrentHashMap<String, Integer>();

	// Static Quiz that will be updated by our main loop and sent to the client
	private static volatile Quiz currentQuiz = null;
	private static long newQuizTime = 0;
	private static volatile int currentID = 0;
	private Thread t;
	private String threadName;
	private Client client;
	private Socket socket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	private int state;
	// states:
	//	0 - just started, need username
	// 	1 - In game, sent quiz, need answer
	//	2 - In game, received answer, need to send quiz
	private volatile boolean active;
	public ClientServer(Socket connection) {
		try {
			InetAddress address = connection.getInetAddress();
			int port = connection.getPort();
			int id = getNewID();
			this.threadName = String.valueOf(id);
			this.socket = connection;
			this.inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.outToClient = new DataOutputStream(socket.getOutputStream());
			this.client = new Client(address, port, id, null);
			this.state = 0;
			this.active = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// The run method will send the quiz info to the client.
	// We will also receive which answer the client choose, determine if the answer was correct, and give
	// an score accordingly.
	public void run() {
		System.out.println("Running thread");
		try {
			Quiz prevQuiz = null;
			if(client.getName() == null) {
				getUsername();
			}
			// run this loop until we detect that we are disconnected from the client
			while(active) {
				String request = inFromClient.readLine();
				if(request != null) {
					String[] requestWords = request.split(" ");
					if(requestWords.length == 3) {
						if(requestWords[0].equals("GET")) {
							sendPage(requestWords[1]);
						}
					}
				}
				// if(currentQuiz != prevQuiz) {
				// 	// Send the quiz info to the client using xmlhttprequest
				// 	sendQuiz();
				// 	waitAndReceive(inFromClient);
				//
				// 	prevQuiz = currentQuiz;
				// }
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	public void start() {
		if(t == null) {
			t = new Thread(this, threadName);
			t.start();
		}
	}
	public boolean isActive() {
		return active;
	}

	// The sendQuiz function and the waitAndReceive function have redundant code. Can we fix this?
	public void sendPage(String pageName) {
		String page = "";
		if(pageName.equals("/")) {
			System.out.println("Serving vine website");
			page = getPage("vinequiz.html");
		}
		else if(pageName.equals("/quiz")) {
			page = getPage("vinequiz.html");
		}
		else if(pageName.equals("/getQuiz")) {
			sendQuiz();
			return;
		}
		else if(pageName.contains("images")) {
			sendImage(pageName);
			return;
		}
		else {
			page = getPage("not found");
		}
		try {
			System.out.printf("Writing %s out to client\n", pageName);
			outToClient.writeBytes(page);
		} catch (IOException e) {
			active = false;
			e.printStackTrace();
		}
	}
	// Send the quiz in json format.
	// Wait for a GET request
	public void getUsername() {
		System.out.println("Running getUsername");
		try {
			while(true) {
				String request = inFromClient.readLine();
				if(request != null) {
					System.out.println(socket.getKeepAlive());
					System.out.printf("GET INFO: %s\n", request);
					String[] requestWords = request.split(" ");
					if(requestWords.length == 3) {
						if(requestWords[0].equals("GET")) {
							System.out.printf("requested page from GET %s\n", requestWords[1]);
							sendPage(requestWords[1]);
							while(!(request = inFromClient.readLine()).isEmpty()) {
								if(request.equals("Connection: keep-alive"))
									socket.setKeepAlive(true);
								System.out.println(request);
							}
							System.out.println("End of GET");
						}
						if(requestWords[0].equals("POST") && requestWords[1].contains("/username")) {
							System.out.println("Got post username");
							String line = "";
							int contentLength = 0;
							while(!(line = inFromClient.readLine()).isEmpty()) {
								if(line.toLowerCase().contains("content-length"))
									contentLength = Integer.valueOf(line.split(" ")[1]);
								System.out.println(line);
							}
							char[] body = new char[contentLength];
							inFromClient.read(body, 0, contentLength);
							for(int i = 0; i < body.length; i++) {
								if(body[i] == '+')
									body[i] = ' ';
								if(body[i] == '%') {
									String hex = "" + body[i+1] + body[i+2];
									char newChar = (char) (Integer.parseInt(hex, 16));
									body[i] = newChar;
									body[i+1] = Character.MIN_VALUE;
									body[i+2] = Character.MIN_VALUE;
									i += 2;
								}
							}
							String stringBody = String.valueOf(body);
							client.setName(stringBody);
							sendQuiz();
							return;
						}
					}
				}
				else
					continue;
			}
		} catch (IOException e) {
			active = false;
			e.printStackTrace();
		}
		return;
	}
	// Send new quiz info in json format
	// {"item1":"", "item2":"", "item3":"", "item4":"", "imgPath":"", "vidPath":""}
	public void sendQuiz() {
		try {
			long currentTime = System.currentTimeMillis();
			long diff = currentTime - newQuizTime;
			long waitTime = 13000 - diff;
			String[] answers = currentQuiz.getAnswers();

			String imagePath = currentQuiz.getImagePath();
			String videoPath = currentQuiz.getVideoPath();
			String json = String.format("{\"item1\":\"%s\", \"item2\":\"%s\", \"item3\":\"%s\", \"item4\":\"%s\", \"imgPath\":\"%s\", \"vidPath\":\"%s\", \"time\":\"%d\"}",
				answers[0], answers[1], answers[2], answers[3], imagePath, videoPath, waitTime);


			String header = "HTTP/1.0 200 OK\n Server: VineQuiz/1.0 Java/9.0.0\nConnection: Keep-Alive\n";
			SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
			Date current_time = new Date();
			String date = formatter.format(current_time).toString();
			int bodyLength = json.getBytes().length;
			header = header + date + "\nContent-Type: application/json; charset=utf-8\nContent-Length: ";
			header = header + String.valueOf(bodyLength);

			String headerBody = header + "\n\n" + json;
			outToClient.writeBytes(headerBody);
			waitAndReceive();
			return;
		} catch (IOException e) {
			active = false;
			e.printStackTrace();
		}
	}
	// Wait to receive the client's answer to the Quiz. While we wait, check for page requests, ya know.
	public void waitAndReceive() {
		try {
			while(true) {
				String request = inFromClient.readLine();
				System.out.printf("received in waitAndReceive : %s\n", request);
				if(request != null) {
					String[] requestWords = request.split(" ");

					if(requestWords.length == 3) {
						if(requestWords[0].equals("GET")) {
							sendPage(requestWords[1]);
						}
						if(requestWords[0].equals("POST") && requestWords[1].equals("/submit")) {
							// Code for parsing the POST request
							// Should be in x-www-form-urlencoded format
							System.out.println("Received /submit");
							boolean noBlank = true;
							String line = "";
							int contentLength = 0;
							while(!(line = inFromClient.readLine()).isEmpty()) {
								if(line.toLowerCase().contains("content-length"))
									contentLength = Integer.valueOf(line.split(" ")[1]);
							}
							char[] body = new char[contentLength];
							inFromClient.read(body, 0, contentLength);
							String stringBody = String.valueOf(body);
							int answer = Integer.valueOf(stringBody);

							int score = currentQuiz.calcScore(answer);
							client.addScore(score);
							// Update the scores map
							scores.put(client.getName(), client.getScore());

							// Send the scores of each client to this client
							sendScores(currentQuiz.getCorrect(), client.getScore());
							return;
						}
					}
				}
			}
		} catch (IOException e) {
			active = false;
			e.printStackTrace();
		}
	}
	public void sendScores(int correctIdx, int clientScore) {
		String header = "HTTP/1.1 200 OK\nServer: VineQuiz/1.0 Java/9.0.0\nConnection: Keep-Alive\n";
		SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
		Date current_time = new Date();
		String date = formatter.format(current_time).toString();

		int nClients = scores.size();
		HashMap<String, Integer> sortedScores = sortByValues(scores);
		String json = String.format("[{\"correctAns\":\"%d\",\"myscore\":\"%d\"}", correctIdx, clientScore);
		int i = 0;
		Set scoreSet = sortedScores.entrySet();
		Iterator scoreIterator = scoreSet.iterator();
		System.out.println(scoreSet.size());
		while(i < 10 && i < scoreSet.size()) {
			Map.Entry<String,Integer> entry = (Map.Entry) scoreIterator.next();
			String username = entry.getKey();
			int score = entry.getValue();
			json = String.format("%s,{\"username\":\"%s\",\"score\":\"%d\"}", json, username, score);
			i++;
		}
		json = json + "]";
		int bodyLength = json.getBytes().length;
		header = header + date + "\nContent-Type: application/json; charset=utf-8\nContent-Length: ";
		header = header + String.valueOf(bodyLength);
		String headerBody = header + "\n\n" + json;
		try {
			outToClient.writeBytes(headerBody);
		} catch (IOException e) {
			e.printStackTrace();
			active = false;
		}
	}

	// Get the html code for a given page name
	// returns a single string with all the html code
	public String getPage(String pageName) {
		String body = getHTML(pageName);
		if(body.equals("Not Found") || body.equals("Something really went wrong")) {
			String header = "HTTP/1.1 404 Not Found\nServer: VineQuiz/1.0 Java/9.0.0\nConnection: Keep-Alive\n";
			SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
			Date current_time = new Date();
			String date = formatter.format(current_time).toString();
			header = header + date;
			return header;
		}
		else {
			String header = "HTTP/1.1 200 OK\nServer: VineQuiz/1.0 Java/9.0.0\nConnection: Keep-Alive\n";
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
		System.out.printf("Opening file: %s\n", filename);
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
			System.out.printf("Unable to open file: %s", filename);
			return "Not Found";
		}
	}
	private void sendImage(String imgPath) {
		try {
			File img = new File(imgPath.substring(1));
			String header = "HTTP/1.1 200 OK\nServer: VineQuiz/1.0 Java/9.0.0\nConnection: Keep-Alive\n";
			SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
			Date current_time = new Date();
			String date = formatter.format(current_time).toString();
			long bodyLength = img.length();
			header = header + date + "\nContent-type: image/jpeg; charset=utf-8\nContent-Length: ";
			header = header + String.valueOf(bodyLength) + "\n\n";
			outToClient.writeBytes(header);
			Files.copy(img.toPath(), outToClient);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	public static int getNewID() {
		return currentID++;
	}
	public static void updateQuiz(Quiz newQuiz){
		currentQuiz = newQuiz;
		newQuizTime = System.currentTimeMillis();
	}
	private static HashMap sortByValues(Map map) {
		List list = new LinkedList(map.entrySet());
		// Defined Custom Comparator here
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
					.compareTo(((Map.Entry) (o2)).getValue());
			}
		});
		HashMap sortedHashMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedHashMap.put(entry.getKey(), entry.getValue());
		}
		return sortedHashMap;
	}
}
