package com.example.demo.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import com.example.demo.exception.CertificationCodeNotMatchedException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.UserStatus;
import com.example.demo.model.dto.UserCreateDto;
import com.example.demo.model.dto.UserUpdateDto;
import com.example.demo.repository.UserEntity;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlGroup;

@SpringBootTest
@TestPropertySource("classpath:test-application.properties")
@SqlGroup({
	@Sql(value = "/sql/user-service-test-data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD),
	@Sql(value = "/sql/delete-all-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
})
class UserServiceTest {

	@Autowired
	private UserService userService;

	@MockBean
	private JavaMailSender mailSender;

	@Test
	void getByEmail은_ACTIVE_상태인_유저를_찾아올_수_있다() {
		// given
		String email = "ljs0429777@gmail.com";

		// when
		UserEntity result = userService.getByEmail(email);

		// then
		assertThat(result.getNickname()).isEqualTo("JeongSeok");
	}

	@Test
	void getByEmail은_PENDING_상태인_유저를_찾아올_수_없다() {
		// given
		String email = "ljs0429778@gmail.com";

		// when
		// then
		assertThatThrownBy(() -> {
			UserEntity result = userService.getByEmail(email);
		}).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void getById은_ACTIVE_상태인_유저를_찾아올_수_있다() {
		// given
		// when
		UserEntity result = userService.getById(1);

		// then
		assertThat(result.getNickname()).isEqualTo("JeongSeok");
	}

	@Test
	void getById은_PENDING_상태인_유저를_찾아올_수_없다() {
		// given
		// when
		// then
		assertThatThrownBy(() -> {
			UserEntity result = userService.getById(2);
		}).isInstanceOf(ResourceNotFoundException.class);
	}


	/**
	 * DataIntegrityViolationException 문제가 발생했는데, 이는 DB에 값이 있다는 오류같음
	 * 해결방법으로는 test-application.properties 파일에 테스트용 DB 설정을 완료
	 * 이쯤되면 궁금한게 그럼 properties 파일, yml파일은 프로젝트에 어떤 역할을 수행하게 되는건가?
	 */
	@Test
	void userCreateDto_를_이용하여_유저를_생성할_수_있다() {
		// given
		UserCreateDto userCreateDto = UserCreateDto.builder()
			.email("ljs0429777@kakao.com")
			.address("Incheon")
			.nickname("jeongseok")
			.build();

		BDDMockito.doNothing().when(mailSender).send(any(SimpleMailMessage.class));

		// when
		UserEntity result = userService.create(userCreateDto);

		// then
		assertThat(result.getId()).isNotNull();
		assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
//		assertThat(result.getCertificationCode()).isEqualTo(T.T);

	}

	@Test
	void userUpdateDto_를_이용하여_유저를_수정할_수_있다() {
		// given
		UserUpdateDto userUpdateDto = UserUpdateDto.builder()
			.address("Busan")
			.nickname("jeongseok2")
			.build();

		BDDMockito.doNothing().when(mailSender).send(any(SimpleMailMessage.class));

		// when
		UserEntity result = userService.update(1, userUpdateDto);

		// then
		UserEntity userEntity = userService.getById(1);
		assertThat(userEntity.getId()).isNotNull();
		assertThat(userEntity.getAddress()).isEqualTo("Busan");
		assertThat(userEntity.getNickname()).isEqualTo("jeongseok2");
//		assertThat(result.getCertificationCode()).isEqualTo(T.T);

	}

	@Test
	void user를_로그인_시키면_마지막_로그인_시간이_변경된다() {
		// given
		// when
		userService.login(1);

		// then
		UserEntity userEntity = userService.getById(1);
		assertThat(userEntity.getLastLoginAt()).isGreaterThan(0L);
	}

	@Test
	void PENDING_상태의_사용자는_인증_코드로_ACTIVE_시킬_수_있다() {
		// given
		// when
		userService.verifyEmail(2, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab");

		// then
		UserEntity userEntity = userService.getById(2);
		assertThat(userEntity.getStatus()).isEqualTo(UserStatus.ACTIVE);

	}

	@Test
	void PENDING_상태의_사용자는_잘못된_인증_코드를_받으면_에러를_던진다() {
		// given
		// when
		// then
		assertThatThrownBy(() -> {
			userService.verifyEmail(2, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaac");
		}).isInstanceOf(CertificationCodeNotMatchedException.class);

	}

}