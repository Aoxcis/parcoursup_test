package fr.parcoursup.algos.propositions;

import fr.parcoursup.algos.donnees.ConnecteurSQL;
import fr.parcoursup.algos.propositions.donnees.ConnecteurDonneesPropositionsSQL;
import fr.parcoursup.algos.propositions.donnees.testConnecteurDonneesPropositionsSQL.TestConnecteurDonneesPropositionsSQL;
import org.junit.Test;
import org.mockito.Mockito;

import static fr.parcoursup.algos.donnees.ConnecteurSQL.ADMISSIONS_TABLE_SORTIE;

public class TestEnvoiPropositions extends TestConnecteurDonneesPropositionsSQL {

    public TestEnvoiPropositions() {
        super(TestEnvoiPropositions.class.getSimpleName());
    }

    @Test
    public void test_envoi_propositons_jeu_de_tests_doit_peupler_table_propositions() throws Exception {


        try (ConnecteurSQL connecteurSQL
                     = getConnecteurDonneesProd()) {

            ConnecteurDonneesPropositionsSQL connecteurDonneesPropositions
                    = Mockito.spy(new ConnecteurDonneesPropositionsSQL(connecteurSQL.connection()));
            Mockito.when(connecteurDonneesPropositions.getNbJoursCampagne()).thenReturn(30);

            setValeurFlagInterruptionFluxDonnees(1);
            EnvoiPropositions envoi = new EnvoiPropositions(
                    connecteurDonneesPropositions,
                    connecteurDonneesPropositions);

            int nombreLignesTable_A_ADM_PROP_AvantExportation = this.getConnection().getRowCount(ADMISSIONS_TABLE_SORTIE);
            envoi.execute(false);
            int nombreLignesTable_A_ADM_PROP_ApresExportation = this.getConnection().getRowCount(ADMISSIONS_TABLE_SORTIE);
            assertTrue(nombreLignesTable_A_ADM_PROP_AvantExportation < nombreLignesTable_A_ADM_PROP_ApresExportation);

        }
    }


}
