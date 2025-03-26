package game.players;

import game.Board;
import game.feedbacks.AttackFeedback;
import game.feedbacks.DefeatFeedback;
import game.feedbacks.EqualStrengthFeedback;
import game.feedbacks.Feedback;
import game.feedbacks.MoveFeedback;
import game.pieces.OpponentPiece;
import game.pieces.Piece;
import game.pieces.PieceAction;
import game.pieces.PieceFactory;
import game.pieces.QuantityPerPiece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

public class RaiAraujoPlayer implements Player {
  private String playerName = "Raí Araujo";
  private int[] prisonerPosition;
  private Piece[][] setup = new Piece[4][10];

  private Map<String, Double>[][] enemyProbabilities;
  private Map<String, Integer> enemyPiecesRemaining;

  private Stack<Piece> recentPiecesPlayed = new Stack<Piece>();
  private Map<String, Integer> pieceStrength = new HashMap<>();

  public RaiAraujoPlayer(String name) {
    this.playerName = name;
    initEnemyKnowledge();
    pieceStrength.put("PS", 0);   // Prisioneiro (não combate)
    pieceStrength.put("M", 99);    // Mina (não se move)
    pieceStrength.put("AS", 10);  // Agente Secreto (pode derrotar qualquer peça)
    pieceStrength.put("G", 9);    // General
    pieceStrength.put("CR", 8);   // Coronel
    pieceStrength.put("MJ", 7);   // Major
    pieceStrength.put("CP", 6);   // Capitão
    pieceStrength.put("T", 5);    // Tenente
    pieceStrength.put("ST", 4);   // Subtenente
    pieceStrength.put("SG", 3);   // Sargento
    pieceStrength.put("C", 2);    // Cabo
    pieceStrength.put("S", 1);    // Soldado
  }

  @Override
  public String getPlayerName() {
    return this.playerName;
  }

  @Override
  public Piece[][] setup(Board board) {
    List<String> pieces = generatePiecesList();

    placePrisoner(pieces, board);
    placeMinesAroundPrisoner(pieces, board);
    placeAgent(pieces, board);
    placeCorporals(pieces, board);
    placeRemainingPieces(pieces, board);

    return setup;
  }

  private List<String> generatePiecesList() {
    List<String> pieces = new ArrayList<>();
    for (QuantityPerPiece piece : QuantityPerPiece.values()) {
      pieces.addAll(Collections.nCopies(piece.getQuantity(), piece.getCode()));
    }
    return pieces;
  }

  private void placePrisoner(List<String> pieces, Board board) {
    int prisonerRow = 3;
    int prisonerCol = new Random().nextInt(10);

    prisonerPosition = new int[] { prisonerRow, prisonerCol };
    placePiece(pieces, "PS", prisonerRow, prisonerCol, board);
  }

  // Posiciona minas ao redor do prisioneiro
  private void placeMinesAroundPrisoner(List<String> pieces, Board board) {
    int minesPlaced = 0;
    int maxMines = 3;

    for (int[] pos : getAdjacentPositions(prisonerPosition[0], prisonerPosition[1])) {
      if (minesPlaced >= maxMines)
        break;

      if (placePiece(pieces, "M", pos[0], pos[1], board))
        minesPlaced++;
    }
  }

  private void placeAgent(List<String> pieces, Board board) {
    int row = 2;
    int col = new Random().nextInt(10);

    placePiece(pieces, "AS", row, col, board);
    placePiece(pieces, "M", row - 1, col, board); // Coloca uma mina na frente do agente
  }

  private void placeCorporals(List<String> pieces, Board board) {
    List<Integer> columns = new ArrayList<>();
    int row = 3;

    for (int i = 0; i < 10; i++)
      columns.add(i);

    Collections.shuffle(columns); // Embaralha as colunas para posicionamento aleatório

    for (int col : columns)
      placePiece(pieces, "C", row, col, board);
  }

  private void placeRemainingPieces(List<String> pieces, Board board) {
    Collections.shuffle(pieces);

    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 10; col++) {
        if (pieces.isEmpty())
          break;
        placePiece(pieces, pieces.get(0), row, col, board);
      }
    }
  }

  private boolean placePiece(List<String> pieces, String code, int row, int col, Board board) {
    if (Board.isValidPosition(row, col) && setup[row][col] == null && pieces.contains(code)) {
      setup[row][col] = PieceFactory.createPiece(code, playerName, board);
      pieces.remove(code);
      return true;
    }
    return false;
  }

  private int[][] getAdjacentPositions(int row, int col) {
    return new int[][] {
        { row - 1, col }, // Acima
        { row, col - 1 }, { row, col + 1 }, // Lados
        { row - 1, col - 1 }, { row - 1, col + 1 } // Diagonais superiores
    };
  }

  @SuppressWarnings("unchecked") // Se tira da um erro nd com nd
  public void initEnemyKnowledge() {
    // Cada celula tem um Hmap<String, Double> com as probabilidades de cada peça
    enemyProbabilities = new HashMap[Board.ROWS][Board.COLS];
    for (int x = 0; x < Board.ROWS; x++) {
      for (int y = 0; y < Board.COLS; y++) {
        enemyProbabilities[x][y] = new HashMap<>();
      }
    }
    // Preenche enemyPiecesRemaining com a quantidade inicial de cada tipo
    enemyPiecesRemaining = new HashMap<>();
    for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
      enemyPiecesRemaining.put(qpp.getCode(), qpp.getQuantity());
    }

    // Se sou Player1, o inimigo está em 6..9; se Player2, 0..3
    int enemyStartRow = this.playerName.equals("Player1") ? 6 : 0;
    int enemyEndRow = this.playerName.equals("Player1") ? 9 : 3;
    double totalEnemy = 40.0;

    for (int x = 0; x < Board.ROWS; x++) {
      for (int y = 0; y < Board.COLS; y++) {
        // Se for uma posição de lago zera tudo
        if (Board.isLake(x, y) || x < enemyStartRow || x > enemyEndRow) {
          for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
            enemyProbabilities[x][y].put(qpp.getCode(), 0.0);
          }
        } else {
          for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
            // na proporção de cada peça no total de 40
            double prob = qpp.getQuantity() / totalEnemy;
            enemyProbabilities[x][y].put(qpp.getCode(), prob);
          }
        }
      }
    }
  }

  // Pega as infos dos feedbacks e atualiza as probs
  public void updateKnowledgeFromFeedback(Feedback feedback) {
    if (feedback == null)
      return;

    if (feedback instanceof AttackFeedback atk) {
      // AttackFeedback => o "attacker" venceu e se moveu de [fromX, fromY] para [toX,
      // toY].
      Piece attacker = atk.attacker;
      Piece defender = atk.defender;
      int fromX = atk.fromX;
      int fromY = atk.fromY;
      int toX = atk.toX;
      int toY = atk.toY;

      // 1) Se o inimigo perdeu (defender é inimigo), remover do board
      if (isEnemyPiece(defender)) {
        removeEnemyPieceFromBoard(defender);
      }

      // 2) Se attacker é o inimigo e quem venceu ,
      // então na célula (toX, toY) tem 100% de chance de ser a peça do
      // attacker.getRepresentation()
      if (isEnemyPiece(attacker)) {
        String code = attacker.getRepresentation();
        // Seta a célula (toX, toY) para prob = 1.0(100%) desse code, e 0.0 dos demais
        setCellDistribution(toX, toY, code);

        // A célula (fromX, fromY) agora está vazia => zere as probabilidades ali
        clearCellDistribution(fromX, fromY);
      }
    }

    else if (feedback instanceof DefeatFeedback df) {
      // DefeatFeedback => o "attacker" perdeu, "defender" ganhou e ficou na mesma
      // célula.
      Piece attacker = df.attacker; // perdeu
      Piece defender = df.defender; // ganhou
      int x = defender.getPosX();
      int y = defender.getPosY();

      // 1) Se o inimigo perdeu (attacker é inimigo)
      if (isEnemyPiece(attacker)) {
        removeEnemyPieceFromBoard(attacker);
      }

      // 2) Se o inimigo é quem ganhou (defender),
      // definimos prob = 1.0 para "defender.getRepresentation()" naquela célula
      if (isEnemyPiece(defender)) {
        String code = defender.getRepresentation();
        setCellDistribution(x, y, code);
      }
    }

    else if (feedback instanceof EqualStrengthFeedback eq) {
      // Ambos se eliminaram => se algum era inimigo, removemos do board
      Piece p1 = eq.attacker;
      Piece p2 = eq.defender;

      if (isEnemyPiece(p1))
        removeEnemyPieceFromBoard(p1);
      if (isEnemyPiece(p2))
        removeEnemyPieceFromBoard(p2);
      // a celula fica vazia e decrementa 1 peça de cada time
    }

    // TODO: ver outros feedbacks insanos ai
  }

  /**
   * “fixa” que (x, y) contém exatamente aquela peça e nenhuma outra.
   */
  private void setCellDistribution(int x, int y, String code) {
    if (!Board.isValidPosition(x, y))
      return;
    Map<String, Double> cellDist = enemyProbabilities[x][y];
    for (String c : cellDist.keySet()) {
      if (c.equals(code)) {
        cellDist.put(c, 1.0);
      } else {
        cellDist.put(c, 0.0);
      }
    }
  }

  /**
   * Exemplo de limpar a célula (x,y) => ficou vazia
   */
  private void clearCellDistribution(int x, int y) {
    if (!Board.isValidPosition(x, y))
      return;
    Map<String, Double> cellDist = enemyProbabilities[x][y];
    for (String c : cellDist.keySet()) {
      cellDist.put(c, 0.0);
    }
  }

  /**
   * Remove a peça inimiga do board => se for destruída,
   * zera a célula e decrementa 'enemyPiecesRemaining'.
   */
  private void removeEnemyPieceFromBoard(Piece piece) {
    int px = piece.getPosX();
    int py = piece.getPosY();
    if (!Board.isValidPosition(px, py))
      return;

    String code = piece.getRepresentation();
    // Decrementa do enemyPiecesRemaining
    if (enemyPiecesRemaining.containsKey(code)) {
      int old = enemyPiecesRemaining.get(code);
      if (old > 0)
        enemyPiecesRemaining.put(code, old - 1);
    }
    // Zera probabilidade na célula
    clearCellDistribution(px, py);
  }

  private void updateEnemyProbabilities(Board board) {
    for (int x = 0; x < Board.ROWS; x++) {
      for (int y = 0; y < Board.COLS; y++) {
        Piece boardPiece = board.getPiece(x, y);
        // Pra cada tipo do player atualiza as probabilidades
        for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
          // só atualiza se for diferente de 0
          if (enemyProbabilities[x][y].get(qpp.getCode()) != 0) {
            // reseta a probabilidade
            enemyProbabilities[x][y].put(qpp.getCode(), 0.0);
            
            if (boardPiece == null) // senão houver peça no mapa, não atualiza probabilidades
              continue;
            String code = qpp.getCode();
            int remainingPiecesByCode = enemyPiecesRemaining.get(code);
            double prob = remainingPiecesByCode / qpp.getQuantity();
            enemyProbabilities[x][y].put(qpp.getCode(), prob);
          }
        }
      }
    }
  }

  private boolean isEnemyPiece(Piece piece) {
    // Se a piece não for nula e o player != this.playerName
    if (piece == null || piece.getPlayer() == null)
      return false;
    return !piece.getPlayer().equals(this.playerName);
  }

  private int getStrengthOfCode(String code) {
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException e) {
      return switch (code) {
        case "B" -> 99; // Bomba
        case "PS" -> 0; // Prisioneiro
        default -> -1; // ex: "OP"
      };
    }
  }

  public double estimateWinProbability(Piece myPiece, int ex, int ey) {
    if (!Board.isValidPosition(ex, ey))
      return 0.0;
    // probabilidade de cada peça na célula (ex, ey)
    Map<String, Double> dist = enemyProbabilities[ex][ey];
    if (dist == null || dist.isEmpty())
      return 0.0;

    double sumWin = 0.0, sumTie = 0.0, sumLose = 0.0;
    // Minha força
    int myStr = myPiece.getStrength();

    for (var e : dist.entrySet()) {
      String code = e.getKey();
      double p = e.getValue();
      if (p < 1e-9)
        continue; // se for praticamente 0, ignora

      int enStr = getStrengthOfCode(code);
      // Cabo vs Bomba
      if ("B".equals(code)) {
        if (myStr == 3)
          sumWin += p;
        else
          sumLose += p;
        continue;
      }
      // Agente vs General
      // TODO: apenas se o agente jogar primeiro
      if (myStr == 1 && enStr == 10) {
        sumWin += p;
        continue;
      }
      // Geral
      if (myStr > enStr)
        sumWin += p;
      else if (myStr == enStr)
        sumTie += p;
      else
        sumLose += p;
    }

    // Calcula a probabilidade total de vitória, empate e derrota
    double total = sumWin + sumTie + sumLose;

    if (total < 1e-9)
      return 0.0;
    // retorna a probabilidade de vitória
    return sumWin / total;
  }

  public double estimateRiskOfBomb(int x, int y) {
    if (!Board.isValidPosition(x, y))
      return 0.0;
    Map<String, Double> dist = enemyProbabilities[x][y];
    if (dist == null || dist.isEmpty())
      return 0.0;
    return dist.getOrDefault("B", 0.0);
  }

  /**
   * Peça não é Bomba (M/B) nem Prisioneiro (PS).
   */
  private boolean isMovablePiece(Piece piece) {
    if (piece == null)
      return false;
    String rep = piece.getRepresentation();
    return !(rep.equals("M") || rep.equals("B") || rep.equals("PS"));
  }

  public PieceAction chooseMove(Board board) {
    double bestScore = Double.NEGATIVE_INFINITY;
    PieceAction bestAction = null;

    for (int x = 0; x < Board.ROWS; x++) {
      for (int y = 0; y < Board.COLS; y++) {
        Piece piece = board.getPiece(x, y);
        if (piece != null
            && piece.getPlayer() != null
            && piece.getPlayer().equals(this.playerName)
            && isMovablePiece(piece) && checkRecentActions(piece)) {
          int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
          for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (Board.isValidPosition(nx, ny)) {
              double sc = scoreMove(piece, nx, ny, board);
              if (sc > bestScore) {
                bestScore = sc;
                bestAction = new PieceAction(piece, nx, ny);
              }
            }
          }
        }
      }
    }

    return bestAction;
  }

  public void updateRecentActions(Piece piece) {
    // se o topo da pilha for diferente da peça que vai ser jogada, reseta a pilha
    if (recentPiecesPlayed.isEmpty()
        || !(recentPiecesPlayed.peek().equals(piece))) {
      recentPiecesPlayed.clear();
    }

    recentPiecesPlayed.push(piece);
  }

  public boolean checkRecentActions(Piece piece) {
    // se na pilha já existe 2 jogadas e a última é da mesma peça, evitar jogar pois perde a vez
    if (recentPiecesPlayed.size() >= 2 && recentPiecesPlayed.peek().equals(piece)) {
      return false;
    }
    return true;
  }

  public double scoreMove(Piece myPiece, int tx, int ty, Board board) {
    Piece occupant = board.getPiece(tx, ty);

    // 1) Casa vazia => base 5 + 3 se "para frente" = máx 8
    if (occupant == null) {
      int forwardDir = (this.playerName.equals("Player1")) ? 1 : -1;
      int toX = tx - myPiece.getPosX();
      double bonus = (toX == forwardDir) ? 3.0 : 0.0;
      return 5.0 + bonus;
    }

    // 2) occupant existe
    if (occupant instanceof OpponentPiece) {
      // **Trata OpponentPiece como inimigo "desconhecido"**
      // => "scoreAttackUnknown(...)"
      return scoreAttackUnknown(myPiece, tx, ty);
    }

    String occPlayer = occupant.getPlayer();
    // Se for nulo e NÃO for OpponentPiece, devolve -9999 (peça sem dono, mas não
    // OP?)
    if (occPlayer == null) {
      return -9999.0;
    }

    // Se for aliado => -9999
    return -9999.0;
  }

  private List<String> getStrongerCodes(String code) {
    int myPieceStrength = pieceStrength.get(code);
    List<String> strongers = new ArrayList<>();
    for(var piece: pieceStrength.entrySet()) {
      if (myPieceStrength >= piece.getValue()) {
        strongers.add(piece.getKey());
      }
    }
    return strongers;
  }

  /**
   * Se for OpponentPiece, não temos getPlayer(). Então tratamos como "inimigo"
   * de força desconhecida => assume uma força média ou faz uma heurística
   * para pontuar o ataque.
   */
  private double scoreAttackUnknown(Piece myPiece, int tx, int ty) {
    String code = myPiece.getRepresentation();
    List<String> strongers = getStrongerCodes(code);
    List<Double> probabilities = new ArrayList<>();

    Map<String, Double> cellDist = enemyProbabilities[tx][ty];
    for(var enemyPiece : cellDist.entrySet()) {
        if (strongers.contains(enemyPiece.getKey()) && enemyPiece.getValue() > 0) {
            probabilities.add(enemyPiece.getValue());
        }
    }

    if (probabilities.isEmpty()) {
        return 0;
    }

    probabilities.sort(Collections.reverseOrder());
    double bestProbability = probabilities.get(0);
    
    // Supõe chance de bomba de 20%
    double guessBomb = 0.2;

    // Fórmula de pontuação (ajuste conforme sua lógica de jogo)
    double base = 10.0;
    return base + 80.0 * bestProbability - 30.0 * guessBomb;
}

  @Override
  public PieceAction play(Board board, Feedback myLastFeedback, Feedback enemyLastFeedback) {
    // Atualiza prob
    updateKnowledgeFromFeedback(myLastFeedback);
    updateKnowledgeFromFeedback(enemyLastFeedback);
    updateEnemyProbabilities(board);

    // Tenta escolher jogada
    PieceAction action = chooseMove(board);

    if (action != null) {
      updateRecentActions(action.getPiece());
    } 
    return action;
  }
}