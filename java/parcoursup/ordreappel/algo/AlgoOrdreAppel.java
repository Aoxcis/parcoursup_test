
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
package parcoursup.ordreappel.algo;

import parcoursup.verification.VerificationsResultatsAlgoOrdreAppel;

public class AlgoOrdreAppel {

    /* la boucle principale de calcul des ordres d'appels. 
        Renvoit une exception en cas de problème. */
    public static AlgoOrdreAppelSortie calculerOrdresAppels(AlgoOrdreAppelEntree data) throws Exception {

        AlgoOrdreAppelSortie resultat = new AlgoOrdreAppelSortie();

        /* calcul de l'ordre d'appel de chaque groupe de classement */
        for (GroupeClassement ga : data.groupesClassements) {
            resultat.ordresAppel.put(ga.C_GP_COD, ga.calculerOrdreAppel());
        }

        /* vérification avant retoru des resultats */
        VerificationsResultatsAlgoOrdreAppel verif = new VerificationsResultatsAlgoOrdreAppel();
        verif.verifier(data, resultat);
        
                
        return resultat;

    }

    private AlgoOrdreAppel() {
    }

}
