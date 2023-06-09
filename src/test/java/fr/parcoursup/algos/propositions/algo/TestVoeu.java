package fr.parcoursup.algos.propositions.algo;

import fr.parcoursup.algos.exceptions.VerificationException;
import fr.parcoursup.algos.exceptions.VerificationExceptionMessage;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.parcoursup.algos.propositions.algo.Voeu.*;
import static org.junit.Assert.*;

public class TestVoeu {

    @Test
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<Voeu> constructor = Voeu.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void constructeur_doit_copier() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v1 = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false, false, false);
        Voeu v2 = new Voeu(v1);
        assertTrue(
            v1 != v2
            && v1.equals(v2)
        );
    }

    @Test
    public void estDemissionAutomatiqueProposition_doit_retournerTrue_si_StatutVoeuEstDemissionAutoProposition()
            throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.REP_AUTO_REFUS_PROPOSITION, false, false, false);
        assertTrue(v.estDemissionAutomatiqueProposition());
    }

    @Test
    public void estDemissionAutomatiqueProposition_doit_retournerFalse_si_StatutVoeuEstEnAttenteProposition()
            throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        assertFalse(v.estDemissionAutomatiqueProposition());
    }

    @Test
    public void refuserAutomatiquement_doit_echouer_si_estAffecteHorsPP() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.PROPOSITION_JOURS_PRECEDENTS_ACCEPTEE, true);
        VerificationException exception = assertThrows(VerificationException.class, v::refuserAutomatiquementParApplicationRepondeurAutomatique);
        assertSame(VerificationExceptionMessage.VOEU_HORS_PP_NON_REFUSABLE_AUTOMATIQUEMENT, exception.exceptionMessage);
    }

    @Test
    public void refuserAutomatiquement_doit_echouer_si_repondeurNonActive() throws VerificationException {
        // True branch coverage de la ligne 133
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        v.setRepondeurActive(false);
        VerificationException exception = assertThrows(VerificationException.class, v::refuserAutomatiquementParApplicationRepondeurAutomatique);
        assertSame(VerificationExceptionMessage.VOEU_REFUS_AUTOMATIQUE_IMPOSSIBLE, exception.exceptionMessage);
    }

    @Test
    public void refuserAutomatiquement_doit_echouer_si_statutVoeuEstRefus()
            throws VerificationException {
        // True branch coverage de la ligne 135
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1,
                StatutVoeu.REP_AUTO_REFUS_PROPOSITION,
                false);
        v.setRepondeurActive(true);
        VerificationException exception = assertThrows(VerificationException.class, v::refuserAutomatiquementParApplicationRepondeurAutomatique);
        assertSame(VerificationExceptionMessage.VOEU_REFUS_AUTOMATIQUE_IMPOSSIBLE, exception.exceptionMessage);
    }

    @Test
    public void proposer_doit_echouer_si_statutVoeuNestPasEnAttenteDeProposition() throws VerificationException {
        // True branch coverage de la ligne 144
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.PROPOSITION_DU_JOUR, false);
        VerificationException exception = assertThrows(VerificationException.class, v::proposer);
        assertSame(VerificationExceptionMessage.VOEU_PROPOSITION_IMPOSSIBLE, exception.exceptionMessage);
    }

    @Test
    public void constructeur_doit_echouer_si_horsPPEtEnAttenteProposition() throws VerificationException {
        // True branch coverage de la ligne 180
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.PROPOSITION_DU_JOUR, true);
        });
        assertSame(VerificationExceptionMessage.VOEU_INCONSISTENCE_STATUT_HORS_PP, exception.exceptionMessage);
    }

    @Test
    public void constructeur_doit_echouer_si_ordreAppelNegatif() throws VerificationException {
        // True branch coverage de la ligne 184
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            Voeu v = new Voeu(0, false, g.id, -1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        });
        assertSame(VerificationExceptionMessage.VOEU_RANGS_NEGATIFS, exception.exceptionMessage);
    }

    @Test
    public void constructeur_doit_echouer_si_ordreAppelZeroEtNonAffecteJoursPrecedents()
            throws VerificationException {
        // True branch coverage de la ligne 187
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            Voeu v = new Voeu(0, false, g.id, 0, 0, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        });
        assertSame(VerificationExceptionMessage.VOEU_ORDRE_APPEL_MANQUANT, exception.exceptionMessage);
    }

    @Test
    public void constructeurInternat_doit_echouer_si_horsPPEtEnAttenteProposition() throws VerificationException {
        // True branch coverage de la ligne 213
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        GroupeInternat gi = new GroupeInternat(new GroupeInternatUID(1, 0, 0), 1);
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            Voeu v = new Voeu(0, g.id, 1, 1, gi.id, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, true);
        });
        assertSame(VerificationExceptionMessage.VOEU_INCONSISTENCE_STATUT_HORS_PP, exception.exceptionMessage);
    }

    @Test
    public void constructeurInternat_doit_echouer_si_ordreAppelNegatif() throws VerificationException {
        // True branch coverage de la ligne 217
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        GroupeInternat gi = new GroupeInternat(new GroupeInternatUID(1, 0, 0), 1);
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            Voeu v = new Voeu(0, g.id, -1, 1, gi.id, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        });
        assertSame(VerificationExceptionMessage.VOEU_RANGS_NEGATIFS, exception.exceptionMessage);
    }

    @Test
    public void constructeurInternat_doit_echouer_si_ordreAppelZeroEtNonAffecteJoursPrecedents()
            throws VerificationException {
        // True branch coverage de la ligne 220
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        GroupeInternat gi = new GroupeInternat(new GroupeInternatUID(1, 0, 0), 1);
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            Voeu v = new Voeu(0, g.id, 0, 0, gi.id, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        });
        assertSame(VerificationExceptionMessage.VOEU_ORDRE_APPEL_MANQUANT, exception.exceptionMessage);
    }

    @Test
    public void estDemissionAutomatiqueVoeuAttente_doit_retournerTrueSiStatutVoeuEstDemissionAutoVoeuAttente()
            throws VerificationException {
        // Coverage de la ligne 96
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.REP_AUTO_DEMISSION_ATTENTE, false);
        assertTrue(v.estDemissionAutomatiqueVoeuAttenteParRepondeurAutomatique());
    }

    @Test
    public void refuserAutomatiquement_doit_changerStatutSiAffecteJoursPrecedents() throws VerificationException {
        // True branch coverage de la ligne 131
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.PROPOSITION_JOURS_PRECEDENTS_ACCEPTEE, false);
        v.refuserAutomatiquementParApplicationRepondeurAutomatique();
        assertEquals(Voeu.StatutVoeu.REP_AUTO_REFUS_PROPOSITION, v.statut);
    }

    @Test
    public void refuserAutomatiquement_doit_changerStatutEnRefusAutoPropositionSiAcceptationAutomatique()
            throws VerificationException {
        // False branch coverage de la ligne 135
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.REP_AUTO_ACCEPTE, false);
        v.setRepondeurActive(true);
        v.refuserAutomatiquementParApplicationRepondeurAutomatique();
        assertEquals(StatutVoeu.REP_AUTO_REFUS_PROPOSITION, v.statut);
    }

    @Test
    public void proposer_doit_changerStatutSiRepondeurActive() throws VerificationException {
        // True branch coverage de la ligne 148
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        v.setRepondeurActive(true);
        v.proposer();
        assertEquals(Voeu.StatutVoeu.REP_AUTO_ACCEPTE, v.statut);
    }

    @Test
    public void getRepondeurActive_doit_retournerRepondeurActive() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        v.setRepondeurActive(true);
        assertTrue(v.getRepondeurActive());
    }

    @Test
    public void getRangListAttente_doit_retournerRangListeAttente() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        assertEquals(v.getRangListeAttente(), v.rangListeAttente);
        v.setRangListeAttente(2);
        assertEquals(2, v.getRangListeAttente());
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void toString_doit_reussir_avecDemandeInternat() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        GroupeInternat gi = new GroupeInternat(new GroupeInternatUID(1, 0, 0), 1);
        Voeu v = new Voeu(0, g.id, 1, 1, gi.id, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        assertTrue(v.toString().contains("avec demande internat"));
    }

    @Test
    public void simulerAcceptation_doit_changerStatut() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        v.simulerAcceptation();
        assertSame(Voeu.StatutVoeu.PROPOSITION_JOURS_PRECEDENTS_ACCEPTEE, v.statut);
    }

    @Test
    public void simulerEnAttente_doit_changerStatut() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        GroupeInternat gi = new GroupeInternat(new GroupeInternatUID(1, 0, 0), 1);
        
        Voeu v1 = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        v1.simulerEnAttenteSaufSiNonClasse();
        assertSame(Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, v1.statut);

        Voeu v2 = new Voeu(0, g.id, 1, 1, gi.id, 0, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        v2.simulerEnAttenteSaufSiNonClasse();
        assertSame(Voeu.StatutVoeu.NON_CLASSE, v2.statut);
    }

    @Test
    public void equals_doit_echouer_si_objetsDifferents() throws VerificationException {
        Parametres p = new Parametres(59, 60,90);
        GroupeAffectation g = new GroupeAffectation(1, new GroupeAffectationUID(0, 0, 0), 1, 1, p);
        Voeu v = new Voeu(0, false, g.id, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        Throwable exception = assertThrows(ClassCastException.class, () -> v.equals(""));
        assertTrue(exception.getMessage().contains("Test d'égalité imprévu"));
    }

    @Test
    public void deux_voeux_de_meme_id_sont_consideres_identiques_dans_les_containers() throws VerificationException {
        GroupeAffectationUID gid = new GroupeAffectationUID(0, 0, 0);
        Voeu v1 = new Voeu(0, false, gid, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        Voeu v2 = new Voeu(0, false, gid, 1, 1, 1, Voeu.StatutVoeu.EN_ATTENTE_DE_PROPOSITION, false);
        List<Voeu> l = new ArrayList<>();
        l.add(v1);
        assertTrue(l.contains(v2));

        Set<Voeu> s = new HashSet<>();
        s.add(v1);
        s.add(v2);
        assertEquals(1, s.size());
        s.remove(v1);
        assertTrue(s.isEmpty());

    }

}
