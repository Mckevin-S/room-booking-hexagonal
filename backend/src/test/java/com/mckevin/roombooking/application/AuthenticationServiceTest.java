package com.mckevin.roombooking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mckevin.roombooking.support.Agency;
import com.mckevin.roombooking.support.Fixtures;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

    private final Agency agency = new Agency();

    @Test
    void ouvreUneSessionAvecLeBonMotDePasse() {
        var session = agency.authentication.login("  Camille@Demo.fr ", "demo1234");
        assertThat(session.account().id()).isEqualTo(Fixtures.CAMILLE_ID);
        assertThat(session.token().value()).isEqualTo("jeton-1");
    }

    @Test
    void retrouveLeCompteDuCollaborateurConnecte() {
        assertThat(agency.authentication.me(Fixtures.HUGO).email()).isEqualTo("hugo@demo.fr");
        assertThatThrownBy(() -> agency.authentication.me(new Actor(com.mckevin.roombooking.domain.employee.EmployeeId.of(99),
                "Inconnu", false))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void refuseUnMotDePasseFauxOuUnCompteInconnu() {
        assertThatThrownBy(() -> agency.authentication.login("camille@demo.fr", "mauvais"))
                .isInstanceOf(InvalidCredentialsException.class).hasMessage("E-mail ou mot de passe incorrect.");
        assertThatThrownBy(() -> agency.authentication.login("inconnu@demo.fr", "demo1234"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> agency.authentication.login(null, null)).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> agency.authentication.login("camille@demo.fr", null))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
