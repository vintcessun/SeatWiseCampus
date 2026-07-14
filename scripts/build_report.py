# -*- coding: utf-8 -*-
"""在《Java程序设计实践》项目开发报告（团队版）模板中，
填充「（二）、系统设计」与「（三）、系统测试」两部分。
输出两份：
  *_完整版.docx   —— 正文 + 图 + 表（完整）
  *_仅插图版.docx —— 仅小节标题 + 图 + 图题（供 Word with Claude 续写正文）
用法： python build_report.py
"""
import os
from PIL import Image
import docx
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.text.paragraph import Paragraph

REPO = r"D:\Scripts\SeatWiseCampus"
TEMPLATE = r"C:\Users\vintc\OneDrive - stu.xmu.edu.cn\学校\课程\大二小学期\java\3《Java程序设计实践》项目开发报告（团队版）.docx"
OUT_DIR = os.path.dirname(TEMPLATE)
OUT_FULL = os.path.join(OUT_DIR, "3《Java程序设计实践》项目开发报告（团队版）_完整版.docx")
OUT_IMG = os.path.join(OUT_DIR, "3《Java程序设计实践》项目开发报告（团队版）_仅插图版.docx")

FIG = os.path.join(REPO, "scripts", "figures")
BODY_FONT = "宋体"
HEAD_FONT = "黑体"
CONTENT_W_CM = 15.4          # 正文可用宽度
PORTRAIT_H_CM = 10.0         # 竖屏截图按高度约束


# ---------- 字体 / 段落工具 ----------
def _set_east_asia(run, font):
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.append(rFonts)
    rFonts.set(qn("w:eastAsia"), font)
    rFonts.set(qn("w:ascii"), font)
    rFonts.set(qn("w:hAnsi"), font)


def _fmt(run, font=BODY_FONT, size=12, bold=False, italic=False, color=None):
    run.font.name = font
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    if color:
        run.font.color.rgb = RGBColor.from_string(color)
    _set_east_asia(run, font)


class Cursor:
    """在锚点段落之后按顺序插入内容。"""
    def __init__(self, doc, anchor_p):
        self.doc = doc
        self.cur = anchor_p  # lxml <w:p>

    def _add_p_after(self):
        p = OxmlElement("w:p")
        self.cur.addnext(p)
        self.cur = p
        return Paragraph(p, self.doc._body)

    def heading(self, text, size=13):
        para = self._add_p_after()
        para.paragraph_format.space_before = Pt(10)
        para.paragraph_format.space_after = Pt(4)
        para.paragraph_format.line_spacing = 1.5
        _fmt(para.add_run(text), font=HEAD_FONT, size=size, bold=True, color="1F3864")
        return para

    def body(self, text):
        para = self._add_p_after()
        para.paragraph_format.line_spacing = 1.5
        para.paragraph_format.space_after = Pt(4)
        para.paragraph_format.first_line_indent = Pt(24)
        _fmt(para.add_run(text), font=BODY_FONT, size=12)
        return para

    def bullet(self, text):
        para = self._add_p_after()
        para.paragraph_format.line_spacing = 1.5
        para.paragraph_format.left_indent = Pt(24)
        r = para.add_run("· ")
        _fmt(r, font=BODY_FONT, size=12, bold=True, color="3B6CFF")
        _fmt(para.add_run(text), font=BODY_FONT, size=12)
        return para

    def image(self, filename):
        path = filename if os.path.isabs(filename) else os.path.join(FIG, filename)
        para = self._add_p_after()
        para.alignment = WD_ALIGN_PARAGRAPH.CENTER
        para.paragraph_format.space_before = Pt(6)
        para.paragraph_format.space_after = Pt(2)
        run = para.add_run()
        if not os.path.exists(path):
            _fmt(para.add_run(f"（图缺失：{os.path.basename(path)}）"), color="FF0000")
            return para
        w, h = Image.open(path).size
        if h > w * 1.25:  # 竖屏
            run.add_picture(path, height=Cm(PORTRAIT_H_CM))
        else:
            run.add_picture(path, width=Cm(CONTENT_W_CM))
        return para

    def caption(self, text):
        para = self._add_p_after()
        para.alignment = WD_ALIGN_PARAGRAPH.CENTER
        para.paragraph_format.space_after = Pt(10)
        _fmt(para.add_run(text), font=BODY_FONT, size=10.5, color="595959")
        return para

    def table(self, header, rows, caption=None):
        if caption:
            cap = self._add_p_after()
            cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            cap.paragraph_format.space_before = Pt(6)
            _fmt(cap.add_run(caption), font=BODY_FONT, size=10.5, bold=True, color="595959")
        tbl = self.doc.add_table(rows=1, cols=len(header))
        _set_table_borders(tbl)
        tbl.alignment = 1  # center
        for j, htext in enumerate(header):
            c = tbl.rows[0].cells[j]
            c.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
            _fmt(c.paragraphs[0].add_run(htext), font=HEAD_FONT, size=10.5, bold=True)
            _shade(c, "D9E2F3")
        for row in rows:
            cells = tbl.add_row().cells
            for j, val in enumerate(row):
                _fmt(cells[j].paragraphs[0].add_run(str(val)), font=BODY_FONT, size=10.5)
        # 移动到 cursor 之后
        self.cur.addnext(tbl._tbl)
        self.cur = tbl._tbl
        # 表后空段
        self._add_p_after()
        return tbl


def _set_table_borders(tbl):
    tblPr = tbl._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        e = OxmlElement("w:" + edge)
        e.set(qn("w:val"), "single")
        e.set(qn("w:sz"), "4")
        e.set(qn("w:space"), "0")
        e.set(qn("w:color"), "8A93A6")
        borders.append(e)
    tblPr.append(borders)


def _shade(cell, hexcolor):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:fill"), hexcolor)
    tcPr.append(shd)


# ---------- 定位与清理 ----------
def find_p(doc, text):
    for ch in doc.element.body.iterchildren():
        if ch.tag.endswith("}p"):
            par = Paragraph(ch, doc._body)
            if par.text.strip() == text:
                return ch
    return None


def delete_between(start_el, end_el):
    """删除 start_el 与 end_el 之间的所有兄弟节点（不含两端）。"""
    node = start_el.getnext()
    while node is not None and node is not end_el:
        nxt = node.getnext()
        node.getparent().remove(node)
        node = nxt


def delete_after_until_sectpr(start_el):
    node = start_el.getnext()
    while node is not None:
        nxt = node.getnext()
        if node.tag.endswith("}sectPr"):
            break
        node.getparent().remove(node)
        node = nxt


# ============================================================ 内容
def sp(p):  # 相对仓库路径 → 绝对
    return os.path.join(REPO, p)


DESIGN_FIGS = [
    ("2.1  系统总体架构",
     "系统采用经典的 C/S（浏览器/服务器）分层架构。前端 client 工程基于 Vue3 + Vite + Element Plus + Pinia + ECharts，"
     "分为页面层、组件层、状态管理层与 Axios REST / SSE 连接层，仅通过 REST 接口与 SSE 通道访问后端，不直接接触数据库。"
     "后端 server 工程基于 Spring Boot 3.5 + MyBatis-Plus + Sa-Token，由 Controller、Service、Mapper 三层构成，"
     "并配合 Redisson 分布式锁/延迟队列与定时任务。MySQL 8 作为唯一的正确性来源，Redis 7 仅承担座位状态缓存、"
     "计数与调度，二者职责严格分离。系统总体架构如图 2-1 所示。",
     "fig1_architecture.png", "图 2-1    系统总体架构图"),

    ("2.2  数据库设计",
     "数据库以「校区—楼栋—自习室—座位」的层级组织基础数据，以「预约主记录 reservation + 时间片占用 reservation_slot」"
     "承载预约业务，并辅以黑名单、积分流水、日聚合等表。核心表关系如图 2-2 所示。其中 reservation_slot 表将一次预约按"
     "30 分钟粒度拆分为若干时间片，其 (seat_id, date, slot_index) 唯一索引是防止重复预约的最终兜底，该表的字段设计见表 2-1。",
     "fig2_er.png", "图 2-2    数据库 E-R 关系图"),

    ("2.3  时间片并发模型",
     "为在高并发选座下保证「同一座位同一时段只能被一人预约」，系统不采用「读取区间再比较重叠」的方式（存在检查—写入竞态、"
     "难以施加数据库级唯一约束），而是将预约按 30 分钟粒度拆片存入 reservation_slot 表。如图 2-3 所示，每个 (座位, 时间片) "
     "至多被一条预约占用，插入即判重，冲突由数据库唯一索引原子拒绝，天然规避并发双占。",
     "fig3_slot_model.png", "图 2-3    时间片占用模型（30 分钟粒度拆片）"),

    ("2.4  核心预约与并发控制",
     "预约提交流程如图 2-4 所示：服务端先完成登录、角色、黑名单、开放时间、单日次数及自身时段冲突等校验，将起止时间换算为"
     "时间片序号列表；随后使用 Redisson 对目标座位加分布式锁以降低冲突概率，在同一事务内插入预约主记录与批量时间片占用记录。"
     "若命中 (seat_id, date, slot_index) 唯一索引冲突则回滚并返回 SEAT_ALREADY_RESERVED；成功则提交事务、写入 Redis 座位状态、"
     "注册超时释放延迟任务并推送 SSE 事件。Redisson 锁只是「减少冲突」，MySQL 唯一索引才是「最终兜底」——即使 Redis 不可用，"
     "并发预约仍不会双占。",
     "fig4_reservation_flow.png", "图 2-4    预约提交与并发控制流程"),

    ("2.5  预约状态机",
     "一条预约的完整生命周期由后端状态机驱动，如图 2-5 所示。预约成功后进入「待签到 PENDING_SIGN_IN」；在签到窗口内签到"
     "转为「使用中 IN_USE」，主动签退或到达结束时间自动完成转为「已完成 COMPLETED」；待签到期间用户可取消转为「已取消 "
     "CANCELLED」；若超时未签到则由延迟任务置为「爽约释放 EXPIRED_RELEASED」，累计爽约并可能进入黑名单。所有状态变更均在"
     "后端事务内完成并触发 SSE 推送。",
     "fig5_state_machine.png", "图 2-5    预约状态机"),

    ("2.6  实时看板（快照 + SSE 增量）",
     "实时热力看板采用「初始化快照 + SSE 增量推送」的方案，如图 2-6 所示。客户端打开看板时先拉取一次 board_snapshot（座位网格"
     "及每格状态），随后建立 SSE 订阅；当任一客户端完成预约/签到/释放等操作，服务端生成对应增量事件（如 seat_reserved）并由 SSE "
     "推送管理器广播给所有订阅者，各端仅对相关座位做局部更新，实现多客户端秒级一致，避免整屏轮询。",
     "fig6_sse.png", "图 2-6    实时看板 SSE 推送机制"),

    ("2.7  超时释放与黑名单",
     "为避免「占而不用」，系统在预约成功时向 Redisson DelayedQueue 投递一个到期时刻为「预约开始 + 15 分钟」的延迟任务。"
     "如图 2-7 所示，任务到期后校验预约状态：若已签到或已取消则忽略；若仍为待签到，则释放时间片、置座位为 FREE 并推送 SSE，"
     "同时爽约计数加一、扣除积分；当爽约次数达到阈值时写入 blacklist_record，使该用户在限制期（7 天）内预约被拒，但仍可登录与查看历史。",
     "fig7_timeout.png", "图 2-7    超时释放与黑名单机制"),

    ("2.8  部署架构",
     "系统通过 Docker Compose 一键部署，如图 2-8 所示，包含 frontend（Nginx 提供静态资源并反向代理 /api）、backend（Spring Boot）、"
     "mysql、redis 四个容器。浏览器/移动端仅访问前端 8888 端口，Nginx 在容器网络内将接口请求反代至后端；后端分别通过 JDBC 与 "
     "Lettuce/Redisson 连接 MySQL 与 Redis。该拓扑使前后端、数据库、缓存解耦，便于本地演示与迁移。",
     "fig8_deploy.png", "图 2-8    部署拓扑图"),
]

DB_TABLE = (
    "表 2-1    reservation_slot 时间片占用表结构",
    ["字段名称", "数据类型", "允许空", "键 / 约束", "说明"],
    [
        ["id", "BIGINT", "否", "PK 自增", "主键"],
        ["reservation_id", "BIGINT", "否", "IDX 外键", "关联 reservation 主记录"],
        ["seat_id", "BIGINT", "否", "UNIQUE(seat_id,date,slot_index)", "占用的座位；唯一键兜底防重"],
        ["date", "DATE", "否", "↑ 联合唯一", "预约日期"],
        ["slot_index", "INT", "否", "↑ 联合唯一", "30 分钟时间片序号"],
        ["created_time", "DATETIME", "是", "", "创建时间"],
    ],
)

# 系统测试：小节 -> [(prose, 图列表[(img_abs, caption)])]
TEST_SECTIONS = [
    ("3.1  测试环境与方法",
     "测试在 Docker Compose 环境下进行（前端 Nginx:8888、后端 Spring Boot:18080、MySQL 8、Redis 7），"
     "采用「自动化冒烟/并发脚本 + 真实 Chrome 多端页面走查」相结合的方式。自动化脚本 scripts/smoke-test.mjs 覆盖登录、"
     "看板、预约、重复预约拒绝、8 并发仅 1 成功、签到签退、座位释放、单日限次、报表、权限等 13 项核心用例并全部通过；"
     "页面走查覆盖学生端 9 页、管理端 10 页在桌面/iOS/Android、中英双语、明暗主题下的表现，均无控制台错误。主要功能测试用例与结果见表 3-1。",
     []),
    ("3.2  登录与权限",
     "系统提供学生/管理员两类角色登录及注册、找回密码、图形验证码等能力，登录后由 Sa-Token 维护登录态与角色鉴权，"
     "并按角色路由到对应工作台。登录页如图 3-1 所示。",
     [(sp("scripts/report/desktop/00-login.png"), "图 3-1    登录页")]),
    ("3.3  选座预约主流程",
     "学生按「校区→楼栋→楼层」筛选自习室（图 3-2），进入后在座位网格上选择日期、时间片与具体座位提交预约（图 3-3）；"
     "座位状态（空闲/已约/使用中/禁用）以颜色区分，最终预约结果以后端返回为准。",
     [(sp("scripts/report/desktop/student-rooms.png"), "图 3-2    自习室筛选与列表"),
      (sp("scripts/report/shots/03-student-seats.png"), "图 3-3    座位网格选座")]),
    ("3.4  实时热力看板",
     "管理端与学生端共享「快照 + SSE」实时看板：两个客户端打开同一自习室，一端预约/释放，另一端座位秒级变色（图 3-4）；"
     "时空占用图进一步以时间轴维度展示各时段占用情况（图 3-5），验证了 SSE 增量推送与多端一致性。",
     [(sp("scripts/report/shots/09-admin-board.png"), "图 3-4    管理端实时看板"),
      (sp("scripts/report/desktop/admin-spacetime.png"), "图 3-5    时空占用图")]),
    ("3.5  管理与运营",
     "管理端提供概览驾驶舱（图 3-6）、座位排布可视化编辑器（图 3-7）与黑名单管理（图 3-8）等能力，"
     "支持自习室/座位维护、爽约用户查看与解禁等运营操作。",
     [(sp("scripts/report/desktop/admin-dashboard.png"), "图 3-6    管理台概览"),
      (sp("scripts/report/shots/35-layout-edit.png"), "图 3-7    座位排布编辑器"),
      (sp("scripts/report/shots/11-admin-blacklist.png"), "图 3-8    黑名单管理")]),
    ("3.6  数据报表与积分排行",
     "系统基于日聚合表生成使用率、取消率、爽约率、热门时段与利用率排行等报表（图 3-9），并按守约/爽约规则结算积分、"
     "生成积分排行榜（图 3-10）。",
     [(sp("scripts/report/desktop/admin-reports.png"), "图 3-9    数据报表"),
      (sp("scripts/report/desktop/student-ranking.png"), "图 3-10    积分排行榜")]),
    ("3.7  智能与工程化",
     "学生端集成 AI 智能选座助手，可用自然语言表达需求并给出可解释的座位推荐（图 3-11，离线内置规则引擎，亦可接入大模型）；"
     "后端接口经 Knife4j 生成可交互文档（图 3-12），便于联调与验收。",
     [(sp("scripts/report/shots/13-ai-assistant.png"), "图 3-11    AI 选座助手"),
      (sp("scripts/report/shots/12-knife4j.png"), "图 3-12    Knife4j 接口文档")]),
    ("3.8  多端适配与国际化",
     "前端实现了明暗主题（图 3-13）、移动端响应式（iOS 见图 3-14、Android 见图 3-15）与中英双语国际化（图 3-16）。"
     "在 iPhone 13 与 Pixel 5 模拟设备上，侧边栏收起为抽屉式导航、栅格纵向堆叠，无横向溢出；语言切换即时联动界面与组件。",
     [(sp("scripts/report/shots/27-dark-student.png"), "图 3-13    深色模式"),
      (sp("scripts/report/ios/student-home.png"), "图 3-14    iOS 移动端"),
      (sp("scripts/report/android/admin-reports.png"), "图 3-15    Android 移动端"),
      (sp("scripts/report/desktop-en/student-home.png"), "图 3-16    英文界面")]),
]

TEST_TABLE = (
    "表 3-1    主要功能测试用例与结果",
    ["编号", "测试项", "操作 / 输入", "预期结果", "结果"],
    [
        ["T01", "登录鉴权", "演示账号登录", "返回 token 与角色，路由正确", "通过"],
        ["T02", "自习室查询", "按校区/楼栋/楼层筛选", "返回匹配自习室与开放状态", "通过"],
        ["T03", "看板快照", "打开自习室看板", "返回座位网格与每格状态", "通过"],
        ["T04", "正常预约", "选座+时间片提交", "预约成功并占用时间片", "通过"],
        ["T05", "重复预约拒绝", "同座同片再次预约", "返回 SEAT_ALREADY_RESERVED", "通过"],
        ["T06", "并发抢座", "8 人并发抢同一座位", "仅 1 人成功，其余被拒", "通过"],
        ["T07", "签到 / 签退", "预约后签到再签退", "状态流转正确并释放座位", "通过"],
        ["T08", "超时释放", "超签到窗口未签到", "自动释放并计爽约", "通过"],
        ["T09", "单日限次", "超过单日预约次数", "返回 DAILY_LIMIT_EXCEEDED", "通过"],
        ["T10", "黑名单", "爽约达阈值后预约", "返回 USER_IN_BLACKLIST", "通过"],
        ["T11", "实时推送", "一端预约另一端观察", "另一端座位秒级变色", "通过"],
        ["T12", "数据报表", "查看报表页", "口径与聚合表一致", "通过"],
        ["T13", "权限校验", "学生访问管理接口", "被拒绝（无权限）", "通过"],
    ],
)


# ============================================================ 组装
def build(images_only):
    doc = docx.Document(TEMPLATE)
    h_design = find_p(doc, "（二）、系统设计")
    h_test = find_p(doc, "（三）、系统测试")
    assert h_design is not None and h_test is not None, "未找到章节标题"

    # 清理模板占位内容
    delete_between(h_design, h_test)
    delete_after_until_sectpr(h_test)

    # ---- （二）系统设计 ----
    c = Cursor(doc, h_design)
    if not images_only:
        c.body("本部分从总体架构、数据库、并发模型、核心流程、状态机、实时看板、超时释放与部署八个方面阐述系统设计。"
               "系统的两大技术核心是「时间片并发选座的正确性」与「多客户端实时看板的一致性」，其余设计均围绕二者展开。")
    for title, prose, img, cap in DESIGN_FIGS:
        c.heading(title)
        if not images_only:
            c.body(prose)
        c.image(img)
        c.caption(cap)
        if title.startswith("2.2"):
            if images_only:
                c.table(DB_TABLE[1], DB_TABLE[2], caption=DB_TABLE[0])
            else:
                c.table(DB_TABLE[1], DB_TABLE[2], caption=DB_TABLE[0])

    # ---- （三）系统测试 ----
    c2 = Cursor(doc, h_test)
    if not images_only:
        c2.body("本部分给出系统的测试环境、方法与主要功能的测试结果。测试以自动化脚本验证核心正确性，"
                "以真实浏览器多端走查验证交互与展示效果。")
    for title, prose, imgs in TEST_SECTIONS:
        c2.heading(title)
        if not images_only:
            c2.body(prose)
        for img_abs, cap in imgs:
            c2.image(img_abs)
            c2.caption(cap)
        if title.startswith("3.1"):
            c2.table(TEST_TABLE[1], TEST_TABLE[2], caption=TEST_TABLE[0])

    out = OUT_IMG if images_only else OUT_FULL
    doc.save(out)
    print("已生成:", out)


if __name__ == "__main__":
    build(images_only=False)
    build(images_only=True)
    print("完成。")
