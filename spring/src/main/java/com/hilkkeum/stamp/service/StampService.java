package com.hilkkeum.stamp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hilkkeum.common.exception.ApiException;
import com.hilkkeum.common.exception.ErrorCode;
import com.hilkkeum.coupon.domain.WishCoupon;
import com.hilkkeum.coupon.service.CouponService;
import com.hilkkeum.game.domain.Game;
import com.hilkkeum.game.repository.GameRepository;
import com.hilkkeum.room.domain.RoomMember;
import com.hilkkeum.room.repository.RoomMemberRepository;
import com.hilkkeum.stamp.dto.AwardStampResponse;
import com.hilkkeum.stamp.dto.StampsResponse;

import lombok.RequiredArgsConstructor;

/**
 * 스탬프 규칙
 * - 결산 승자에게 1개 적립, 최대 3개 (무승부 = 맞힌 개수 동일이면 승부 없이 두 사람 모두 1개, 벌칙 없음)
 * - 3번째 스탬프를 얻었고 소원권이 없으면 소원권 자동 발급 (스탬프는 3개로 유지)
 * - 소원권 보유 중 승리하면 스탬프는 적립되지 않고 소멸
 * - 소원권을 사용하거나 만료되면 스탬프 0 (CouponService)
 */
@Service
@RequiredArgsConstructor
public class StampService {

	/** 스탬프 최대 보유량 = 소원권 1장에 필요한 수 */
	public static final int MAX_STAMPS = 3;

	private final GameRepository gameRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final CouponService couponService;

	/**
	 * 결산 결과에 따라 스탬프 적립 (Node → Spring 내부 호출).
	 * - 승리(winnerMemberId 있음): DB에 확정된 승자와 같아야 하고, 승자 1명에게 1개
	 * - 무승부(winnerMemberId 없음, 맞힌 개수 동일): DB에 무승부로 확정돼 있어야 하고, 승부 없이 두 사람 모두 1개
	 * - 게임당 1회만 처리(멱등): 같은 gameId 재요청은 모두 awarded=false
	 * - 처리 기록 · 적립 · 자동 발급은 한 트랜잭션, 멤버 행을 잠가 동시 요청에도 소원권이 중복 발급되지 않음
	 */
	@Transactional
	public AwardStampResponse award(Long gameId, Long winnerMemberId) {
		Game game = gameRepository.findById(gameId)
			.orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
		boolean draw = winnerMemberId == null;
		List<Long> memberIds = draw ? confirmDraw(game) : List.of(confirmWinner(game, winnerMemberId));
		AwardStampResponse.Result result = draw ? AwardStampResponse.Result.DRAW : AwardStampResponse.Result.WIN;

		LocalDateTime now = LocalDateTime.now();
		if (gameRepository.markStampAwarded(gameId, now) != 1) {
			List<AwardStampResponse.MemberAward> already = memberIds.stream()
				.map(id -> AwardStampResponse.MemberAward.notAwarded(id, currentStamps(id)))
				.toList();
			return new AwardStampResponse(gameId, result, already);
		}
		couponService.expireOverdue(game.getRoomId(), now);

		// 잠금 순서를 멤버 ID 오름차순으로 고정 → 무승부 동시 처리에도 교착 없음
		List<AwardStampResponse.MemberAward> awards = memberIds.stream().sorted()
			.map(id -> awardOne(id, now))
			.toList();
		return new AwardStampResponse(gameId, result, awards);
	}

	/** 한 사람에게 1개 적립: 소원권 보유 중이면 소멸, 3개가 되면 소원권 자동 발급 */
	private AwardStampResponse.MemberAward awardOne(Long memberId, LocalDateTime now) {
		RoomMember member = roomMemberRepository.findByIdForUpdate(memberId)
			.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (couponService.hasActiveCouponForUpdate(member.getId())) {
			return AwardStampResponse.MemberAward.notAwarded(memberId, member.getStampCount());
		}
		boolean awarded = member.addStamp(MAX_STAMPS);
		WishCoupon issued = member.hasFullStamps(MAX_STAMPS) ? couponService.issue(member, now) : null;
		return AwardStampResponse.MemberAward.of(memberId, awarded, member.getStampCount(), issued);
	}

	/** 승리: 결산 확정 + 요청한 승자가 DB에 확정된 승자와 같은지 */
	private Long confirmWinner(Game game, Long winnerMemberId) {
		RoomMember winner = roomMemberRepository.findById(winnerMemberId)
			.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (!game.isSettled()) {
			throw new ApiException(ErrorCode.GAME_NOT_SETTLED);
		}
		if (!game.isWinnerConfirmed()) {
			throw new ApiException(ErrorCode.WINNER_MISMATCH);
		}
		Long confirmedWinnerMemberId = gameRepository.findMemberIdOfPlayer(game.getId(), game.getWinnerPlayerId())
			.orElseThrow(() -> new ApiException(ErrorCode.WINNER_MISMATCH));
		if (!confirmedWinnerMemberId.equals(winner.getId()) || !winner.getRoom().getId().equals(game.getRoomId())) {
			throw new ApiException(ErrorCode.WINNER_MISMATCH);
		}
		return winner.getId();
	}

	/** 무승부: 결산 확정 + 승자 없음 + decided_by = DRAW 인지, 두 참가자 반환 */
	private List<Long> confirmDraw(Game game) {
		if (!game.isSettled()) {
			throw new ApiException(ErrorCode.GAME_NOT_SETTLED);
		}
		if (!game.isDrawConfirmed()) {
			throw new ApiException(ErrorCode.GAME_NOT_DRAW);
		}
		List<Long> memberIds = gameRepository.findMemberIdsOfGame(game.getId());
		if (memberIds.size() != 2) {
			throw new ApiException(ErrorCode.MEMBER_NOT_FOUND);
		}
		return memberIds;
	}

	/** 보유 스탬프 및 소원권 보유 여부 */
	@Transactional
	public StampsResponse getStamps(Long memberId, Long roomId) {
		couponService.expireOverdue(roomId, LocalDateTime.now());
		return new StampsResponse(currentStamps(memberId), MAX_STAMPS, couponService.hasActiveCoupon(memberId));
	}

	private int currentStamps(Long memberId) {
		return roomMemberRepository.findById(memberId).map(RoomMember::getStampCount).orElse(0);
	}
}
