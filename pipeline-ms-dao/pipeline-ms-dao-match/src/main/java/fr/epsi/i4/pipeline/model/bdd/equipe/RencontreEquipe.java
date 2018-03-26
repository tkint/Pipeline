package fr.epsi.i4.pipeline.model.bdd.equipe;

import com.thomaskint.minidao.annotation.MDEntity;
import com.thomaskint.minidao.annotation.MDField;
import com.thomaskint.minidao.annotation.MDManyToOne;
import com.thomaskint.minidao.enumeration.MDLoadPolicy;
import fr.epsi.i4.pipeline.model.bdd.tournoi.Rencontre;

import java.math.BigDecimal;

/**
 * @author Thomas Kint
 */
@MDEntity(tableName = "rencontre_equipe")
public class RencontreEquipe {

	public static final String idRencontreFieldName = "id_rencontre";

	public static final String idEquipeFieldName = "id_equipe";

	public BigDecimal idRencontreEquipe;

	@MDField(fieldName = idRencontreFieldName)
	public BigDecimal idRencontre;

	@MDManyToOne(fieldName = idRencontreFieldName, target = Rencontre.class, loadPolicy = MDLoadPolicy.HEAVY)
	public Rencontre rencontre;

	@MDField(fieldName = idEquipeFieldName)
	public BigDecimal idEquipe;

	@MDManyToOne(fieldName = idEquipeFieldName, target = Equipe.class, loadPolicy = MDLoadPolicy.HEAVY)
	public Equipe equipe;
}
