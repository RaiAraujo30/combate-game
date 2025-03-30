package game.feedbacks;

import game.pieces.OpponentPiece;

public class ConvertFeedbackToEnemy {
    public static Feedback convert(Feedback feedback) {
        if (feedback instanceof InvalidMoveFeedback) {
            return new InvalidMoveFeedback(feedback.getMessage(), false);
        }

        if (feedback instanceof MoveFeedback) {
            return new MoveFeedback(new OpponentPiece(feedback.getPiece()), ((MoveFeedback) feedback).fromX, ((MoveFeedback) feedback).fromY);
        }

        return feedback; // LandmineFeedback, LandMineDeactivationFeedback, PrisonerFeedback, AttackFeedback, EqualStrengthFeedback, DefeatFeedback
    }
}
