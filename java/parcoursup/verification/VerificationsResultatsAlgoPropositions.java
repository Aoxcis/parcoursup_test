/*
    Copyright 2018 © Ministère de l'Enseignement Supérieur , de la Recherche et de
l'Innovation,
    Hugo Gimbert (hugo.gimbert@enseignementsup.gouv.fr)

    This file is part of Algorithmes-de-parcoursup.

    Algorithmes-de-parcoursup is free software: you can redistribute it and/or modify
    it under the terms of the Affero GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Algorithmes-de-parcoursup is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Affero GNU General Public License for more details.

    You should have received a copy of the Affero GNU General Public License
    along with Algorithmes-de-parcoursup.  If not, see <http://www.gnu.org/licenses/>.

 */
package parcoursup.verification;

import parcoursup.exceptions.VerificationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import parcoursup.propositions.algo.AlgoPropositionsEntree;
import parcoursup.propositions.algo.AlgoPropositionsSortie;
import parcoursup.propositions.algo.GroupeAffectation;
import parcoursup.propositions.algo.GroupeInternat;
import parcoursup.propositions.algo.Voeu;

/* Permet de vérifier un certain nombre de propriétés statiques
    des sorties de l'algorithme. Sans garantir la correction du code,
    cela garantit que les résultats produits satisfont les principales propriétés
    énoncées dans le document.
    Des tests complémentaires sont effectués en base par des scripts PL/SQL.

En production, certains cas particuliers adviennent, par exemple:

    * réinsertion de voeuxEnAttente suite à des demandes candidats d'annuler une démission sur un voeu,
        ce sont les voeuxEnAttente pour lesquels estAnnulationDemission() est true.
        Entretemps, des candidats moins bien positionnés dans l'ordre d'appel
        peuvent avoir reçu une proposition, ce cas est pris en compte dans P1.

    * modifications de classements par les formations (rare),
        suite à erreurs de saisie. Dans ce cas les candidats ayant déjà
        bénéficié d'une proposition la conserve et prennent la tête de l'ordre
        d'appel. L'ordre d'appel des candidats restants est recalculé sur la base
        du nouveau classement.

Les vérifications tiennet comptent de ces cas particuliers (e.g. ajout du flag estAnnulationDemission().

Afin de ne pas bloquer l'envoi quotidien des propositions quand un nouveau cas particulier
est découvert, on implémente un mode de vérification non-bloquant:
en cas de violation d'une propriété, le groupe de classement
et les éventuels internats associés sont exclus de la génération des propositions.
On calcule la zone d'influence de l'erreur, c'est-à-dire la composante
connexe contenant la formation ou l'internat concerné dans le graphe dont les
sommets sont ces formations et internats et les arètes sont induites par les voeuxEnAttente
en attente.


 */
public class VerificationsResultatsAlgoPropositions {

    private static final Logger LOGGER = Logger.getLogger(VerificationsResultatsAlgoPropositions.class.getSimpleName());

    /* Données sur les voeux */
    Map<GroupeAffectation, List<Voeu>> voeuxParFormation = new HashMap<>();
    Map<GroupeInternat, List<Voeu>> voeuxParInternat = new HashMap<>();
    Map<GroupeAffectation, Set<Integer>> initialementAffectesFormations = new HashMap<>();
    Map<GroupeInternat, Set<Integer>> initialementAffectesInternats = new HashMap<>();
    Map<GroupeAffectation, Set<Integer>> actuellementAffectesFormations = new HashMap<>();
    Map<GroupeInternat, Set<Integer>> actuellementAffectesInternats = new HashMap<>();
    Map<GroupeAffectation, Integer> rangsDernierAppeleParFormation = new HashMap<>();
    Set<GroupeAffectation> formationsAvecRangLimiteEffectif = new HashSet<>();

    /* Vérifie les résultats du calcul et supprime de la sortie les groupes
    ne passant pas la vérification. En cas d'alerte ou d'avertissement,
    positionne les flags correspondants dans sortie.
     */
    public void verifier(AlgoPropositionsEntree entree, AlgoPropositionsSortie sortie) throws VerificationException {
        LOGGER.log(Level.INFO, "Vérification des propriétés attendues des propositions "
                + "pour les {0} groupes d''affectation", entree.groupesAffectations.size());

        int step = Integer.max(1, entree.groupesAffectations.size() / 50);
        int count = 0;

        groupesNonValides.clear();
        voeuxParFormation.clear();
        voeuxParInternat.clear();
        initialementAffectesFormations.clear();
        initialementAffectesInternats.clear();
        actuellementAffectesFormations.clear();
        actuellementAffectesInternats.clear();
        rangsDernierAppeleParFormation.clear();
        formationsAvecRangLimiteEffectif.clear();

        entree.groupesAffectations.values().forEach(groupe -> {
            voeuxParFormation.put(groupe, new ArrayList<>());
            initialementAffectesFormations.put(groupe, new HashSet<>());
            actuellementAffectesFormations.put(groupe, new HashSet<>());
            rangsDernierAppeleParFormation.put(groupe, 0);
        });

        entree.internats.values().forEach(internat -> {
            voeuxParInternat.put(internat, new ArrayList<>());
            initialementAffectesInternats.put(internat, new HashSet<>());
            actuellementAffectesInternats.put(internat, new HashSet<>());
        });

        sortie.voeux.forEach(v -> {

            GroupeAffectation groupe = v.groupe;
            GroupeInternat internat = v.internat;
            voeuxParFormation.get(groupe).add(v);

            if (internat != null) {
                voeuxParInternat.get(internat).add(v);
            }

            if (v.estProposition()) {
                actuellementAffectesFormations.get(groupe).add(v.id.gCnCod);
                if (internat != null) {
                    actuellementAffectesInternats.get(internat).add(v.id.gCnCod);
                }
                if (v.estPropositionDuJour()
                        && !v.estMeilleurBachelier()) {
                    int rangActuel = rangsDernierAppeleParFormation.get(v.groupe);
                    if (v.getOrdreAppel() > rangActuel) {
                        rangsDernierAppeleParFormation.put(groupe, v.getOrdreAppel());
                    }
                }
                if (v.estAffecteJoursPrecedents()) {
                    initialementAffectesFormations.get(groupe).add(v.id.gCnCod);
                    if (internat != null) {
                        initialementAffectesInternats.get(internat).add(v.id.gCnCod);
                    }
                }
            }
            if (v.ordreAppelInitial <= v.groupe.getRangLimite()
                    && (v.estProposition() || v.estDemissionAutomatiqueProposition())) {
                formationsAvecRangLimiteEffectif.add(v.groupe);
            }
        });

        for (GroupeAffectation groupe : voeuxParFormation.keySet()) {

            if (count++ % step == 0) {
                LOGGER.log(Level.INFO, "verification effectu\u00e9e de {0} groupes ", count);
            }
            try {
                verifierRespectOrdreAppelVoeuxSansInternat(groupe);
                verifierVoeuxAvecInternat(groupe);
                verifierSurcapaciteEtRemplissage(groupe);
            } catch (VerificationException e) {
                LOGGER.warning(e.getMessage());
                invalider(groupe);
            }

        }

        LOGGER.info("");
        LOGGER.log(Level.INFO, "V\u00e9rification des propri\u00e9t\u00e9s attendues des propositions dans les {0} internats", entree.internats.size());

        step = Integer.max(1, entree.internats.size() / 50);
        count = 0;

        for (GroupeInternat internat : voeuxParInternat.keySet()) {

            if (count++ % step == 0) {
                LOGGER.log(Level.INFO, "verification effectu\u00e9e de {0} internats ", count);
            }

            try {

                if (internat.getPositionAdmission() > internat.getPositionMaximaleAdmission()) {
                    alerter("Violation limite position maximale admission "
                            + internat.getPositionAdmission() + " > " + internat.getPositionMaximaleAdmission()
                            + " dans internat " + internat);
                }

                verifierRespectClassementInternat(internat);

                verifierSurcapaciteEtRemplissageInternat(internat);

            } catch (VerificationException e) {
                LOGGER.warning(e.getMessage());
                invalider(internat);
            }

        }

        if (!groupesNonValides.isEmpty()) {

            LOGGER.warning("Invalidation d'une propriété.");

            /* étend l'invalidation à tous les groupes
                pouvant être influencés par les groupes invalidés */
            clotureTransitiveDependances(groupesNonValides);

            LOGGER.log(Level.WARNING, "Suppression de {0} groupes ignor\u00e9s dans les donn\u00e9es de sortie", groupesNonValides.size());

            Collection<Voeu> propositionsAnnulees = sortie.invaliderGroupes(groupesNonValides);
            propositionsAnnulees.forEach(v
                    -> LOGGER.log(Level.WARNING, "Suppression de la proposition {0}", v)
            );

            /* si au final proposition n'est ignorée, on émet un simple avertissement */
            long nbPropositionsAnnulees = propositionsAnnulees.size();

            if (nbPropositionsAnnulees == 0) {
                LOGGER.warning("Invalidation propriété sans conséquence: aucune proposition supprimée.");
                sortie.setAvertissement();
            } else {
                LOGGER.log(Level.WARNING, sortie.getAlerteMessage());
                LOGGER.log(Level.WARNING,
                        "Invalidation propri\u00e9t\u00e9 ayant conduit \u00e0 la suppression de {0} propositions.\n"
                        + "Afin de compl\u00e9ter l''export, veuillez consulter le log,v\u00e9rifier "
                        + "les donn\u00e9es d''entr\u00e9e et relancer le calcul des propositions.", nbPropositionsAnnulees);
                sortie.setAlerte();
            }
        }
    }

    /* liste des groupes d'affectations ignorés par l'alerte */
    private final Set<GroupeAffectation> groupesNonValides = new HashSet<>();

    private static final String V1_FLOUE_PAR_V2 = " v1 floué par v2 où v1 est ";
    /*
    P1 (respect ordre appel pour les voeuxEnAttente sans internat)
    
    Si un candidat C1 précède un candidat C2 dans l'ordre d'appel d'une formation F
    et si C1 a un voeu en attente pour F sans demande d'internat
    alors C2 n'a pas de proposition pour F.
     */
    private void verifierRespectOrdreAppelVoeuxSansInternat(GroupeAffectation groupe) throws VerificationException {

        Set<Integer> initialementAffectesFormation = initialementAffectesFormations.get(groupe);
        List<Voeu> voeux = voeuxParFormation.get(groupe);

        /* on trie les voeux, le meilleur classement en tête de liste */
        voeux.sort((Voeu v1, Voeu v2) -> v1.getOrdreAppel() - v2.getOrdreAppel());

        /* on vérifie  si le voeu v1 est floué par v2, i.e. v1 aurait dû avoir une proposition
        mais ne l'a pas eue. On ne regarde que les cas où il n'y a pas de demande
        internat pour v1.
         */
        for (Voeu v1 : voeux) {
            if (v1.estEnAttenteDeProposition()
                    && !v1.avecInternatAClassementPropre()) {
                for (Voeu v2 : voeux) {
                    if (v2.estPropositionDuJour()
                            && !initialementAffectesFormation.contains(v2.id.gCnCod)
                            && v2.getOrdreAppel() > v1.getOrdreAppel()
                            && !v2.getEligibleDispositifMB() //les MB peuvent remonter dans l'ordre d'appel
                            ) {
                        alerter(
                                "Violation respect ordre appel pour"
                                + " les voeux sans demande internat"
                                + V1_FLOUE_PAR_V2
                                + v1 + " et v2 est " + v2);
                    }
                }
                break;//il suffit de vérifier pour un seul v1, puisque la liste est triée
            }
        }
    }

    /*
    P2  (respect ordre appel et classement internat pour les voeuxEnAttente avec internat)

    Si un candidat C1 précède un candidat C2
    à la fois dans l'ordre d'appel d'une formation F
    et dans un classement d'internat I
    et si C1 a un voeu en attente pour F avec internat I
    alors C2 n'a pas de proposition pour F avec internat I.

     */
    private void verifierVoeuxAvecInternat(GroupeAffectation groupe) throws VerificationException {

        List<Voeu> voeux = voeuxParFormation.get(groupe);
        Set<Integer> initialementAffectesFormation = initialementAffectesFormations.get(groupe);

        for (Voeu v1 : voeux) {
            if (v1.estEnAttenteDeProposition()
                    && v1.avecInternatAClassementPropre()) {
                GroupeInternat internat = v1.internat;
                Set<Integer> dejaAffectesInternat = initialementAffectesInternats.get(internat);
                for (Voeu v2 : voeux) {
                    if (v2.avecInternatAClassementPropre()
                            && v1.internatID() == v2.internatID()
                            && v2.estPropositionDuJour()
                            && v2.getOrdreAppel() > v1.getOrdreAppel()
                            && !v2.formationDejaObtenue()
                            && !initialementAffectesFormation.contains(v2.id.gCnCod)
                            && v2.rangInternat > v1.rangInternat
                            && !dejaAffectesInternat.contains(v2.id.gCnCod)
                            && !v2.getEligibleDispositifMB() //les MB peuvent remonter dans l'ordre d'appel
                            ) {
                        alerter(
                                "Violation respect ordre et classement "
                                + "pour les voeux avec demande internat"
                                + V1_FLOUE_PAR_V2
                                + v1 + " et v2 est  " + v2);
                    }
                }
            }
        }
    }

    /*
    P3 (respect classement internat pour les candidats avec une proposition sans internat)

    La barre d'admission est inférieure à la barre maximale d'admission.

    Si une proposition est faite alors le classement du voeu à l'internat
    est inférieur à la barre d'admission.

    Si un candidat C1 a un voeu en attente pour une formation F avec demande d'internat I
    et une proposition acceptée ou en attente de réponse de sa part pour la formation F
    sans demande d'internat,
    et si C2 est un candidat moins bien classé que C1 à l'internat I
    et si une des nouvelles propositions du jour offre l'internat I à C2
    alors que C2 n'avait pas de propositions pour I auparavant
    alors une des nouvelles propositions du jour offre la formation F et l'internat I à C1.

     */
    private void verifierRespectClassementInternat(GroupeInternat internat) throws VerificationException {

        List<Voeu> voeux = voeuxParInternat.get(internat);
        Set<Integer> initialementAffectesInternat = initialementAffectesInternats.get(internat);

        for (Voeu v1 : voeux) {
            if (!v1.avecInternatAClassementPropre()) {
                alerter("Voeu dans internat " + internat
                        + " avec classement propre sans classement internat ");
            }
        }

        voeux.sort((Voeu v1, Voeu v2) -> v1.rangInternat - v2.rangInternat);

        for (Voeu v1 : voeux) {
            Set<Integer> actuellementAffectesFormation = actuellementAffectesFormations.get(v1.groupe);
            if (v1.estEnAttenteDeProposition()
                    && actuellementAffectesFormation.contains(v1.id.gCnCod)) {
                for (Voeu v2 : voeux) {
                    if (v2.estPropositionDuJour()
                            && v2.rangInternat > v1.rangInternat
                            && !initialementAffectesInternat.contains(v2.id.gCnCod)) {
                        alerter("Violation respect ordre appel pour les attributions d'internat"
                                + "pour les voeux avec demande internat"
                                + V1_FLOUE_PAR_V2
                                + v1 + " et v2 est  " + v2);
                    }
                }
                break;//une seule passe suffit car les voeuxEnAttente sont triés
            }
        }
    }

    /*

    P4  (remplissage maximal des formations dans le respect des positions d'admission à l'internat)

    Le nombre de propositions doit être inférieur au nombre de places vacantes.

    Si le nombre de nouvelles propositions dans une groupe est strictement inférieur
    au nombre de places vacantes dans cette groupe, alors tous les voeux en attente
    pour cette groupe sont des voeux avec internat,
    effectués par des candidats dont le rang de classement dans l'internat correspondant
    est strictement supérieur à la position d'admission dans cet internat.
     */
    private void verifierSurcapaciteEtRemplissage(GroupeAffectation groupe) throws VerificationException {

        List<Voeu> voeux = voeuxParFormation.get(groupe);
        Set<Integer> initialementAffectesFormation = initialementAffectesFormations.get(groupe);
        Set<Integer> actuellementAffectesFormation = actuellementAffectesFormations.get(groupe);

        int nbNouveauxArrivants = actuellementAffectesFormation.size() - initialementAffectesFormation.size();
        boolean surCapacite = (actuellementAffectesFormation.size() > groupe.nbRecrutementsSouhaite);
        boolean sousCapacite = (actuellementAffectesFormation.size() < groupe.nbRecrutementsSouhaite);

        if (surCapacite
                && (nbNouveauxArrivants > 0)
                && !formationsAvecRangLimiteEffectif.contains(groupe)) {
            alerter("ajout de propositions dans une formation en surcapacité" + groupe);
        }

        if (sousCapacite) {
            for (Voeu v : voeux) {
                if (v.estEnAttenteDeProposition()
                        && !v.avecInternatAClassementPropre()) {
                    alerter("souscapacité formation " + groupe
                            + " sans classement internat"
                            + " compensable par le voeu " + v);
                }
            }
        }

    }

    /*
    P5  (remplissage maximal des internats dans le respect des ordres d'appel)

    Le nombre de propositions doit être inférieur au nombre de places vacantes.

    Si le nombre de nouvelles propositions dans un internat I est strictement inférieur
    au nombre de places vacantes dans I, alors tous les voeuxEnAttente en attente
    pour une formation F et demande d'internat I sont
    soit effectués par des candidats
    dont le classement à l'internat I est strictement supérieur
    à la position d'admission dans I ou bien situés plus bas dans l'ordre d'appel de F
    que tous les candidats ayant reçu une proposition de F ce jour là.
     */
    private void verifierSurcapaciteEtRemplissageInternat(GroupeInternat internat) throws VerificationException {

        Set<Integer> initialementAffectesInternat = initialementAffectesInternats.get(internat);
        Set<Integer> actuellementAffectesInternat = actuellementAffectesInternats.get(internat);

        int nbNouveauxArrivants = actuellementAffectesInternat.size() - initialementAffectesInternat.size();
        boolean surCapacite = (actuellementAffectesInternat.size() > internat.capacite);

        if (surCapacite && (nbNouveauxArrivants > 0)) {
            alerter("ajout de propositions dans un internat en surcapacité" + internat);
        }

    }

    /*
    P6  (maximalité des positions d'admission)

    Pour tout internat, la position d'admission est inférieure
    ou égale à la position maximale d'admission.
    Dans le cas où elle est strictement inférieure,
    augmenter d'une unité la position d'admission
    entrainerait une surcapacité d'un des internats.

    Non-implémenté.

     */
 /* ignore un groupe et ses dépendances */
    private void alerter(String message) throws VerificationException {
        LOGGER.severe(message);
        throw new VerificationException(message);
    }

    /* ignore un internat et ses dépendances */
    private void invalider(GroupeInternat internat) {
        groupesNonValides.addAll(internat.groupesConcernes);
    }

    /* ignore un groupe et ses dépendances */
    private void invalider(GroupeAffectation ga) {
        groupesNonValides.add(ga);
    }

    /* Etant donne une liste initiale de groupes à ignorer,
    il convient également d'ignorer tous les internats liés à ces groupes
    et tous les groupes liés à ces internats, etc...
    Ce calcul pourrait être évité si la notion d'établissement
    était intégrée au modèle Java: l'influence d'un groupe ne peut aller
    au delà de son établissement.
     */
    private static void clotureTransitiveDependances(
            Collection<GroupeAffectation> groupesAIgnorer) {
        List<GroupeAffectation> aTraiter = new ArrayList<>();
        aTraiter.addAll(groupesAIgnorer);

        /* groupes deja pris en compte dans le calcul de la cloture transitive */
        Set<GroupeAffectation> traites = new HashSet<>();

        while (!aTraiter.isEmpty()) {
            GroupeAffectation ga = aTraiter.get(0);
            for (Voeu v : ga.voeuxEnAttente) {
                if (v.avecInternatAClassementPropre()) {
                    GroupeInternat internat = v.internat;
                    for (GroupeAffectation ng : internat.groupesConcernes) {
                        if (!traites.contains(ng)) {
                            aTraiter.add(ng);
                        }
                    }
                }
            }
            aTraiter.remove(ga);
            traites.add(ga);
        }

        groupesAIgnorer.addAll(traites);

    }

}
