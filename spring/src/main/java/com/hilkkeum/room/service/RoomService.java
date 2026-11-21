package com.hilkkeum.room.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hilkkeum.common.auth.AuthMember;
import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.room.domain.MemberRole;
import com.hilkkeum.room.domain.Room;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.dto.CreateRoomResponse;
import com.hilkkeum.room.dto.JoinRoomResponse;
import com.hilkkeum.room.dto.RoomStatusResponse;
import com.hilkkeum.room.repository.RoomMemberRepository;
import com.hilkkeum.room.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {

	private static final int MAX_CODE_ATTEMPTS = 10;
	private static final int ROOM_CAPACITY = 2;

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final CodeGenerator codeGenerator;

	/** 방 생성: 6자리 초대 코드 발급 + 생성자(방장) 등록 + memberToken 발급 */
	@Transactional
	public CreateRoomResponse create() {
		Room room = roomRepository.save(Room.create(newUniqueInviteCode()));
		RoomMember creator = roomMemberRepository.save(
			RoomMember.create(room, MemberRole.CREATOR, codeGenerator.memberToken()));
		return new CreateRoomResponse(room.getId(), room.getInviteCode(), creator.getId(), creator.getMemberToken());
	}

	/**
	 * 초대 코드 입장.
	 * - 없는 코드 → INVALID_INVITE_CODE (재입력 안내)
	 * - 이미 방이 성립된 코드 → INVITE_CODE_ALREADY_USED (정원 초과)
	 */
	@Transactional
	public JoinRoomResponse join(String rawInviteCode) {
		String inviteCode = normalize(rawInviteCode);
		Room room = roomRepository.findByInviteCode(inviteCode)
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_INVITE_CODE));

		// 조건부 UPDATE로 정원(2명) 보장: 동시에 입장해도 한 명만 성공
		if (!room.isPending() || roomRepository.activateIfPending(room.getId()) != 1) {
			throw new ApiException(ErrorCode.INVITE_CODE_ALREADY_USED);
		}

		Room activated = roomRepository.findById(room.getId())
			.orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
		RoomMember joiner = roomMemberRepository.save(
			RoomMember.create(activated, MemberRole.JOINER, codeGenerator.memberToken()));
		return new JoinRoomResponse(activated.getId(), joiner.getId(), joiner.getMemberToken(), joiner.getRole());
	}

	/**
	 * 방 정보 조회 (대기 화면 폴링 · 재접속 복구).
	 * 방이 없으면 404, 해당 방 참가자가 아니면 403. 상대 토큰은 응답하지 않는다.
	 */
	@Transactional(readOnly = true)
	public RoomStatusResponse getStatus(Long roomId, AuthMember me) {
		Room room = roomRepository.findById(roomId)
			.orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
		if (!me.belongsTo(roomId)) {
			throw new ApiException(ErrorCode.NOT_ROOM_MEMBER);
		}
		List<RoomStatusResponse.Member> members = roomMemberRepository.findByRoomIdOrderByIdAsc(roomId).stream()
			.map(m -> new RoomStatusResponse.Member(m.getId(), m.getRole()))
			.toList();
		return new RoomStatusResponse(room.getId(), room.getStatus(), members.size(), ROOM_CAPACITY, members);
	}

	private String newUniqueInviteCode() {
		for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
			String code = codeGenerator.inviteCode();
			if (!roomRepository.existsByInviteCode(code)) {
				return code;
			}
		}
		throw new ApiException(ErrorCode.INTERNAL_ERROR);
	}

	/** 공백 제거 + 대문자 (사용자가 소문자로 입력해도 허용) */
	private String normalize(String inviteCode) {
		return inviteCode.trim().toUpperCase(Locale.ROOT);
	}
}
