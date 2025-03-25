package game.players;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import game.Board;
import game.feedbacks.AttackFeedback;
import game.feedbacks.DefeatFeedback;
import game.feedbacks.EqualStrengthFeedback;
import game.feedbacks.Feedback;
import game.feedbacks.InvalidMoveFeedback;
import game.pieces.Captain;
import game.pieces.Colonel;
import game.pieces.Corporal;
import game.pieces.General;
import game.pieces.LandMine;
import game.pieces.Lieutenant;
import game.pieces.Major;
import game.pieces.OpponentPiece;
import game.pieces.Piece;
import game.pieces.PieceAction;
import game.pieces.Prisoner;
import game.pieces.QuantityPerPiece;
import game.pieces.Sargent;
import game.pieces.SecretAgent;
import game.pieces.Soldier;
import game.pieces.Sublieutenant;

public class SimplePlayer implements Player {
    private final String playerName;
    private final Set<Piece> exhaustedPieces = new HashSet<>();

    private Map<String, Double>[][] enemyProbabilities;
    private Map<String, Integer> enemyPiecesRemaining;

    public SimplePlayer(String playerName) {
        this.playerName = playerName;
    }

    @Override
    public String getPlayerName() {
        return this.playerName;
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
        int enemyEndRow   = this.playerName.equals("Player1") ? 9 : 3;
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
        if (feedback == null) return;
    
        if (feedback instanceof AttackFeedback atk) {
            // AttackFeedback => o "attacker" venceu e se moveu de [fromX, fromY] para [toX, toY].
            Piece attacker = atk.attacker; // venceu
            Piece defender = atk.defender; // perdeu
            int fromX = atk.fromX;
            int fromY = atk.fromY;
            int toX = atk.toX;
            int toY = atk.toY;
    
            // 1) Se o inimigo perdeu (defender é inimigo), remover do board
            if (isEnemyPiece(defender)) {
                removeEnemyPieceFromBoard(defender);
            }
    
            // 2) Se o inimigo é quem venceu (attacker),
            //    então na célula (toX, toY) tem 100% de chance de ser a peça do attacker.getRepresentation()
            if (isEnemyPiece(attacker)) {
                String code = attacker.getRepresentation();
                // Seta a célula (toX, toY) para prob = 1.0(100%) desse code, e 0.0 dos demais
                setCellDistribution(toX, toY, code);
    
                // A célula (fromX, fromY) agora está vazia => zere as probabilidades ali
                clearCellDistribution(fromX, fromY);
            }
        }
    
        else if (feedback instanceof DefeatFeedback df) {
            // DefeatFeedback => o "attacker" perdeu, "defender" ganhou e ficou na mesma célula.
            Piece attacker = df.attacker; // perdeu
            Piece defender = df.defender; // ganhou
            int x = defender.getPosX();
            int y = defender.getPosY();
    
            // 1) Se o inimigo perdeu (attacker é inimigo)
            if (isEnemyPiece(attacker)) {
                removeEnemyPieceFromBoard(attacker);
            }
    
            // 2) Se o inimigo é quem ganhou (defender),
            //    definimos prob = 1.0 para "defender.getRepresentation()" naquela célula
            if (isEnemyPiece(defender)) {
                String code = defender.getRepresentation();
                setCellDistribution(x, y, code);
            }
        }
    
        else if (feedback instanceof EqualStrengthFeedback eq) {
            // Ambos se eliminaram => se algum era inimigo, removemos do board
            Piece p1 = eq.attacker;
            Piece p2 = eq.defender;
    
            if (isEnemyPiece(p1)) removeEnemyPieceFromBoard(p1);
            if (isEnemyPiece(p2)) removeEnemyPieceFromBoard(p2);
            // a celula fica vazia e decrementa 1 peça de cada time
        }
        
        // TODO: ver outros feedbacks insanos ai
    }
    
    /** 
     * “fixa” que (x, y) contém exatamente aquela peça e nenhuma outra.
     */
    private void setCellDistribution(int x, int y, String code) {
        if (!Board.isValidPosition(x, y)) return;
        Map<String, Double> cellDist = enemyProbabilities[x][y];
        double sum = 0.0;
        for (String c : cellDist.keySet()) {
            if (c.equals(code)) {
                cellDist.put(c, 1.0);
                sum += 1.0;
            } else {
                cellDist.put(c, 0.0);
            }
        }
    }
    
    /** 
     * Exemplo de limpar a célula (x,y) => ficou vazia
     */
    private void clearCellDistribution(int x, int y) {
        if (!Board.isValidPosition(x, y)) return;
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
        if (!Board.isValidPosition(px, py)) return;
    
        String code = piece.getRepresentation();
        // Decrementa do enemyPiecesRemaining
        if (enemyPiecesRemaining.containsKey(code)) {
            int old = enemyPiecesRemaining.get(code);
            if (old > 0) enemyPiecesRemaining.put(code, old - 1);
        }
        // Zera probabilidade na célula
        clearCellDistribution(px, py);
    }

    private boolean isEnemyPiece(Piece piece) {
        // Se a piece não for nula e o player != this.playerName
        if (piece == null || piece.getPlayer() == null) return false;
        return !piece.getPlayer().equals(this.playerName);
    }


    private int getStrengthOfCode(String code) {
        try {
            return Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return switch (code) {
                case "B"  -> 99; // Bomba
                case "PS" -> 0;  // Prisioneiro
                default   -> -1; // ex: "OP"
            };
        }
    }

    public double estimateWinProbability(Piece myPiece, int ex, int ey) {
        if (!Board.isValidPosition(ex, ey)) return 0.0;
        // probabilidade de cada peça na célula (ex, ey)
        Map<String, Double> dist = enemyProbabilities[ex][ey];
        if (dist == null || dist.isEmpty()) return 0.0;

        double sumWin = 0.0, sumTie = 0.0, sumLose = 0.0;
        // Minha força
        int myStr = myPiece.getStrength();

        for (var e : dist.entrySet()) {
            String code = e.getKey();
            double p = e.getValue();
            if (p < 1e-9) continue; // se for praticamente 0, ignora

            int enStr = getStrengthOfCode(code);
            // Cabo vs Bomba
            if ("B".equals(code)) {
                if (myStr == 3) sumWin += p;
                else sumLose += p;
                continue;
            }
            // Agente vs General 
            // TODO: apenas se o agente jogar primeiro
            if (myStr == 1 && enStr == 10) {
                sumWin += p;
                continue;
            }
            // Geral
            if (myStr > enStr) sumWin += p;
            else if (myStr == enStr) sumTie += p;
            else sumLose += p;
        }

        // Calcula a probabilidade total de vitória, empate e derrota
        double total = sumWin + sumTie + sumLose;

        if (total < 1e-9) return 0.0;
        // retorna a probabilidade de vitória
        return sumWin / total;
    }

    public double estimateRiskOfBomb(int x, int y) {
        if (!Board.isValidPosition(x, y)) return 0.0;
        Map<String, Double> dist = enemyProbabilities[x][y];
        if (dist == null || dist.isEmpty()) return 0.0;
        return dist.getOrDefault("B", 0.0);
    }

    /**
     * Peça não é Bomba (M/B) nem Prisioneiro (PS).
     */
    private boolean isMovablePiece(Piece piece) {
        if (piece == null) return false;
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
                    && isMovablePiece(piece)
                    && !exhaustedPieces.contains(piece))
                {
                    int[][] dirs = { {-1,0}, {1,0}, {0,-1}, {0,1} };
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


    public double scoreMove(Piece myPiece, int tx, int ty, Board board) {
        Piece occupant = board.getPiece(tx, ty);

        // 1) Casa vazia => base 5 + 3 se "para frente" = máx 8
        if (occupant == null) {
            int forwardDir = (this.playerName.equals("Player1")) ? 1 : -1;
            int deltaX = tx - myPiece.getPosX();
            double bonus = (deltaX == forwardDir) ? 3.0 : 0.0;
            return 5.0 + bonus;
        }

        // 2) occupant existe
        if (occupant instanceof OpponentPiece) {
            // **Trata OpponentPiece como inimigo "desconhecido"** 
            // => "scoreAttackUnknown(...)"
            return scoreAttackUnknown(myPiece);
        }

        String occPlayer = occupant.getPlayer();
        // Se for nulo e NÃO for OpponentPiece, devolve -9999 (peça sem dono, mas não OP?)
        if (occPlayer == null) {
            return -9999.0;
        }

        // Se for inimigo "real"
        // TODO: se ja conhecemos a peça, basta calcular se ganhamos ou nao
        if (!occPlayer.equals(this.playerName)) {
            double winProb = estimateWinProbability(myPiece, tx, ty);
            double bombRisk = estimateRiskOfBomb(tx, ty);
            // base + 80*winProb - 30*bombRisk
            return 10.0 + 80.0 * winProb - 30.0 * bombRisk;
        }

        // Se for aliado => -9999
        return -9999.0;
    }

    /**
     * Se for OpponentPiece, não temos getPlayer(). Então tratamos como "inimigo"
     * de força desconhecida => assume uma força média ou faz uma heurística
     * para pontuar o ataque.
     */
    private double scoreAttackUnknown(Piece myPiece) {
        int myStr = myPiece.getStrength();
        double guessWinProb;
    
        if (myStr >= 9) guessWinProb = 0.9;
        else if (myStr >= 6) guessWinProb = 0.7;
        else if (myStr == 3) guessWinProb = 0.5;
        else guessWinProb = 0.3;
    
        // Supõe chance de bomba de 20%
        double guessBomb = 0.2;
    
        double base = 10.0;
        return base + 80.0 * guessWinProb - 30.0 * guessBomb;
    }

    @Override
    public Piece[][] setup(Board board) {
        initEnemyKnowledge();

        List<String> codes = new ArrayList<>();
        for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
            for (int i = 0; i < qpp.getQuantity(); i++) {
                codes.add(qpp.getCode());
            }
        }
        Collections.shuffle(codes);

        Piece[][] result = new Piece[4][10];
        int idx = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                String code = codes.get(idx++);
                result[i][j] = createPieceFromCode(code, this.playerName, board);
            }
        }
        return result;
    }

    private Piece createPieceFromCode(String code, String player, Board board) {
        return switch (code) {
            case "G"  -> new General(player, board);
            case "CR" -> new Colonel(player, board);
            case "MJ" -> new Major(player, board);
            case "CP" -> new Captain(player, board);
            case "T"  -> new Lieutenant(player, board);
            case "SG" -> new Sargent(player, board);
            case "ST" -> new Sublieutenant(player, board);
            case "C"  -> new Corporal(player, board);
            case "S"  -> new Soldier(player, board);
            case "AS" -> new SecretAgent(player, board);
            case "PS" -> new Prisoner(player, board);
            case "M"  -> new LandMine(player, board);
            default   -> new OpponentPiece(board);
        };
    }

    @Override
    public PieceAction play(Board board, Feedback myLastFeedback, Feedback enemyLastFeedback) {
        // Atualiza prob
        updateKnowledgeFromFeedback(myLastFeedback);
        updateKnowledgeFromFeedback(enemyLastFeedback);

        // Se "mesma peça 3x", parsear e exaurir
        if (myLastFeedback instanceof InvalidMoveFeedback imf) {
            String msg = imf.getMessage();
            if (msg.contains("mesma peça mais de 3 vezes")) {
                parseExhaustedPiece(msg, board);
            }
        }

        // Tenta escolher jogada
        PieceAction action = chooseMove(board);
        return action; // 
    }

    private void parseExhaustedPiece(String msg, Board board) {
        // Ex: "Jogada inválida: Player1 moveu a mesma peça mais de 3 vezes, CP em [A, 0]"
        int idxEm = msg.indexOf(" em [");
        if (idxEm < 0) return;
        String sub = msg.substring(idxEm + 5);
        int idxFech = sub.indexOf(']');
        if (idxFech < 0) return;
        String posStr = sub.substring(0, idxFech); // "A, 0"
        String[] parts = posStr.split(",\\s*");
        if (parts.length != 2) return;

        int row = parts[0].charAt(0) - 'A'; 
        int col = Integer.parseInt(parts[1]);

        int idxVirg = msg.lastIndexOf(',', idxEm);
        if (idxVirg < 0) return;
        String rep = msg.substring(idxVirg + 2, idxEm).trim(); // "CP"

        if (Board.isValidPosition(row, col)) {
            Piece p = board.getPiece(row, col);
            if (p != null && p.getRepresentation().equals(rep)) {
                exhaustedPieces.add(p);
            }
        }
    }
}
