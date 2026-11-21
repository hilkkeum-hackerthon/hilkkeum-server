package com.hilkkeum.common.auth;

import com.hilkkeum.room.domain.MemberRole;

/**
 * memberToken으로 인증된 요청자 정보.
 */
public record AuthMember(Long memberId, Long roomId, MemberRole role) {

	/** 요청자가 해당 방의 멤버인지 */
	public boolean belongsTo(Long roomId) {
		return this.roomId.equals(roomId);
	}
}
