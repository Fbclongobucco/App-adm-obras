package com.longobuccodev.app_adm_obras.infra.adapters.repository;

import com.longobuccodev.app_adm_obras.core.domain.User;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.repositories.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(UserRepositoryAdapter.class)
class UserRepositoryAdapterTest {

    @Autowired
    private UserRepositoryAdapter adapter;

    @Autowired
    private UserJpaRepository jpaRepository;

    @Test
    @DisplayName("save deve persistir o usuario com as roles na tabela user_role")
    void saveShouldPersistUserWithRoles() {
        User user = new User(null, "Maria Souza", "maria@email.com", true);
        user.addRole(Role.ADMIN);
        user.addRole(Role.OPERADOR);

        User saved = adapter.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(jpaRepository.findById(saved.getId())).isPresent();

        User reloaded = adapter.findById(saved.getId());
        assertThat(reloaded.getName()).isEqualTo("Maria Souza");
        assertThat(reloaded.getEmail()).isEqualTo("maria@email.com");
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.OPERADOR);
    }

    @Test
    @DisplayName("findByEmail deve ignorar as diferencas de caixa nos dois sentidos")
    void findByEmailShouldIgnoreCase() {
        adapter.save(new User(null, "Joao da Silva", "joao@email.com", true));

        assertThat(adapter.findByEmail("joao@email.com")).isNotNull();
        assertThat(adapter.findByEmail("JOAO@EMAIL.COM")).isNotNull();
        assertThat(adapter.findByEmail("nao-existe@email.com")).isNull();
    }

    @Test
    @DisplayName("findAll deve retornar todos os usuarios persistidos")
    void findAllShouldReturnEveryUser() {
        adapter.save(new User(null, "Ana Lima", "ana@email.com", true));
        adapter.save(new User(null, "Bruno Alves", "bruno@email.com", false));

        assertThat(adapter.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("update deve sobrescrever os campos e substituir as roles")
    void updateShouldOverwriteFieldsAndRoles() {
        User user = new User(null, "Ana Lima", "ana@email.com", true);
        user.addRole(Role.ADMIN);
        User saved = adapter.save(user);

        User changed = new User(saved.getId(), "Ana Maria Lima", "ana.maria@email.com", false);
        changed.addRole(Role.OPERADOR);
        adapter.update(changed);

        User reloaded = adapter.findById(saved.getId());
        assertThat(reloaded.getName()).isEqualTo("Ana Maria Lima");
        assertThat(reloaded.getEmail()).isEqualTo("ana.maria@email.com");
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getRoles()).containsExactly(Role.OPERADOR);
    }

    @Test
    @DisplayName("deleteById deve remover o usuario")
    void deleteByIdShouldRemoveUser() {
        User saved = adapter.save(new User(null, "Carla Dias", "carla@email.com", true));

        adapter.deleteById(saved.getId());

        assertThat(adapter.findById(saved.getId())).isNull();
    }

    @Test
    @DisplayName("findById deve retornar null quando o id nao existir")
    void findByIdShouldReturnNullWhenMissing() {
        assertThat(adapter.findById(UUID.randomUUID())).isNull();
    }

    @Test
    @DisplayName("update nao deve lancar excecao quando o usuario nao existir")
    void updateShouldIgnoreMissingUser() {
        User orphan = new User(UUID.randomUUID(), "Fantasma", "fantasma@email.com", true);
        orphan.addRole(Role.OPERADOR);

        adapter.update(orphan);

        assertThat(adapter.findById(orphan.getId())).isNull();
    }

    @Test
    @DisplayName("findAll paginado deve retornar a fatia correta com os metadados")
    void paginatedFindAllShouldReturnSliceWithMetadata() {
        saveUsers(5);

        Page<User> firstPage = adapter.findAll(PageRequest.of(0, 2));

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.page()).isZero();
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.totalElements()).isEqualTo(5);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("cada pagina deve trazer elementos distintos e a ultima deve ser a final")
    void paginatedFindAllShouldNotRepeatElements() {
        saveUsers(5);

        java.util.Set<UUID> seen = new java.util.HashSet<>();
        for (int page = 0; page < 3; page++) {
            adapter.findAll(PageRequest.of(page, 2)).content().forEach(u -> seen.add(u.getId()));
        }
        assertThat(seen).hasSize(5);

        Page<User> lastPage = adapter.findAll(PageRequest.of(2, 2));
        assertThat(lastPage.content()).hasSize(1);
        assertThat(lastPage.hasNext()).isFalse();
        assertThat(lastPage.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("uma pagina alem do ultimo elemento deve vir vazia e sem hasNext")
    void paginatedFindAllShouldReturnEmptyPageBeyondRange() {
        saveUsers(3);

        Page<User> beyond = adapter.findAll(PageRequest.of(9, 2));

        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.isEmpty()).isTrue();
        assertThat(beyond.totalElements()).isEqualTo(3);
        assertThat(beyond.hasNext()).isFalse();
    }

    @Test
    @DisplayName("findAll paginado sem dados deve devolver pagina vazia")
    void paginatedFindAllShouldReturnEmptyPageWithoutData() {
        Page<User> page = adapter.findAll(PageRequest.firstPage());

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("findAll paginado deve manter as roles dos usuarios mapeados")
    void paginatedFindAllShouldMapRoles() {
        User user = new User(null, "Diego Reis", "diego@email.com", true);
        user.addRole(Role.OPERADOR);
        adapter.save(user);

        Page<User> page = adapter.findAll(PageRequest.ofSize(10));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().getRoles()).containsExactly(Role.OPERADOR);
    }

    private void saveUsers(int quantity) {
        for (int i = 1; i <= quantity; i++) {
            adapter.save(new User(null, "Usuario " + i, "usuario" + i + "@email.com", true));
        }
    }
}
