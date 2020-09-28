package Test;


import com.google.gson.Gson;
import controllers.PlayGame;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import models.GameBoard;
import models.Message;
import models.Player;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GameTest {
    Gson gson = new Gson();

    @BeforeAll
    public static void init(){
        String[] args = new String[1];
        PlayGame.main(args);
        System.out.println("Before all");
    }
    @BeforeEach
    public void startNewGame() {
        // Test if server is running. You need to have an endpoint /
        // If you do not wish to have this end point, it is okay to not have anything in this method.
        HttpResponse response = Unirest.get("http://localhost:8080/").asString();
        int restStatus = response.getStatus();

        System.out.println("Before Each "+restStatus);
    }

    /**
     * This is a test case to evaluate the newgame endpoint.
     */
    @Test
    @Order(1)
    public void newGameTest() {

        // Create HTTP request and get response
        HttpResponse response = Unirest.get("http://localhost:8080/newgame").asString();
        int restStatus = response.getStatus();

        // Check assert statement (New Game has started)
        assertEquals(restStatus, 200);
        System.out.println("Test New Game");
    }

    /**
     * This is a test case to evaluate the startgame endpoint.
     */
    @Test
    @Order(2)
    public void startGameTest() {

        // Create a POST request to startgame endpoint and get the body
        // Remember to use asString() only once for an endpoint call. Every time you call asString(), a new request will be sent to the endpoint. Call it once and then use the data in the object.
        HttpResponse response = Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
        String responseBody = (String) response.getBody();

        // --------------------------- JSONObject Parsing ----------------------------------

        System.out.println("Start Game Response: " + responseBody);

        // Parse the response to JSON object
        JSONObject jsonObject = new JSONObject(responseBody);

        // Check if game started after player 1 joins: Game should not start at this point
        assertEquals(false, jsonObject.get("gameStarted"));

        // ---------------------------- GSON Parsing -------------------------

        // GSON use to parse data to object
        GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
        Player player1 = gameBoard.getP1();
        int turn = gameBoard.getTurn();
        boolean gameStarted = gameBoard.isGameStarted();
        // Check if player type is correct
        assertEquals('X', player1.getType());
        assertEquals(1, turn);
        assertEquals(false, gameStarted);
        System.out.println("Test Start Game");
    }

    @Test
    @Order(3)
    public void testMoveWithoutStart(){
        HttpResponse response1 = Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
        JSONObject res_body1 = new JSONObject(response1.getBody().toString());
        Message mess1 = gson.fromJson(res_body1.toString(), Message.class);
        assertEquals(false, mess1.isMoveValidity());
        System.out.println("Test move without even start the game...");
    }

    @Test
    @Order(4)
    public void joinGameTest(){
        HttpResponse response = Unirest.get("http://localhost:8080/joingame").asString();
        int status =  response.getStatus();
        System.out.println("Test joingame:  /joingame response status: "+status);
        assertEquals(200, status);
        System.out.println("Test player 2 joingame...");
    }

    @Test
    @Order(5)
    public void moveTest() {
        HttpResponse response1 = Unirest.post("http://localhost:8080/move/2").body("x=0&y=0").asString();
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println("message after player 2 move first:   " + response1.getBody());
        JSONObject res_body1 = new JSONObject(response1.getBody().toString());
        Message mess1 = gson.fromJson(res_body1.toString(), Message.class);

        // player 2 cannot make the first move
        assertEquals(false, mess1.isMoveValidity());
        assertEquals(102, mess1.getCode());
    }

    @Test
    @Order(6)
    public void validMoveTest() {

        // player 1 make a valid move (0, 0), expect valid
        HttpResponse response2 = Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
        JSONObject res_body2 = new JSONObject(response2.getBody().toString());
        Message mess2 = gson.fromJson(res_body2.toString(), Message.class);
        assertEquals(200, response2.getStatus());
        assertEquals(true, mess2.isMoveValidity());
    }
    @Test
    @Order(7)
    public void consecutiveInvalidMoveTest() {

        // player 1 try to make consecutive move, invalid!
        HttpResponse response3 = Unirest.post("http://localhost:8080/move/1").body("x=1&y=0").asString();
        JSONObject res_body3 = new JSONObject(response3.getBody().toString());
        Message mess3 = gson.fromJson(res_body3.toString(), Message.class);
        assertEquals(false, mess3.isMoveValidity());
    }

    @Test
    @Order(8)
    public void moveOnTakenPoint_InvalidMoveTest() {

        // player 2 try to make move (1, 0), valid
        HttpResponse response4 = Unirest.post("http://localhost:8080/move/2").body("x=1&y=0").asString();
        JSONObject res_body4 = new JSONObject(response4.getBody().toString());
        Message mess4 = gson.fromJson(res_body4.toString(), Message.class);
        assertEquals(200, response4.getStatus());
        assertEquals(true, mess4.isMoveValidity());

        // player 1 try to make move (1, 0), but point has already been taken, invalid!
        HttpResponse response5 = Unirest.post("http://localhost:8080/move/1").body("x=1&y=0").asString();
        JSONObject res_body5 = new JSONObject(response5.getBody().toString());
        Message mess5 = gson.fromJson(res_body5.toString(), Message.class);
        assertEquals(false, mess5.isMoveValidity());
    }

    @Test
    @Order(9)
    public void playerWin_MoveTest() {
        // player 1 make a line (0, 0), (0, 1), (0, 2), expect player 1 win
        Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
        Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
        HttpResponse response6 = Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
        JSONObject res_body6 = new JSONObject(response6.getBody().toString());
        Message mess6 = gson.fromJson(res_body6.toString(), Message.class);
        System.out.println(mess6.getMessage());
        assertEquals(200, response6.getStatus());
        assertEquals(true, mess6.isMoveValidity());
        assertEquals("Player 1 win!", mess6.getMessage());

    }

    @Test
    @Order(9)
    public void TestGameBoard_Win(){
        HttpResponse board = Unirest.get("http://localhost:8080/gameboard").asString();
        JSONObject boardJson = new JSONObject(board.getBody().toString());
        GameBoard currBoard = gson.fromJson(boardJson.toString(), GameBoard.class);
        assertEquals(1, currBoard.getWinner());
    }

    @Test
    @Order(10)
    public void testDraw(){
        System.out.println("Testing draw game...");
        startNewGame();
        Unirest.get("http://localhost:8080/newgame").asString();
        Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
        Unirest.get("http://localhost:8080/joingame").asString();
        Unirest.post("http://localhost:8080/move/1").body("x=1&y=0").asString();
        Unirest.post("http://localhost:8080/move/2").body("x=0&y=0").asString();
        Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
        Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
        Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
        Unirest.post("http://localhost:8080/move/2").body("x=1&y=2").asString();
        Unirest.post("http://localhost:8080/move/1").body("x=2&y=1").asString();
        Unirest.post("http://localhost:8080/move/2").body("x=2&y=0").asString();
        HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();
        JSONObject body = new JSONObject(response.getBody().toString());
        Message mess = gson.fromJson(body.toString(), Message.class);
        assertEquals("This game draw", mess.getMessage());
        assertEquals(101, mess.getCode());
    }

    @Test
    @Order(11)
    public void TestGameBoard_Draw(){
        HttpResponse board = Unirest.get("http://localhost:8080/gameboard").asString();
        JSONObject boardJson = new JSONObject(board.getBody().toString());
        GameBoard currBoard = gson.fromJson(boardJson.toString(), GameBoard.class);
        assertEquals(true, currBoard.isDraw());
    }

    @Test
    @Order(12)
    public void testJoinBeforeGame(){
        startNewGame();
        Unirest.get("http://localhost:8080/newgame").asString();
        HttpResponse board = Unirest.get("http://localhost:8080/gameboard").asString();
        System.out.println(board.getBody().toString());
        HttpResponse response = Unirest.get("http://localhost:8080/joingame").asString();
        System.out.println(response.getBody().toString());
        JSONObject body = new JSONObject(response.getBody().toString());
        Message mess = gson.fromJson(body.toString(), Message.class);
        assertEquals("Haven't start game yet...", mess.getMessage());
    }

    /**
     * This will run every time after a test has finished.
     */
    @AfterEach
    public void finishGame() {
        System.out.println("After Each");
    }

    // Test the case game reaches a draw


    /**
     * This method runs only once after all the test cases have been executed.
     */
    @AfterAll
    public static void close() {
        // Stop Server
        PlayGame.stop();
        System.out.println("After All");
    }
}
