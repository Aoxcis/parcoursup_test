/* Copyright 2018 © Ministère de l'Enseignement Supérieur, de la Recherche et de
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
package fr.parcoursup.algos.bacasable.ordreappel;

import fr.parcoursup.algos.exceptions.VerificationException;
import fr.parcoursup.algos.ordreappel.algo.GroupeClassement;
import fr.parcoursup.algos.ordreappel.algo.VoeuClasse;

public class ExempleA6 extends ExempleOrdreAppel {

    @Override
    public String nom() {
        return "exemple_A6";
    }

    @Override
    GroupeClassement initialise() throws VerificationException {

        GroupeClassement groupe = new GroupeClassement(0, 10, 95);

        for (int i = 1; i <= 100; i++) {
            groupe.ajouterVoeu(new VoeuClasse(i, i, false, true));
        }

        for (int i = 101; i <= 111; i++) {
            groupe.ajouterVoeu(new VoeuClasse(i, i, true, false));
        }

        return groupe;

    }

}
