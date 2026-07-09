package com.seatwise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.entity.Announcement;
import com.seatwise.entity.User;
import com.seatwise.mapper.AnnouncementMapper;
import com.seatwise.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告中心：管理员发布系统公告；学生端首页横幅 + 列表展示；发布时可选一并推送站内通知（复用通知中心）。
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper mapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /** 学生/管理员：生效中的公告，按时间倒序 */
    public List<Announcement> listActive() {
        return mapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getActive, 1)
                .orderByDesc(Announcement::getCreatedTime));
    }

    /** 管理员：全部公告（含已下线） */
    public List<Announcement> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreatedTime));
    }

    public Announcement create(Long publisherId, String title, String content, String level, boolean notifyAll) {
        if (title == null || title.isBlank() || content == null || content.isBlank())
            throw new BizException(BizError.BAD_REQUEST, "标题和内容不能为空");
        Announcement a = new Announcement();
        a.setTitle(title.trim());
        a.setContent(content.trim());
        a.setLevel("WARN".equalsIgnoreCase(level) ? "WARN" : "INFO");
        a.setActive(1);
        a.setPublisherId(publisherId);
        mapper.insert(a);
        if (notifyAll) pushToStudents(a);
        return a;
    }

    public Announcement update(Long id, String title, String content, String level, Integer active) {
        Announcement a = mapper.selectById(id);
        if (a == null) throw new BizException(BizError.BAD_REQUEST, "公告不存在");
        if (title != null && !title.isBlank()) a.setTitle(title.trim());
        if (content != null && !content.isBlank()) a.setContent(content.trim());
        if (level != null) a.setLevel("WARN".equalsIgnoreCase(level) ? "WARN" : "INFO");
        if (active != null) a.setActive(active);
        mapper.updateById(a);
        return a;
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void pushToStudents(Announcement a) {
        List<User> students = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT"));
        for (User u : students) {
            notificationService.notify(u.getId(), "ANNOUNCEMENT",
                    "📢 " + a.getTitle(), a.getContent());
        }
    }
}
