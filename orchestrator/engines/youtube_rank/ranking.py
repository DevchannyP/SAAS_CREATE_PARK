import math

def calc_hot_score(view_count: int, like_count: int, comment_count: int, hours_since_publish: float) -> float:
    """Stable v1 ranking formula; inputs are public aggregate metadata only."""
    hours = max(hours_since_publish, 1.0)
    views_per_hour = max(view_count, 0) / hours
    score = (
        math.log10(max(view_count, 0) + 1) * 0.35
        + math.log10(max(like_count, 0) + 1) * 0.20
        + math.log10(max(comment_count, 0) + 1) * 0.20
        + math.log10(views_per_hour + 1) * 0.20
        + (1 / hours) * 0.05
    )
    return round(score, 4)
