package game.feedbacks;

import game.pieces.Piece;

public class MoveFeedback extends Feedback {
    public int fromX;
    public int fromY;
    public int toX;
    public int toY;

    public MoveFeedback(Piece piece, int fromX, int fromY) {
        super(piece);
        this.toX = piece.getPosX();
        this.toY = piece.getPosY();
        this.fromX = fromX;
        this.fromY = fromY;
    }

    @Override
    public String getMessage() {
        String baseString = "%s de %s foi movido de [%s, %d] para [%s, %d]";
        String pieceName = piece.getRepresentation();
        String playerName = piece.getPlayer();
        String fromPosX = convertIntToAlfa(fromX);
        int fromPosY = (fromY + 1);
        String posX = convertIntToAlfa(toX);
        int posY = (toY + 1);

        return String.format(baseString, pieceName, playerName, fromPosX, fromPosY, posX, posY);
    }
}
