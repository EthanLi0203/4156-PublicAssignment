package controllers;

import io.javalin.Javalin;

import models.*;

import java.awt.desktop.SystemSleepEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import org.eclipse.jetty.websocket.api.Session;
import com.google.gson.*;

class PlayGame {

  private static final int PORT_NUMBER = 8080;

  private static Javalin app;
  
  private static boolean gameStarted;
  
  static GameBoard currGameBoard;

  /** Main method of the application.
   * @param args Command line arguments
   */
  public static void main(final String[] args) {
	  
	Gson gson = new Gson();
    app = Javalin.create(config -> {
      config.addStaticFiles("/public");
    }).start(PORT_NUMBER);

    // Test Echo Server
    app.post("/echo", ctx -> {
      ctx.result(ctx.body());
    });
    
    // new game
    app.get("/newgame", res ->{
    	res.redirect("/tictactoe.html");
    });
    
    // start game
    app.post("/startgame", ctx -> {
    	
    	String body = ctx.body();
    	System.out.println(body);
    	String type = body.split("=")[1].strip();
    	Player player1 = new Player();
    	player1.setId(1);
    	player1.setType(type.charAt(0));
    	GameBoard gameBoard = new GameBoard();
    	gameBoard.setBoardState(new char[3][3]);
    	gameBoard.setDraw(false);
    	gameBoard.setGameStarted(false);
    	gameBoard.setP1(player1);
    	gameBoard.setTurn(1);
    	
    	currGameBoard = gameBoard;
    	String json = gson.toJson(gameBoard);
    	ctx.result(json);
    });
    
    // join game
    app.get("/joingame", ctx -> {
    	char p2Type = currGameBoard.getP1().getType() == 'X' ? 'O' : 'X';
    	Player player2 = new Player();
    	player2.setId(2);
    	player2.setType(p2Type);
    	currGameBoard.setGameStarted(true);
    	currGameBoard.setP2(player2);
		String json = gson.toJson(currGameBoard);
		sendGameBoardToAllPlayers(json);
    	ctx.redirect("/tictactoe.html?p=2");
    });
    
    // player move
    app.post("/move/:playerId", ctx -> {
    	boolean successMove = true;
    	String playerId = ctx.pathParam("playerId");
    	System.out.println("playerId: "+playerId+"  is moving");
		int X = ctx.body().split("&")[0].charAt(2) - '0';
		int Y = ctx.body().split("&")[1].charAt(2) - '0';
    	currGameBoard.setGameStarted(true);
    	char[][] currBoard = currGameBoard.getBoardState();
		System.out.println(currGameBoard.getTurn());
    	if(currBoard[X][Y] == 'X' || currBoard[X][Y] == 'O' || X < 0 || X > 2 || Y < 0 || Y > 2) {
    		ctx.result("Invalid move");
		}
		Message message = new Message();
    	if(!currGameBoard.isGameStarted()){
    		message.setCode(101);
    		message.setMoveValidity(false);
    		message.setMessage("Please start a game first...");
    		String mes = gson.toJson(message);
    		ctx.result(mes);
    		successMove = false;
		}

    	if(Integer.valueOf(playerId) != currGameBoard.getTurn()){
    		message.setCode(102);
    		message.setMoveValidity(false);
    		String oppo = playerId.equals("1") ? "2" : "1";
    		message.setMessage("Please wait for player "+oppo+"...");
			String mes = gson.toJson(message);
			ctx.result(mes);
			successMove = false;
		}


    	char currType = playerId.equals("1")? currGameBoard.getP1().getType() : currGameBoard.getP2().getType();

		if(currGameBoard.getBoardState()[X][Y] == 'X' || currGameBoard.getBoardState()[X][Y] == 'O'){
			message.setCode(103);
			message.setMoveValidity(false);
			message.setMessage("Are you serious???");
			String mes = gson.toJson(message);
			ctx.result(mes);
			successMove = false;
		}
		if(successMove){

			currBoard[X][Y] = currType;
			currGameBoard.setBoardState(currBoard);
			currGameBoard.setTurn(currGameBoard.getTurn() == 1 ? 2 : 1);

			Result res = checkBoard(currBoard);
			if(res == Result.DRAW){
				message.setMessage("This game draw");
				String mes = gson.toJson(message);
				currGameBoard.setDraw(true);
				ctx.result(mes);

			}else if(res == Result.O_WIN){
				int winplayer = currGameBoard.getP1().getType() == 'O'? 1 : 2;
				currGameBoard.setWinner(winplayer);
				message.setMessage("Player "+ winplayer + "  win!");
				String mes = gson.toJson(message);
				ctx.result(mes);
			}else if(res == Result.X_WIN){
				int winplayer = currGameBoard.getP1().getType() == 'X'? 1 : 2;
				currGameBoard.setWinner(winplayer);
				message.setMessage("Player "+ winplayer + "  win!");
				String mes = gson.toJson(message);
				ctx.result(mes);
			}

			sendGameBoardToAllPlayers(gson.toJson(currGameBoard));
		}

    });
     
    // Web sockets - DO NOT DELETE or CHANGE
    app.ws("/gameboard", new UiWebSocket());
  }

  /** Send message to all players.
   * @param gameBoardJson Gameboard JSON
   * @throws IOException Websocket message send IO Exception
   */
  private static void sendGameBoardToAllPlayers(final String gameBoardJson) {
    Queue<Session> sessions = UiWebSocket.getSessions();
    System.out.println(gameBoardJson);
    for (Session sessionPlayer : sessions) {
      try {
//    	  System.out.println("sessionPlayer:   "+sessionPlayer);
        sessionPlayer.getRemote().sendString(gameBoardJson);
      } catch (IOException e) {
        // Add logger here
    	  e.printStackTrace(System.err);
      }
    }
  }

  // function that check current board game state
  public static Result checkBoard(char[][] board){
		boolean exhausted = true;
	  	for(int i=0; i<3; i++){
	  		int j=0;
	  		// check if this line possibly will make a winner
	  		int x_appear = 0;
	  		int o_appear = 0;
	  		while(j<3){
	  			if(board[i][j] == 'X') x_appear++;
	  			else if(board[i][j] == 'O') o_appear++;
	  			else{
	  				exhausted =false;
				}
	  			if(x_appear == 3){
	  				return Result.X_WIN;
				}else if(o_appear == 3){
	  				return Result.O_WIN;
				}
	  			j++;
			}
		}
	  	for(int i=0; i<3; i++){
			  int j=0;
			  // check if this line possibly will make a winner
			  int x_appear = 0;
			  int o_appear = 0;
			  while(j<3){
				  if(board[j][i] == 'X') x_appear++;
				  else if(board[j][i] == 'O') o_appear++;
				  else{
				  	exhausted =false;
				  }

				  if(x_appear == 3){
					  return Result.X_WIN;
				  }else if(o_appear == 3){
					  return Result.O_WIN;
				  }
				  j++;
			  }
	  	}
	  	int dia_x = 0, anti_dia_x = 0, dia_o = 0, anti_dia_o = 0;
	  	for(int i=0; i<3; i++){
	  		if(board[i][i] == 'X') dia_x++;
	  		else if(board[i][i] == 'O') dia_o++;

	  		if(board[i][2-i] == 'X') anti_dia_x++;
	  		else if(board[i][2-i] == 'O') anti_dia_o++;
		}
	  	if(dia_x == 3 || anti_dia_x == 3) return Result.X_WIN;
	  	else if(dia_o == 3 || anti_dia_o == 3) return Result.O_WIN;

	  	// If board has been exhausted while have not return any player of victory,
	    // in this case it must be draw, else the game need continue
	  	if(exhausted){
	  		return Result.DRAW;
		}else{
	  		return Result.UNDETERMAINED;
		}
  }

  public enum Result{
  		DRAW,
  		X_WIN,
	  	O_WIN,
	  	UNDETERMAINED
  }

  public static void stop() {
    app.stop();
  }



  
}
