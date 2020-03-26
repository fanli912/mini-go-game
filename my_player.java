import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Stack;
import java.util.Random;

public class my_player {

  public static void main (String[] args) throws IOException {
    final int N = 5;
    String inputFile = "input.txt";
    String outputFile = "output.txt";
    FileReader fr = new FileReader(inputFile);
    BufferedReader br = new BufferedReader(fr);

    int piece_type = Integer.parseInt(br.readLine());
    int[][] previous_board = new int[N][N];
    int[][] board = new int[N][N];
    for(int i=0; i<N;i++) {
      char[] read_row = br.readLine().toCharArray();
      for(int j=0; j<N;j++) {
        previous_board[i][j] = Character.getNumericValue(read_row[j]);
      }
    }
    for(int i=0; i<N;i++) {
      char[] read_row = br.readLine().toCharArray();
      for(int j=0; j<N;j++) {
        board[i][j] = Character.getNumericValue(read_row[j]);
      }
    }
    br.close();

    Go go = new Go(N);
    go.set_board(piece_type, previous_board, board);

    RandomPlayer player = new RandomPlayer();
    int[] action = player.get_input(go, piece_type);

    PrintWriter writer = new PrintWriter(outputFile);
    System.out.println(action[0] + "," + action[1]);
    if(action == null) {
      writer.println("PASS");
    }
    else {
     writer.println(action[0] + "," + action[1]);
    }
    writer.close();
  }
}

/////////////GO CLASS////////////
class Go {
  public int size; //the size of board
  public boolean X_move; //X chess plays first
  public int n_move; //trace the number of moves
  public List<int[]> died_pieces; //initialize died pieces to be empty
  public int max_move;
  public boolean verbose;
  public int komi;
  public int[][] board;
  public int[][] previous_board;

  public Go(int n) {
    this.size = n;
    this.X_move = true;
    this.died_pieces = new ArrayList<>();
    this.n_move = 0;
    this.komi = n/2;
    this.verbose = false;
    this.max_move = n * n - 1;
  }

  public void set_board(int piece_type, int[][] previous_board, int[][] board) {
    for(int i=0; i<this.size;i++) {
      for(int j=0; j<this.size;j++) {
        if(previous_board[i][j] == piece_type  && board[i][j] != piece_type) {
          int[] to_add = new int[2];
          to_add[0] = i;
          to_add[1] = j;
          died_pieces.add(to_add);
        }
      }
    }
    this.previous_board = previous_board;
    this.board = board;

  }

  public boolean find_liberty(int[][] board, int i, int j) {
    List<List<Integer>> ally_members = this.ally_dfs(board, i, j);
    for(List<Integer> member:ally_members) {
      List<List<Integer>> neighbors = this.detect_neighbor(board, member.get(0), member.get(1));
      for(List<Integer> piece:neighbors) {
        if(board[piece.get(0)][piece.get(1)] == 0) {
          return true;
        }
      }
    }
    return false;
  }

  public List<List<Integer>> ally_dfs(int[][] board, int i, int j) {
    List<List<Integer>> ally_members = new ArrayList<>();
    Stack<List<Integer>> st = new Stack<>();

    List<Integer> element = new ArrayList<>(2);
    element.add(i);
    element.add(j);

    st.push(element);
  
    while(!st.isEmpty()) {
      List<Integer> piece = st.pop();

      ally_members.add(piece);

      List<List<Integer>> nlist= this.detect_neighbor_ally(board, piece.get(0), piece.get(1));
      for(List<Integer> p : nlist) {
        if(!st.contains(p) && !ally_members.contains(p)) {
          st.push(p);
        }
      }
    }
    return ally_members;
  }

  public List<List<Integer>> detect_neighbor_ally(int[][] board, int i, int j) {
    List<List<Integer>> ret = new ArrayList<>();
    List<List<Integer>> nlist= this.detect_neighbor(board, i, j);

    for(List<Integer> piece : nlist) {
      if(board[piece.get(0)][piece.get(1)] == board[i][j]) {
        ret.add(piece);
      }
    }
    return ret;
  }


  public List<List<Integer>> detect_neighbor(int[][] board, int i, int j) {
    List<List<Integer>> ret = new ArrayList<>();
    if(i > 0) {
      List<Integer> sub1 = new ArrayList<>(2);
      //ret.add(new int[]{i-1, j});
      sub1.add(i-1);
      sub1.add(j);
      ret.add(sub1);
    }
    if(i < board.length - 1) {
      List<Integer> sub2 = new ArrayList<>(2);
      //ret.add(new int[]{i+1, j});
      sub2.add(i+1);
      sub2.add(j);
      ret.add(sub2);
    }
    if(j > 0) {
      List<Integer> sub3 = new ArrayList<>(2);
      //ret.add(new int[]{i, j-1});
      sub3.add(i);
      sub3.add(j-1);
      ret.add(sub3);
    }
    if(j < board.length - 1) {
      List<Integer> sub4 = new ArrayList<>(2);
      //ret.add(new int[]{i, j+1});
      sub4.add(i);
      sub4.add(j+1);
      ret.add(sub4);
    }
    return ret;
  }

  public boolean compare_board(int[][] previous, int[][] board) {
    for(int i=0; i<this.size;i++) {
      for(int j=0; j<this.size;j++) {
        if(previous[i][j] != board[i][j] ) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean valid_place_check(int i, int j, int piece_type, boolean test_check) {
    if(test_check) {this.verbose = false;}
    if(i<0 || i>= board.length) {
      if(verbose) {
        System.out.print("Invalid placement. row should be in the range 1 to {}.");
      }
      return false;
    }
    if(j<0 || j>= board.length) {
      if(verbose) {
        System.out.print("Invalid placement. column should be in the range 1 to {}.");
      }
      return false;
    }
    if(board[i][j] != 0) {
      if(verbose) {
        System.out.print("Invalid placement. There is already a chess in this position.");
      }
      return false;
    }
    //copy board for testing: copy_board
    int[][] deep_copy = new int[size][size];
    for(int row=0; row<this.size;row++) {
      for(int col=0; col<this.size;col++) {
        deep_copy[row][col] = board[row][col];
      }
    }
    //check if the place has liberty: update_board, find_liberty
    deep_copy[i][j] = piece_type;
    if(!this.find_liberty(deep_copy, i, j)) {
      if(verbose) {
        System.out.print("Invalid placement. No liberty found in this position.");
        return false;
      }
    }
    else if(died_pieces.isEmpty() && this.compare_board(deep_copy, board)) {
      if(verbose) {
        System.out.print("Invalid placement. A repeat move not permitted by the KO rule.");
        return false;
      }
    }

    return true;
  }



}

class RandomPlayer {
  String type;
  public RandomPlayer() {
    type = "random";

  }
  public int[] get_input(Go go, int piece_type) {

    List<int[]> possible_placements= new ArrayList<> ();
    for(int i=0; i<go.size; i++) {
      for(int j=0; j<go.size;j++) {
        if(go.valid_place_check(i, j, piece_type, true)) {
          possible_placements.add(new int[]{i, j});
          System.out.println(i + "," + j);
        }
      }
    }

    if(possible_placements.isEmpty()) {
      return null;
    }
    else {
      Random rand = new Random();
      return possible_placements.get(rand.nextInt(possible_placements.size()));
    }

  }

}
//////////////////////////////////////////////
