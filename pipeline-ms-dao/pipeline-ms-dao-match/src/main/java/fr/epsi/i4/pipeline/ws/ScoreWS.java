package fr.epsi.i4.pipeline.ws;

import com.thomaskint.minidao.enumeration.MDConditionLink;
import com.thomaskint.minidao.enumeration.MDConditionOperator;
import com.thomaskint.minidao.exception.MDException;
import com.thomaskint.minidao.querybuilder.MDCondition;
import com.thomaskint.minidao.querybuilder.MDSelectBuilder;
import fr.epsi.i4.pipeline.model.AjoutPoint;
import fr.epsi.i4.pipeline.model.Score;
import fr.epsi.i4.pipeline.model.Set;
import fr.epsi.i4.pipeline.model.bdd.equipe.Equipe;
import fr.epsi.i4.pipeline.model.bdd.rencontre.RencontreDetail;
import fr.epsi.i4.pipeline.model.bdd.score.JeuMatch;
import fr.epsi.i4.pipeline.model.bdd.score.PointMatch;
import fr.epsi.i4.pipeline.model.bdd.score.SetMatch;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.List;

/**
 * @author Thomas Kint
 */
@RestController
public class ScoreWS extends WebService {

	private static final BigDecimal minPoint = new BigDecimal(0);

	private static final BigDecimal maxPoint = new BigDecimal(5);

	private static final BigDecimal avantage = new BigDecimal(4);

	@GetMapping("/score/{idRencontre}")
	public Score getScoreRencontre(@PathVariable("idRencontre") BigDecimal idRencontre) {
		Score score = new Score();
		RencontreDetail rencontre = getEntityById(RencontreDetail.class, idRencontre);
		if (rencontre != null) {
			score = Score.fromRencontre(rencontre);
		}
		return score;
	}

	@PostMapping("/score/addPoint")
	public Score addPointRencontreEquipe(@RequestBody AjoutPoint ajoutPoint) {
		RencontreDetail rencontre = getEntityById(RencontreDetail.class, ajoutPoint.idRencontre);
		SetMatch dernierSet = rencontre.getLastSet();
		Score score = Score.fromRencontre(rencontre);
		try {
			boolean isEquipeUne = rencontre.equipeUne.idEquipe.compareTo(ajoutPoint.idEquipe) == 0;

			Equipe equipe;
			Equipe equipeDistante;
			BigDecimal pointEquipe;
			BigDecimal pointEquipeDistante;
			if (isEquipeUne) {
				equipe = rencontre.equipeUne;
				equipeDistante = rencontre.equipeDeux;
			} else {
				equipe = rencontre.equipeDeux;
				equipeDistante = rencontre.equipeUne;
			}

			// S'il n'y a pas encore de sets
			if (rencontre.sets.size() == 0) {
				rencontre.sets.add(createSet(rencontre.idRencontre));
				dernierSet = rencontre.getLastSet();
				dernierSet.jeux.add(createJeuEquipe(dernierSet.idSet, equipe.idEquipe));
				dernierSet.jeux.add(createJeuEquipe(dernierSet.idSet, equipeDistante.idEquipe));
				score = Score.fromRencontre(rencontre);
			}

			if (isEquipeUne) {
				pointEquipe = score.pointActuelEquipeUne.idPoint;
				pointEquipeDistante = score.pointActuelEquipeDeux.idPoint;
			} else {
				pointEquipe = score.pointActuelEquipeDeux.idPoint;
				pointEquipeDistante = score.pointActuelEquipeUne.idPoint;
			}

			JeuMatch dernierJeu;

			// Si l'équipe distante a l'avantage
			if (pointEquipeDistante.compareTo(avantage) > -1) {
				// On retire l'avantage
				dernierJeu = dernierSet.getDernierJeu(equipeDistante);

				deleteAvantage(dernierJeu.idJeu);
			}
			// Sinon
			else {
				// On incrémente les points de l'équipe
				dernierJeu = dernierSet.getDernierJeu(equipe);

				BigDecimal nouveauPointEquipe = pointEquipe.add(new BigDecimal(1));

				// Si on ne va pas dépasser les points max
				if (!(nouveauPointEquipe.compareTo(maxPoint) > 0)) {
					// On incrémente les points du jeu
					// Si l'équipe arrive à l'avantage et qu'il n'y a pas égalité
					if (nouveauPointEquipe.equals(avantage) && pointEquipe.compareTo(pointEquipeDistante) != 0) {
						// On fait gagner le set
						nouveauPointEquipe = maxPoint;
					}
					createPoint(dernierJeu.idJeu, nouveauPointEquipe);

					// Si les points arrivent au max ou à l'avantage et qu'il n'y a pas égalité
					if (nouveauPointEquipe.compareTo(maxPoint) == 0) {
						// Si le set arrive à terme
						if (isSetGagnant(score.getDernierSet(), isEquipeUne)) {
							// On créé un nouveau set
							System.out.println("Nouveau set");
							dernierSet = createSet(rencontre.idRencontre);
						}
						// On créé un nouveau jeu
						System.out.println("Nouveau jeu");
						createJeuEquipe(dernierSet.idSet, equipe.idEquipe);
						createJeuEquipe(dernierSet.idSet, equipeDistante.idEquipe);
					}
				}
			}
		} catch (MDException e) {
			e.printStackTrace();
		}

		return getScoreRencontre(rencontre.idRencontre);
	}

	private boolean isSetGagnant(Set set, boolean isEquipeUne) {
		int scoreEquipe;
		int scoreEquipeDistante;
		if (isEquipeUne) {
			scoreEquipe = set.jeuxEquipeUne;
			scoreEquipeDistante = set.jeuxEquipeDeux;
		} else {
			scoreEquipe = set.jeuxEquipeDeux;
			scoreEquipeDistante = set.jeuxEquipeUne;
		}
		// S'il y a plus de deux jeux d'écart
		return scoreEquipe + 1 > 5 && scoreEquipe + 1 - scoreEquipeDistante > 1;
	}

	private void deleteAvantage(BigDecimal idJeu) throws MDException {
		MDSelectBuilder selectBuilder = new MDSelectBuilder().from(PointMatch.class);
		selectBuilder.where(PointMatch.idJeuFieldName, MDConditionOperator.EQUAL, idJeu);
		selectBuilder.and(PointMatch.idPointEnumFieldName, MDConditionOperator.EQUAL, avantage);

		String query = selectBuilder.build();

		ResultSet resultSet = getMiniDAO().executeQuery(query);

		PointMatch avantagePointMatch = getMiniDAO().mapResultSetToEntity(resultSet, PointMatch.class);

		getMiniDAO().delete().deleteEntity(avantagePointMatch);
	}

	private PointMatch createPoint(BigDecimal idJeu, BigDecimal point) throws MDException {
		PointMatch pointMatch = new PointMatch();
		pointMatch.idJeuMatch = idJeu;
		pointMatch.idPointEnum = point;
		if (getMiniDAO().create().createEntity(pointMatch)) {
			MDCondition conditionIdJeu = new MDCondition(PointMatch.idJeuFieldName, MDConditionOperator.EQUAL, idJeu);
			MDCondition condition = new MDCondition(PointMatch.idPointEnumFieldName, MDConditionOperator.EQUAL, point, MDConditionLink.AND, conditionIdJeu);
			List<PointMatch> pointMatches = getMiniDAO().read().getEntities(PointMatch.class, condition);
			if (pointMatches.size() > 0) {
				pointMatch = pointMatches.stream().max(Comparator.comparing(PointMatch::getIdPointMatch)).get();
			}
		}
		return pointMatch;
	}

	private JeuMatch createJeuEquipe(BigDecimal idSet, BigDecimal idEquipe) throws MDException {
		JeuMatch nouveauJeu = new JeuMatch();
		nouveauJeu.idSet = idSet;
		nouveauJeu.idEquipe = idEquipe;
		// Création du nouveau jeu
		if (getMiniDAO().create().createEntity(nouveauJeu)) {
			// Récupération du nouveau jeu
			MDCondition conditionIdSet = new MDCondition(JeuMatch.idSetFieldName, MDConditionOperator.EQUAL, idSet);
			MDCondition condition = new MDCondition(JeuMatch.idEquipeFieldName, MDConditionOperator.EQUAL, idEquipe, MDConditionLink.AND, conditionIdSet);
			List<JeuMatch> jeuMatches = getMiniDAO().read().getEntities(JeuMatch.class, condition);
			if (jeuMatches.size() > 0) {
				nouveauJeu = jeuMatches.stream().max(Comparator.comparing(JeuMatch::getIdJeu)).get();
				nouveauJeu.points.add(createPoint(nouveauJeu.idJeu, minPoint));
			}
		}
		return nouveauJeu;
	}

	private SetMatch createSet(BigDecimal idRencontre) throws MDException {
		SetMatch nouveauSet = new SetMatch();
		nouveauSet.idRencontre = idRencontre;
		// Création du nouveau set
		if (getMiniDAO().create().createEntity(nouveauSet)) {
			// Récupération du nouveau set
			MDCondition condition = new MDCondition(SetMatch.idRencontreFieldName, MDConditionOperator.EQUAL, idRencontre);
			List<SetMatch> setMatches = getMiniDAO().read().getEntities(SetMatch.class, condition);
			if (setMatches.size() > 0) {
				nouveauSet = setMatches.stream().max(Comparator.comparing(SetMatch::getIdSet)).get();
			}
		}
		return nouveauSet;
	}
}
