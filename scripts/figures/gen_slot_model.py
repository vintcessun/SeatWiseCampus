# -*- coding: utf-8 -*-
"""时间片占用模型示意图（30 分钟片 + 唯一键 seat_id,date,slot_index）。"""
import os


def run(here, font="Microsoft YaHei"):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.patches import Rectangle, FancyBboxPatch
    plt.rcParams["font.sans-serif"] = [font, "SimHei"]
    plt.rcParams["axes.unicode_minus"] = False

    seats = ["A-01", "A-02", "A-03", "A-04", "A-05", "A-06"]
    # 30 分钟片：08:00 起，slot_index 16 = 08:00
    times = ["08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00"]
    slot_idx = list(range(16, 24))  # 8 个片
    ncol = len(slot_idx)
    nrow = len(seats)

    # 占用：seat_row -> list of (start_col, end_col_exclusive, who)
    occ = {
        0: [(0, 4, "张三")],          # A-01 08:00-10:00
        1: [(2, 5, "李四")],          # A-02 09:00-10:30
        3: [(0, 2, "王五"), (5, 8, "赵六")],
        4: [(4, 8, "钱七")],
    }

    fig, ax = plt.subplots(figsize=(11, 4.6))
    cell_w, cell_h = 1.0, 1.0
    FREE, OCC, EDGE = "#e3f6e9", "#3b6cff", "#cbd4e6"

    for r in range(nrow):
        for c in range(ncol):
            ax.add_patch(Rectangle((c, nrow - 1 - r), cell_w, cell_h,
                                   facecolor=FREE, edgecolor=EDGE, linewidth=1))
    # 占用块
    for r, blocks in occ.items():
        for (c0, c1, who) in blocks:
            ax.add_patch(FancyBboxPatch((c0 + 0.06, nrow - 1 - r + 0.08), (c1 - c0) - 0.12, cell_h - 0.16,
                         boxstyle="round,pad=0.02,rounding_size=0.08",
                         facecolor=OCC, edgecolor="#274bd6", linewidth=1.2))
            ax.text((c0 + c1) / 2, nrow - 1 - r + 0.5, f"{who}  预约", color="white",
                    ha="center", va="center", fontsize=10, fontweight="bold")

    # 轴标签
    ax.set_xlim(-2.2, ncol + 0.2)
    ax.set_ylim(-1.4, nrow + 0.6)
    ax.axis("off")
    for r in range(nrow):
        ax.text(-0.2, nrow - 1 - r + 0.5, seats[r], ha="right", va="center", fontsize=11, fontweight="bold")
    for c in range(ncol + 1):
        ax.text(c, nrow + 0.12, times[c], ha="center", va="bottom", fontsize=9, color="#5a6172", rotation=0)
        ax.plot([c, c], [nrow, nrow + 0.05], color="#5a6172", linewidth=0.6)
    # slot_index 行
    for c in range(ncol):
        ax.text(c + 0.5, -0.35, f"片{slot_idx[c]}", ha="center", va="center", fontsize=8.5, color="#8a93a6")

    ax.text(-2.1, nrow - 0.5, "座位\nseat_id", ha="left", va="center", fontsize=10, color="#3b6cff", fontweight="bold")
    ax.text(ncol / 2, -0.95, "同一 date 下，每个 (seat_id, slot_index) 至多被一条预约占用 —— 由唯一索引  uk_seat_date_slot(seat_id, date, slot_index)  兜底",
            ha="center", va="center", fontsize=10.5, color="#d64545", fontweight="bold")
    ax.set_title("时间片占用模型：30 分钟粒度拆片存储", fontsize=13, fontweight="bold", pad=16)

    out = os.path.join(here, "fig3_slot_model.png")
    fig.tight_layout()
    fig.savefig(out, dpi=170, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print("  ok fig3_slot_model.png")
