package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.entity.ScoreRecord;
import com.seatwise.entity.User;
import com.seatwise.mapper.ScoreMapper;
import com.seatwise.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreMapper scoreMapper;
    private final UserMapper userMapper;

    /** 记录积分变更并更新用户累计积分（幂等：同一预约同一 reason 只记一次） */
    @Transactional
    public void addScore(Long userId, int change, String reason, Long refReservationId) {
        if (refReservationId != null) {
            Long exist = scoreMapper.selectCount(new LambdaQueryWrapper<ScoreRecord>()
                    .eq(ScoreRecord::getUserId, userId)
                    .eq(ScoreRecord::getReason, reason)
                    .eq(ScoreRecord::getRefReservationId, refReservationId));
            if (exist != null && exist > 0) {
                return;
            }
        }
        ScoreRecord rec = new ScoreRecord();
        rec.setUserId(userId);
        rec.setScoreChange(change);
        rec.setReason(reason);
        rec.setRefReservationId(refReservationId);
        scoreMapper.insert(rec);

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setCreditScore((user.getCreditScore() == null ? 0 : user.getCreditScore()) + change);
            userMapper.updateById(user);
        }
    }

    public Map<String, Object> me(Long userId) {
        User user = userMapper.selectById(userId);
        List<ScoreRecord> records = scoreMapper.selectList(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getUserId, userId)
                .orderByDesc(ScoreRecord::getCreatedTime).last("limit 50"));
        Map<String, Object> map = new HashMap<>();
        map.put("creditScore", user != null ? user.getCreditScore() : 0);
        map.put("records", records);
        return map;
    }

    public List<Map<String, Object>> ranking() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "STUDENT")
                .orderByDesc(User::getCreditScore).last("limit 20"));
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("rank", rank++);
            m.put("userId", u.getId());
            m.put("realName", u.getRealName());
            m.put("creditScore", u.getCreditScore());
            m.put("noShowCount", u.getNoShowCount());
            list.add(m);
        }
        return list;
    }
}
