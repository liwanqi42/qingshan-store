#!/usr/bin/env python3
"""
用 OSV 公开漏洞库扫描 Maven 依赖。

用法（在仓库根目录）：
    mvn -f apps/backend/pom.xml dependency:tree -DoutputFile=dependency-tree.txt
    python scripts/osv-scan.py dependency-tree.txt docs/依赖与漏洞扫描报告.md

说明：
    · OSV 查询 Maven 生态时包名格式为 groupId:artifactId；
    · 只扫描 compile/runtime 依赖，test/provided 默认跳过；
    · 输出按依赖聚合的漏洞表，便于逐项整改。
"""
from __future__ import annotations

import json
import re
import sys
import urllib.error
import urllib.request
from collections import defaultdict
from pathlib import Path

OSV_BATCH_URL = "https://api.osv.dev/v1/querybatch"
OSV_VULN_URL = "https://api.osv.dev/v1/vulns/"
DEPENDENCY_RE = re.compile(
    r"^[|+\\ -]*"
    r"(?P<group>[\w.\-]+):(?P<artifact>[\w.\-]+):jar:(?P<version>[^:]+):(?P<scope>\w+)"
)


def parse_tree(path: Path) -> list[tuple[str, str, str]]:
    deps: dict[tuple[str, str, str], None] = {}
    for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        match = DEPENDENCY_RE.match(line)
        if not match or match.group("scope") not in {"compile", "runtime"}:
            continue
        deps[(match.group("group"), match.group("artifact"), match.group("version"))] = None
    return sorted(deps)


def post_json(url: str, payload: dict) -> dict:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read().decode("utf-8"))


def get_json(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    tree_path = Path(sys.argv[1])
    if not tree_path.exists():
        print(f"依赖树文件不存在：{tree_path}", file=sys.stderr)
        return 2

    deps = parse_tree(tree_path)
    queries = [
        {"package": {"name": f"{group}:{artifact}", "ecosystem": "Maven"}, "version": version}
        for group, artifact, version in deps
    ]

    vulnerabilities: dict[str, dict] = {}
    findings: dict[tuple[str, str, str], list[str]] = defaultdict(list)

    # OSV 批量接口单次查询数量有限，按 500 分批
    for start in range(0, len(queries), 500):
        batch = queries[start:start + 500]
        try:
            result = post_json(OSV_BATCH_URL, {"queries": batch})
        except urllib.error.URLError as exc:
            print(f"OSV 请求失败：{exc}", file=sys.stderr)
            return 1
        for dep, item in zip(deps[start:start + 500], result.get("results", [])):
            for vuln in item.get("vulns", []) or []:
                vuln_id = vuln["id"]
                vulnerabilities[vuln_id] = vuln
                findings[dep].append(vuln_id)

    # 拉取漏洞详情（摘要、严重级别、修复版本）
    details: dict[str, dict] = {}
    for vuln_id in vulnerabilities:
        try:
            details[vuln_id] = get_json(OSV_VULN_URL + vuln_id)
        except urllib.error.URLError:
            details[vuln_id] = vulnerabilities[vuln_id]

    lines = [
        "## OSV 扫描结果",
        "",
        f"- 扫描依赖数：{len(deps)}（compile/runtime，已去重）",
        f"- 命中漏洞依赖数：{len(findings)}",
        f"- 漏洞条目数：{len(vulnerabilities)}",
        "",
        "| 依赖 | 版本 | 漏洞 | 严重级别 | 摘要 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for (group, artifact, version), vuln_ids in sorted(findings.items()):
        for vuln_id in sorted(set(vuln_ids)):
            detail = details.get(vuln_id, {})
            severity = ""
            for item in detail.get("severity", []) or []:
                severity = item.get("score", severity)
            summary = (detail.get("summary") or "").replace("|", "\\|")
            lines.append(
                f"| `{group}:{artifact}` | `{version}` | [{vuln_id}](https://osv.dev/vulnerability/{vuln_id}) "
                f"| {severity or '-'} | {summary or '-'} |"
            )
    if not findings:
        lines.append("| - | - | 未发现已知漏洞 | - | - |")

    output = "\n".join(lines) + "\n"
    if len(sys.argv) >= 3:
        Path(sys.argv[2]).write_text(output, encoding="utf-8")
        print(f"已写入 {sys.argv[2]}：{len(findings)} 个依赖命中 {len(vulnerabilities)} 个漏洞")
    else:
        print(output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
