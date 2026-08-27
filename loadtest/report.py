"""JMeter .jtl 결과에서 TPS·백분위 응답시간을 뽑는다.

    python3 loadtest/report.py build/direct-2.jtl build/kafka-2.jtl

파일을 두 개 주면 개선폭까지 계산한다. 포폴 캐싱 슬라이드와 동일한 지표(TPS, p95)를
같은 방식으로 산출하기 위한 스크립트다.
"""
import csv
import os
import statistics
import sys


def percentile(sorted_values, q):
    """JMeter 와 동일한 방식(가장 가까운 순위법)으로 백분위를 구한다."""
    if not sorted_values:
        return 0
    idx = min(int(len(sorted_values) * q), len(sorted_values) - 1)
    return sorted_values[idx]


def load(path):
    with open(path, newline="") as f:
        rows = list(csv.DictReader(f))
    if not rows:
        raise SystemExit(f"{path}: 샘플이 없습니다")

    elapsed = sorted(int(r["elapsed"]) for r in rows)
    start = min(int(r["timeStamp"]) for r in rows)
    end = max(int(r["timeStamp"]) + int(r["elapsed"]) for r in rows)
    duration = (end - start) / 1000
    errors = sum(1 for r in rows if r.get("success") != "true")

    return {
        "name": os.path.basename(path),
        "n": len(rows),
        "duration": duration,
        "tps": len(rows) / duration if duration else 0,
        "avg": statistics.mean(elapsed),
        "p50": percentile(elapsed, 0.50),
        "p90": percentile(elapsed, 0.90),
        "p95": percentile(elapsed, 0.95),
        "p99": percentile(elapsed, 0.99),
        "max": elapsed[-1],
        "err": errors / len(rows) * 100,
    }


def main(paths):
    results = [load(p) for p in paths]

    header = f"{'파일':<22}{'n':>6}{'소요(s)':>9}{'TPS':>9}{'avg':>8}{'p50':>7}{'p90':>7}{'p95':>7}{'p99':>7}{'max':>7}{'err%':>7}"
    print(header)
    print("-" * len(header))
    for r in results:
        print(f"{r['name']:<22}{r['n']:>6}{r['duration']:>9.2f}{r['tps']:>9.1f}"
              f"{r['avg']:>8.0f}{r['p50']:>7}{r['p90']:>7}{r['p95']:>7}{r['p99']:>7}{r['max']:>7}{r['err']:>7.1f}")

    if len(results) == 2:
        before, after = results
        print()
        print(f"TPS  {before['tps']:.1f} → {after['tps']:.1f}  "
              f"({after['tps'] / before['tps']:.2f}배)")
        print(f"p95  {before['p95']}ms → {after['p95']}ms  "
              f"({(after['p95'] - before['p95']) / before['p95'] * 100:+.1f}%)")

    if any(r["err"] > 0 for r in results):
        print("\n주의: 실패 샘플이 있습니다. 응답코드 분포를 확인하세요.")
        print("  awk -F, 'NR>1{print $4}' <파일> | sort | uniq -c")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    main(sys.argv[1:])
