package com.hilkkeum.stamp.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.coupon.service.CouponService;
import com.hilkkeum.game.domain.Game;
import com.hilkkeum.game.repository.GameRepository;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.repository.RoomMemberRepository;
import com.hilkkeum.stamp.dto.AwardStampResponse;
import com.hilkkeum.stamp.dto.StampsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StampService {

	private final GameRepository gameRepository;
	private final RoomMemberRepository roomMemberRepository;

	/**
	 * 결산 승자에게 스탬프 1개 적립. 게임당 1회만 적립되며(멱등), 같은 게임으로 다시 호출하면 awarded=false.
	 */
	@Transactional
	public AwardStampResponse award(Long gameId, Long winnerMemberId) {
		Game game = gameRepository.findById(gameId)
			.orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
		RoomMember winner = roomMemberRepository.findById(winnerMemberId)
			.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (!winner.getRoom().getId().equals(game.getRoomId())) {
			throw new ApiException(ErrorCode.WINNER_NOT_IN_GAME_ROOM);
		}

		boolean awarded = gameRepository.markStampAwarded(gameId, LocalDateTime.now()) == 1;
		if (awarded) {
			roomMemberRepository.addStamp(winnerMemberId);
		}

		int stampCount = roomMemberRepository.findById(winnerMemberId)
			.map(RoomMember::getStampCount)
			.orElse(0);
		return new AwardStampResponse(gameId, winnerMemberId, awarded, stampCount);
	}

	/** 보유 스탬프 및 교환 가능 여부 */
	@Transactional(readOnly = true)
	public StampsResponse getStamps(Long memberId) {
		int stampCount = roomMemberRepository.findById(memberId)
			.map(RoomMember::getStampCount)
			.orElse(0);
		int perCoupon = CouponService.STAMPS_PER_COUPON;
		return new StampsResponse(stampCount, perCoupon, stampCount >= perCoupon);
	}
}
