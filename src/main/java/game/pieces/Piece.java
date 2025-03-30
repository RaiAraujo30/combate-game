package game.pieces;

import game.Board;
import game.feedbacks.*;
import java.util.UUID;

public abstract class Piece {
    private final UUID id;
    protected int strength;
    protected int posX;
    protected int posY;
    protected String player; // Identifica o dono da peça (ex.: "Player1" ou "Player2")
    protected Board board;

    public Piece(int strength, String player, Board board) {
        this.id = UUID.randomUUID();
        this.strength = strength;
        this.player = player;
        this.board = board;
    }

    public Piece(int strength, Board board) {
        this.id = UUID.randomUUID();
        this.strength = strength;
        this.board = board;
    }

    public Piece(Piece piece, Board board) {
        this.id = piece.id;
        this.board = board;
        this.strength = piece.getStrength();
        this.player = piece.getPlayer();
        this.posX = piece.getPosX();
        this.posY = piece.getPosY();
    }

    public Piece(Piece piece) {
        this.id = piece.id;
        this.strength = piece.getStrength();
        this.player = piece.getPlayer();
        this.posX = piece.getPosX();
        this.posY = piece.getPosY();
    }

    public boolean equals(Piece piece) {
        return this.id.equals(piece.id);
    }

    /**
     * Verifica se a peça pode se mover para a posição (newX, newY) de acordo com as regras.
     */
    public boolean canMove(int newX, int newY) {
        return Math.abs(newX - posX) + Math.abs(newY - posY) == 1 && Board.isValidPosition(newX, newY);
    }

    /**
     * Realiza o movimento para a posição (newX, newY) se for válido.
     */
    public Feedback move(int newX, int newY, Board board) {
        if (canMove(newX, newY)) {
            board.setPiece(posX, posY, null);

            Piece opponent = board.getPiece(newX, newY);
            if (opponent != null) {
                if (opponent.player.equals(this.player)) {
                    board.setPiece(posX, posY, this);
                    return new InvalidMoveFeedback(posX, posY, newX, newY);
                }
                return this.fight(opponent);
            }

            int posXTemp = posX;
            int posYTemp = posY;

            board.setPiece(newX, newY, this);
            this.setPosition(newX, newY);

            return new MoveFeedback(this, posXTemp, posYTemp);
        }

        return new InvalidMoveFeedback(posX, posY, newX, newY);
    }

    public Feedback fight(Piece piece) {
        if (piece.getClass().getSimpleName().equals("LandMine")) {
            board.setPiece(piece.posX, piece.posY, null);
            return new LandmineFeedback(this, piece);
        }

        if (piece.getClass().getSimpleName().equals("Prisoner")) {
            return new PrisonerFeedback(this, piece);
        }

        if (this.strength > piece.getStrength()) {
            int fromX = this.posX;
            int fromY = this.posY;

            board.setPiece(piece.posX, piece.posY, this);
            this.setPosition(piece.posX, piece.posY);
            return new AttackFeedback(this, piece, fromX, fromY, piece.posX, piece.posY);
        }

        if (this.strength == piece.getStrength()) {
            board.setPiece(piece.posX, piece.posY, null);
            return new EqualStrengthFeedback(this, piece);
        }

        // Se o atacante perde:
        board.setPiece(piece.posX, piece.posY, piece);
        return new DefeatFeedback(this, piece, piece.getPosX(), piece.getPosY());
    };


    public int getStrength() {
        return strength;
    }

    public String getPlayer() {
        return player;
    }

    public void setPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    /**
     * Retorna uma representação simplificada da peça para exibição no tabuleiro.
     * Cada peça pode sobrescrever esse método para retornar sua sigla.
     */
    public String getRepresentation() {
        return "P";
    }

    public abstract Piece copy(Board newBoard);

    public abstract Piece copyWithoutBoard();
}
