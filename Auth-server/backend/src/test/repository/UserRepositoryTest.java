package test.java.com.authentication.auth.repository; 

import com.authentication.auth.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findByEmail: 존재하는 이메일로 사용자 조회 성공")
    void findByEmail_existingEmail_returnsUser() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .userName("testuser")
                .build();
        entityManager.persistAndFlush(user);

        // when
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findByEmail: 존재하지 않는 이메일로 사용자 조회 시 빈 Optional 반환")
    void findByEmail_nonExistingEmail_returnsEmpty() {
        // given
        // No user with "nonexistent@example.com"

        // when
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // then
        assertThat(foundUser).isNotPresent();
    }

    @Test
    @DisplayName("findByUserName: 존재하는 userName으로 사용자 조회 성공 (기존 기능 확인)")
    void findByUserName_existingUserName_returnsUser() {
        // given
        User user = User.builder()
                .email("user@example.com")
                .password("password")
                .userName("existinguser")
                .build();
        entityManager.persistAndFlush(user);

        // when
        Optional<User> foundUser = userRepository.findByUserName("existinguser");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserName()).isEqualTo("existinguser");
    }
}
