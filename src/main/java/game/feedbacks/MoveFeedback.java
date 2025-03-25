package game.feedbacks;

import game.pieces.Piece;

public class MoveFeedback extends Feedback {

    public MoveFeedback(Piece piece) {
        super(piece);
    }

    @Override
    public String getMessage() {
        String baseString = "%s de %s foi movido para [%s, %d]";
        String pieceName = piece.getRepresentation();
        String playerName = piece.getPlayer();
        String posX = convertIntToAlfa(piece.getPosX());
        int posY = (piece.getPosY() + 1);

        return String.format(baseString, pieceName, playerName, posX, posY);
    }
}
