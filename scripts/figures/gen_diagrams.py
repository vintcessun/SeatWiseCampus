# -*- coding: utf-8 -*-
"""生成《项目开发报告》系统设计部分所需的全部图（Graphviz + matplotlib）。
输出到本目录 *.png。中文字体使用 微软雅黑。
"""
import os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
FONT = "Microsoft YaHei"
DPI = "170"

# 统一配色
BLUE, BLUE_L = "#3b6cff", "#eef4ff"
PURP, PURP_L = "#8f5bff", "#f3edff"
GREEN, GREEN_L = "#1f9d55", "#e3f6e9"
ORANGE, ORANGE_L = "#d98a00", "#fff2dd"
RED, RED_L = "#d64545", "#ffe1e1"
GRAY, GRAY_L = "#5a6172", "#eef0f5"


def render(name, dot):
    dot_path = os.path.join(HERE, name + ".dot")
    png_path = os.path.join(HERE, name + ".png")
    with open(dot_path, "w", encoding="utf-8") as f:
        f.write(dot)
    r = subprocess.run(["dot", "-Tpng", "-Gdpi=" + DPI, dot_path, "-o", png_path],
                       capture_output=True, text=True)
    if r.returncode != 0:
        print("  !! dot failed for", name, r.stderr)
    else:
        print("  ok", name + ".png")
    os.remove(dot_path)


COMMON = f'''
  graph [fontname="{FONT}", fontsize=12, bgcolor="white"];
  node  [fontname="{FONT}", fontsize=11];
  edge  [fontname="{FONT}", fontsize=10, color="#8a93a6"];
'''

# ---------------------------------------------------------------- 1 架构
def fig_architecture():
    dot = f'''digraph arch {{
  rankdir=TB; {COMMON}
  node [shape=box, style="rounded,filled", color="#c9d4ea"];

  subgraph cluster_client {{
    label="client 前端工程 · Vue3 + Vite + Element Plus + Pinia + ECharts";
    style="rounded,filled"; color="#c9d4ea"; fillcolor="#f7f9ff"; fontcolor="{BLUE}";
    Pages [label="页面层 Views", fillcolor="{BLUE_L}"];
    Comps [label="组件层 Components", fillcolor="{BLUE_L}"];
    Store [label="Pinia 状态管理", fillcolor="{BLUE_L}"];
    Api   [label="Axios REST 层", fillcolor="{PURP_L}"];
    Sse   [label="SSE 连接层", fillcolor="{PURP_L}"];
    Pages -> Comps -> Store; Store -> Api; Store -> Sse;
  }}

  subgraph cluster_server {{
    label="server 后端工程 · Spring Boot 3.5 + MyBatis-Plus + Sa-Token";
    style="rounded,filled"; color="#c9d4ea"; fillcolor="#f6fbf7"; fontcolor="{GREEN}";
    Ctl [label="Controller\\nREST + SSE 端点", fillcolor="{GREEN_L}"];
    Sec [label="Sa-Token 鉴权\\n(角色/权限)", fillcolor="{GREEN_L}"];
    Svc [label="Service 业务层\\n(预约/签到/报表)", fillcolor="{GREEN_L}"];
    Lock[label="Redisson\\n分布式锁 / 延迟队列", fillcolor="{ORANGE_L}"];
    Mp  [label="MyBatis-Plus Mapper", fillcolor="{GREEN_L}"];
    Job [label="定时 / 延迟任务\\n(超时释放·统计聚合)", fillcolor="{ORANGE_L}"];
    SsePush [label="SSE 推送管理器", fillcolor="{GREEN_L}"];
    Ctl -> Sec; Ctl -> Svc; Svc -> Lock; Svc -> Mp; Job -> Svc; SsePush -> Ctl [style=dashed];
  }}

  MySQL [label="MySQL 8\\n主存储 · 唯一索引兜底", shape=cylinder, style=filled, fillcolor="#dfe8ff", color="{BLUE}"];
  Redis [label="Redis 7\\n状态缓存 · 计数 · 调度", shape=cylinder, style=filled, fillcolor="{ORANGE_L}", color="{ORANGE}"];

  Api -> Ctl [label="HTTPS  /api/**", color="{BLUE}", fontcolor="{BLUE}", penwidth=1.6];
  Sse -> Ctl [label="text/event-stream", color="{PURP}", fontcolor="{PURP}", penwidth=1.6, style=dashed];
  Mp  -> MySQL [color="{BLUE}", penwidth=1.6];
  Svc -> Redis [label="缓存/计数", color="{ORANGE}", fontcolor="{ORANGE}"];
  Lock -> Redis [label="锁/延迟队列", color="{ORANGE}", fontcolor="{ORANGE}", style=dashed];
}}'''
    render("fig1_architecture", dot)


# ---------------------------------------------------------------- 2 E-R
def fig_er():
    def tbl(title, fields, fill, color, pk=None, uk=None):
        rows = f'<tr><td bgcolor="{color}" align="center"><font color="white"><b>{title}</b></font></td></tr>'
        for f in fields:
            rows += f'<tr><td align="left" bgcolor="{fill}">{f}</td></tr>'
        return f'<<table border="0" cellborder="1" cellspacing="0" cellpadding="4">{rows}</table>>'
    dot = f'''digraph er {{
  rankdir=LR; {COMMON}
  node [shape=plaintext];
  splines=ortho; nodesep=0.5; ranksep=0.8;

  campus   [label={tbl("campus 校区", ["id (PK)", "name", "经纬度/平面坐标(预留)"], "#f7f9ff", GRAY)}];
  building [label={tbl("building 楼栋", ["id (PK)", "campus_id (FK)", "name"], "#f7f9ff", GRAY)}];
  room     [label={tbl("study_room 自习室", ["id (PK)", "building_id (FK)", "floor_no", "open_start/open_end", "status"], "#eef4ff", BLUE)}];
  seat     [label={tbl("seat 座位", ["id (PK)", "room_id (FK)", "row_index/col_index", "cell_type", "seat_no", "enabled"], "#eef4ff", BLUE)}];
  usr      [label={tbl("sys_user 用户", ["id (PK)", "username (UQ)", "role", "credit_score", "no_show_count"], "#f3edff", PURP)}];
  resv     [label={tbl("reservation 预约主记录", ["id (PK)", "user_id (FK)", "seat_id (FK)", "room_id (FK)", "date", "start_slot/end_slot", "status"], "#e3f6e9", GREEN)}];
  slot     [label={tbl("reservation_slot 时间片占用", ["id (PK)", "reservation_id (FK)", "★ UNIQUE(seat_id,date,slot_index)"], "#ffe1e1", RED)}];
  black    [label={tbl("blacklist_record 黑名单", ["id (PK)", "user_id (FK)", "start/end_time", "active"], "#fff2dd", ORANGE)}];
  score    [label={tbl("score_record 积分流水", ["id (PK)", "user_id (FK)", "change", "reason"], "#fff2dd", ORANGE)}];

  campus -> building [label="1..N", arrowhead=crow];
  building -> room   [label="1..N", arrowhead=crow];
  room -> seat       [label="1..N", arrowhead=crow];
  usr -> resv        [label="1..N", arrowhead=crow];
  seat -> resv       [label="1..N", arrowhead=crow];
  resv -> slot       [label="1..N 拆片", arrowhead=crow, color="{RED}", fontcolor="{RED}", penwidth=1.6];
  usr -> black       [label="1..N", arrowhead=crow, style=dashed];
  usr -> score       [label="1..N", arrowhead=crow, style=dashed];
}}'''
    render("fig2_er", dot)


# ---------------------------------------------------------------- 4 并发预约流程
def fig_reservation_flow():
    dot = f'''digraph flow {{
  rankdir=TB; {COMMON}
  node [shape=box, style="rounded,filled", fillcolor="{BLUE_L}", color="#c9d4ea"];

  start [label="客户端提交预约\\nPOST /api/reservations", shape=stadium, fillcolor="{PURP_L}"];
  check [label="服务端校验\\n登录·角色·黑名单·时间\\n单日次数·自身时段冲突", fillcolor="{GREEN_L}"];
  slot  [label="起止时间 → slotIndex 列表"];
  lock  [label="Redisson 加锁\\nseat:{{seatId}}:date:{{date}}:slots", fillcolor="{ORANGE_L}"];
  tx    [label="开启事务\\ninsert reservation\\nbatch insert reservation_slot", fillcolor="{GREEN_L}"];
  uk    [label="唯一索引\\nuk_seat_date_slot 冲突?", shape=diamond, fillcolor="#fff2dd", color="{ORANGE}"];
  fail  [label="回滚事务\\n返回 SEAT_ALREADY_RESERVED", shape=box, fillcolor="{RED_L}", color="{RED}"];
  ok    [label="提交事务\\nRedis 座位=RESERVED\\n注册超时释放延迟任务\\n推送 SSE seat_reserved", fillcolor="{GREEN_L}"];
  done  [label="解锁 · 返回预约成功", shape=stadium, fillcolor="{PURP_L}"];

  start -> check -> slot -> lock -> tx -> uk;
  uk -> fail [label="是 (DuplicateKey)", color="{RED}", fontcolor="{RED}"];
  uk -> ok   [label="否", color="{GREEN}", fontcolor="{GREEN}"];
  ok -> done; fail -> done [style=dashed, label="解锁"];
}}'''
    render("fig4_reservation_flow", dot)


# ---------------------------------------------------------------- 5 状态机
def fig_state_machine():
    dot = f'''digraph sm {{
  rankdir=LR; {COMMON}
  node [shape=box, style="rounded,filled", color="#c9d4ea"];

  init [label="", shape=circle, width=0.2, style=filled, fillcolor="black"];
  P [label="待签到\\nPENDING_SIGN_IN", fillcolor="{BLUE_L}"];
  U [label="使用中\\nIN_USE", fillcolor="{GREEN_L}"];
  C [label="已完成\\nCOMPLETED", fillcolor="#dfe8ff"];
  X [label="已取消\\nCANCELLED", fillcolor="{GRAY_L}"];
  E [label="爽约释放\\nEXPIRED_RELEASED", fillcolor="{RED_L}", color="{RED}"];

  init -> P [label="预约成功"];
  P -> U [label="签到(窗口内)", color="{GREEN}", fontcolor="{GREEN}"];
  U -> C [label="签退 / 到期自动完成", color="{GREEN}", fontcolor="{GREEN}"];
  P -> X [label="用户取消", color="{GRAY}", fontcolor="{GRAY}"];
  P -> E [label="超时未签到\\nno_show+1·可进黑名单", color="{RED}", fontcolor="{RED}"];
}}'''
    render("fig5_state_machine", dot)


# ---------------------------------------------------------------- 6 SSE 实时看板
def fig_sse():
    dot = f'''digraph sse {{
  rankdir=LR; {COMMON}
  node [shape=box, style="rounded,filled", color="#c9d4ea"];

  subgraph cluster_a {{ label="客户端 A"; style="rounded,filled"; fillcolor="#f7f9ff"; color="#c9d4ea";
    A1 [label="打开看板\\n订阅 SSE", fillcolor="{BLUE_L}"];
    A2 [label="预约某座位成功", fillcolor="{GREEN_L}"];
  }}
  subgraph cluster_s {{ label="server"; style="rounded,filled"; fillcolor="#f6fbf7"; color="#c9d4ea";
    S0 [label="返回初始化快照\\nboard_snapshot", fillcolor="{GREEN_L}"];
    S1 [label="生成增量事件\\nseat_reserved", fillcolor="{ORANGE_L}"];
    S2 [label="SSE 推送管理器\\n广播给所有订阅者", fillcolor="{GREEN_L}"];
  }}
  subgraph cluster_b {{ label="客户端 B"; style="rounded,filled"; fillcolor="#f7f9ff"; color="#c9d4ea";
    B1 [label="打开同一看板", fillcolor="{BLUE_L}"];
    B2 [label="局部更新座位=RESERVED\\n(秒级)", fillcolor="{PURP_L}"];
  }}
  A1 -> S0 [label="① 快照"];
  A2 -> S1 [label="② 写库成功"];
  S1 -> S2;
  S2 -> A1 [label="③ 增量", style=dashed, color="{PURP}"];
  S2 -> B2 [label="③ 增量", style=dashed, color="{PURP}"];
  B1 -> S0 [label="① 快照"];
}}'''
    render("fig6_sse", dot)


# ---------------------------------------------------------------- 7 超时释放
def fig_timeout():
    dot = f'''digraph to {{
  rankdir=TB; {COMMON}
  node [shape=box, style="rounded,filled", fillcolor="{BLUE_L}", color="#c9d4ea"];

  q [label="Redisson DelayedQueue\\n预约开始 +15min 到期", shape=stadium, fillcolor="{ORANGE_L}"];
  chk [label="校验预约当前状态", shape=diamond, fillcolor="#fff2dd", color="{ORANGE}"];
  ign [label="已签到 / 已取消 → 忽略", fillcolor="{GRAY_L}"];
  rel [label="状态 → EXPIRED_RELEASED\\n删除 reservation_slot 释放座位\\nRedis 座位=FREE · 推送 SSE seat_released", fillcolor="{GREEN_L}"];
  cnt [label="no_show_count + 1\\n扣 3 分 (MVP+)", fillcolor="{RED_L}", color="{RED}"];
  bl  [label="达阈值 → 写 blacklist_record\\n(限制期 7 天)", shape=diamond, fillcolor="#fff2dd", color="{ORANGE}"];
  end [label="进入黑名单\\n限制期内预约被拒", fillcolor="{RED_L}", color="{RED}"];

  q -> chk;
  chk -> ign [label="仍待签到? 否"];
  chk -> rel [label="是", color="{GREEN}", fontcolor="{GREEN}"];
  rel -> cnt -> bl;
  bl -> end [label="是", color="{RED}", fontcolor="{RED}"];
}}'''
    render("fig7_timeout", dot)


# ---------------------------------------------------------------- 8 部署拓扑
def fig_deploy():
    dot = f'''digraph dep {{
  rankdir=LR; {COMMON}
  node [shape=box, style="rounded,filled", color="#c9d4ea"];

  browser [label="浏览器 / 移动端\\n(桌面 · iOS · Android)", shape=box3d, fillcolor="{PURP_L}"];
  subgraph cluster_compose {{
    label="Docker Compose 一键部署"; style="rounded,filled"; fillcolor="#f7f9ff"; color="#c9d4ea"; fontcolor="{BLUE}";
    nginx [label="frontend\\nNginx 静态 + 反向代理\\n:8888", fillcolor="{BLUE_L}"];
    backend [label="backend\\nSpring Boot\\n:8080 → 宿主 :18080", fillcolor="{GREEN_L}"];
    mysql [label="mysql\\nMySQL 8\\n:3306 → :3307", shape=cylinder, fillcolor="#dfe8ff", color="{BLUE}"];
    redis [label="redis\\nRedis 7\\n:6379 → :6380", shape=cylinder, fillcolor="{ORANGE_L}", color="{ORANGE}"];
    nginx -> backend [label="/api 反代"];
    backend -> mysql [label="JDBC"];
    backend -> redis [label="Lettuce/Redisson"];
  }}
  browser -> nginx [label="HTTP :8888", color="{BLUE}", penwidth=1.6];
}}'''
    render("fig8_deploy", dot)


def main():
    print("生成 Graphviz 图...")
    fig_architecture(); fig_er(); fig_reservation_flow(); fig_state_machine()
    fig_sse(); fig_timeout(); fig_deploy()
    print("生成 matplotlib 图...")
    import gen_slot_model  # noqa
    gen_slot_model.run(HERE, FONT)
    # 清理测试文件
    for f in ("_cntest.png", "_cntest.dot"):
        p = os.path.join(HERE, f)
        if os.path.exists(p): os.remove(p)
    print("完成。")


if __name__ == "__main__":
    main()
