#!/usr/bin/env bash
# I-1~I-6 不变量离线校验（README 第十节 成果 A）
set -euo pipefail
BASE="${1:-http://localhost:8080}"
curl -sf "$BASE/v1/invariants" | tee /tmp/cons.json
python3 -c '
import json
d=json.load(open("/tmp/cons.json"))["data"]
assert d["healthy"] is True, d
assert all(v==0 for v in d["violations"].values()), d
print("PASS: I-1..I-4 all zero")'