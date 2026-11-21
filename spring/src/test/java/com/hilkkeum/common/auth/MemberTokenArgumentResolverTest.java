package com.hilkkeum.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.NativeWebRequest;

import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.room.domain.MemberRole;
import com.hilkkeum.room.domain.Room;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.repository.RoomMemberRepository;

class MemberTokenArgumentResolverTest {

	private RoomMemberRepository repository;
	private MemberTokenArgumentResolver resolver;
	private NativeWebRequest request;

	@BeforeEach
	void setUp() {
		repository = mock(RoomMemberRepository.class);
		resolver = new MemberTokenArgumentResolver(repository);
		request = mock(NativeWebRequest.class);
	}

	@Test
	void 유효한_토큰이면_멤버와_방_정보를_반환한다() {
		given(request.getHeader("memberToken")).willReturn("token-a");
		given(repository.findByMemberToken("token-a")).willReturn(Optional.of(member(10L, 1L, MemberRole.CREATOR)));

		AuthMember me = resolver.resolveArgument(null, null, request, null);

		assertThat(me.memberId()).isEqualTo(10L);
		assertThat(me.roomId()).isEqualTo(1L);
		assertThat(me.role()).isEqualTo(MemberRole.CREATOR);
		assertThat(me.belongsTo(1L)).isTrue();
		assertThat(me.belongsTo(2L)).isFalse();
	}

	@Test
	void 헤더가_없으면_401_MEMBER_TOKEN_MISSING() {
		given(request.getHeader("memberToken")).willReturn(null);

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, request, null))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.MEMBER_TOKEN_MISSING);
	}

	@Test
	void 없는_토큰이면_401_MEMBER_TOKEN_INVALID() {
		given(request.getHeader("memberToken")).willReturn("unknown");
		given(repository.findByMemberToken("unknown")).willReturn(Optional.empty());

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, request, null))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.MEMBER_TOKEN_INVALID);
	}

	private RoomMember member(Long memberId, Long roomId, MemberRole role) {
		Room room = instantiate(Room.class);
		ReflectionTestUtils.setField(room, "id", roomId);
		RoomMember member = instantiate(RoomMember.class);
		ReflectionTestUtils.setField(member, "id", memberId);
		ReflectionTestUtils.setField(member, "room", room);
		ReflectionTestUtils.setField(member, "role", role);
		return member;
	}

	private <T> T instantiate(Class<T> type) {
		try {
			var constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
